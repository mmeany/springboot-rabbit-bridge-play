package net.mmeany.play.rabbitbridge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.mmeany.play.rabbitbridge.model.MessageWithId;
import net.mmeany.play.rabbitbridge.model.QueueCount;
import net.mmeany.play.rabbitbridge.model.RabbitListener;
import net.mmeany.play.rabbitbridge.model.SimpleMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RabbitService {

    private final Map<String, RabbitListener> listeners = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(0);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitService self;

    public RabbitService(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, @Lazy RabbitService self) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    @PreDestroy
    public void reset() {
        listeners.values().forEach(listener -> listener.dispose(true));
        listeners.clear();
        log.info("Message counter reset to 0");
        counter.set(0);
    }

    public void sendMessage(SimpleMessage message) {
        log.info("Sending message: {}", message);
        rabbitTemplate.convertAndSend("primary", "*.*.%s".formatted(message.queueName()), MessageWithId.builder()
            .id(counter.incrementAndGet())
            .queueName(message.queueName())
            .message(message.message())
            .build());
    }

    public void listenForMessages(List<String> queueNames) {
        queueNames.forEach(self::listenForMessages);
    }

    public void listenForMessages(String queueName) {
        RabbitListener listener = listeners.computeIfAbsent(queueName,
            k -> {
                RabbitListener rabbitListener = new RabbitListener(rabbitTemplate.getConnectionFactory(), queueName);
                log.info("Listener added for queue: '{}'", queueName);
                return rabbitListener;
            });
        listener.startListening();
    }

    public void stopListeningForMessages(String queueName) {
        RabbitListener listener = listeners.get(queueName);
        if (listener != null) {
            listener.stopListening();
            log.info("Stopped listening for messages on queue: '{}'", queueName);
        }
    }

    public void stopListeningToAllQueues() {
        listeners.keySet().forEach(self::stopListeningForMessages);
    }

    public List<QueueCount> messageCount() {
        return listeners.entrySet().stream()
            .map(entry -> QueueCount.builder().
                queueName(entry.getKey())
                .count(entry.getValue().getMessageCount())
                .build())
            .toList();
    }

    public QueueCount messageCount(String queueName) {
        RabbitListener listener = listeners.get(queueName);
        return listener != null
            ? QueueCount.builder()
            .queueName(queueName)
            .count(listener.getMessageCount())
            .build()
            : QueueCount.builder()
            .queueName(queueName)
            .count(0)
            .build();
    }

    public String message(String queueName, int index) {
        RabbitListener listener = listeners.get(queueName);
        return listener != null ? listener.getEntry(index) : null;
    }

    public String findMessagesByJsonPath(String queueName, String jsonPath) {
        RabbitListener listener = listeners.get(queueName);
        if (listener == null) {
            return "[]";
        }

        Function<String, String> messageFromMessageWithId = m -> {
            try {
                return objectMapper.readValue(m, MessageWithId.class).message();
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        };

        Predicate<String> messageMatchesJsonPath = message -> {
            try {
                Object result = JsonPath.read(message, jsonPath);
                if (result instanceof Collection) {
                    return !((Collection<?>) result).isEmpty();
                }
                return result != null && !Boolean.FALSE.equals(result);
            } catch (Exception e) {
                return false;
            }
        };

        String json = "[" + listener.getMessages().stream()
            .map(messageFromMessageWithId)
            .filter(messageMatchesJsonPath)
            .collect(Collectors.joining(",")) +
            "]";
        return json;
    }
}
