package net.mmeany.play.rabbitbridge.model;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class RabbitListener {

    private final Connection connection;
    private final Channel channel;
    private final String queueName;
    private final List<String> messages = new ArrayList<>();

    private String consumerTag;
    private final AtomicBoolean captureComplete = new AtomicBoolean(false);

    public RabbitListener(ConnectionFactory connectionFactory, String queueName) {
        connection = connectionFactory.createConnection();
        channel = connection.createChannel(false);
        this.queueName = queueName;
    }

    public void startListening() {
        failIfCaptureComplete();
        log.info("Start listening for messages on queue: {}", queueName);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            addEntry(new String(delivery.getBody(), StandardCharsets.UTF_8));
        };
        try {
            consumerTag = channel.basicConsume(queueName, true, deliverCallback, System.out::println);
            log.info("Consumer tag: {}", consumerTag);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void stopListening() {
        failIfCaptureComplete();
        log.info("Stop listening for messages on queue: {}", queueName);
        dispose();
        captureComplete.set(true);
    }

    public int getMessageCount() {
        failIfCaptureNotComplete();
        return messages.size();
    }

    public List<String> getMessages() {
        failIfCaptureNotComplete();
        return Collections.unmodifiableList(messages);
    }

    public String getEntry(int index) {
        failIfCaptureNotComplete();
        return messages.get(index);
    }

    public void dispose() {
        try {
            if (consumerTag != null) {
                log.info("Disposing consumer '{}' for queue '{}'", consumerTag, queueName);
                channel.basicCancel(consumerTag);
                consumerTag = null;
            }
        } catch (IOException e) {
            log.error("Error stopping consumer '{}' for queue '{}'", consumerTag, queueName, e);
        }
        try {
            if (channel != null && channel.isOpen()) {
                log.info("Disposing channel for queue '{}'", queueName);
                channel.close();
            }
        } catch (IOException | TimeoutException e) {
            log.error("Error closing channel for queue '{}'", queueName, e);
        }
        if (connection != null && connection.isOpen()) {
            log.info("Disposing connection for queue '{}'", queueName);
            connection.close();
        }
    }

    private void failIfCaptureComplete() {
        if (captureComplete.get()) {
            throw new RuntimeException("Message capture has finished");
        }
    }

    private void failIfCaptureNotComplete() {
        if (!captureComplete.get()) {
            throw new RuntimeException("Message capture not finished");
        }
    }

    private synchronized void addEntry(String entry) {
        messages.add(entry);
    }
}
