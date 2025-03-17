package net.mmeany.play.rabbitbridge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import net.mmeany.play.rabbitbridge.TestData;
import net.mmeany.play.rabbitbridge.model.MessageWithId;
import net.mmeany.play.rabbitbridge.model.QueueCount;
import net.mmeany.play.rabbitbridge.model.RabbitListener;
import net.mmeany.play.rabbitbridge.model.SimpleMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private Connection connection;

    @Mock
    private Channel channel;

    @Mock
    private RabbitListener rabbitListener;

    private RabbitService rabbitService;

    @BeforeEach
    void setUp() {
        rabbitService = spy(new RabbitService(rabbitTemplate, new ObjectMapper(), null));
        ReflectionTestUtils.setField(rabbitService, "self", rabbitService);
    }

    @Test
    void sendMessage_shouldSendMessageWithId() {
        SimpleMessage message = new SimpleMessage(TestData.QUEUE_1, TestData.MESSAGE);

        rabbitService.sendMessage(message);

        ArgumentCaptor<MessageWithId> messageCaptor = ArgumentCaptor.forClass(MessageWithId.class);
        verify(rabbitTemplate).convertAndSend(
            eq(TestData.EXCHANGE),
            eq("*.*." + TestData.QUEUE_1),
            messageCaptor.capture()
        );

        MessageWithId capturedMessage = messageCaptor.getValue();
        assertEquals(1L, capturedMessage.id());
        assertEquals(TestData.QUEUE_1, capturedMessage.queueName());
        assertEquals(TestData.MESSAGE, capturedMessage.message());
    }

    @Test
    void listenForMessages_shouldAddListenerForEachQueue() {
        List<String> queueNames = Arrays.asList(TestData.QUEUE_1, TestData.QUEUE_2);

        when(rabbitTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connection.createChannel(anyBoolean())).thenReturn(channel);

        rabbitService.listenForMessages(queueNames);

        verify(rabbitService).listenForMessages(TestData.QUEUE_1);
        verify(rabbitService).listenForMessages(TestData.QUEUE_2);
    }

    @Test
    void listenForMessages_shouldCreateListenerOnce() {
        String queueName = TestData.QUEUE_1;

        Map<String, RabbitListener> listeners = new ConcurrentHashMap<>();
        listeners.put(queueName, rabbitListener);
        ReflectionTestUtils.setField(rabbitService, "listeners", listeners);

        rabbitService.listenForMessages(queueName);
        rabbitService.listenForMessages(queueName);

        verify(rabbitListener, times(2)).startListening(); // Should start twice
        verify(rabbitService, times(2)).listenForMessages(queueName); // Method called twice
    }

    @Test
    void stopListeningForMessages_shouldStopListener() {
        String queueName = TestData.QUEUE_1;

        Map<String, RabbitListener> listeners = new ConcurrentHashMap<>();
        listeners.put(queueName, rabbitListener);
        ReflectionTestUtils.setField(rabbitService, "listeners", listeners);

        rabbitService.stopListeningForMessages(queueName);

        verify(rabbitListener).stopListening();
    }

    @Test
    void stopListeningToAllQueues_shouldStopAllListeners() {
        List<String> queueNames = Arrays.asList(TestData.QUEUE_1, TestData.QUEUE_2);

        RabbitListener listener1 = mock(RabbitListener.class);
        RabbitListener listener2 = mock(RabbitListener.class);

        Map<String, RabbitListener> listeners = new ConcurrentHashMap<>();
        listeners.put(TestData.QUEUE_1, listener1);
        listeners.put(TestData.QUEUE_2, listener2);
        ReflectionTestUtils.setField(rabbitService, "listeners", listeners);

        rabbitService.stopListeningToAllQueues();

        verify(listener1).stopListening();
        verify(listener2).stopListening();
    }

    @Test
    void messageCount_shouldReturnCountsForAllQueues() {
        String queue1 = TestData.QUEUE_1;
        String queue2 = TestData.QUEUE_2;

        RabbitListener listener1 = mock(RabbitListener.class);
        RabbitListener listener2 = mock(RabbitListener.class);

        when(listener1.getMessageCount()).thenReturn(5);
        when(listener2.getMessageCount()).thenReturn(10);

        Map<String, RabbitListener> listeners = new ConcurrentHashMap<>();
        listeners.put(queue1, listener1);
        listeners.put(queue2, listener2);
        ReflectionTestUtils.setField(rabbitService, "listeners", listeners);

        List<QueueCount> result = rabbitService.messageCount();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(qc -> qc.queueName().equals(queue1) && qc.count() == 5));
        assertTrue(result.stream().anyMatch(qc -> qc.queueName().equals(queue2) && qc.count() == 10));
    }

    @Test
    void messageCount_forSpecificQueue_shouldReturnCount() {
        String queueName = TestData.QUEUE_1;

        RabbitListener listener = mock(RabbitListener.class);
        when(listener.getMessageCount()).thenReturn(5);

        Map<String, RabbitListener> listeners = new ConcurrentHashMap<>();
        listeners.put(queueName, listener);
        ReflectionTestUtils.setField(rabbitService, "listeners", listeners);

        QueueCount result = rabbitService.messageCount(queueName);

        assertEquals(queueName, result.queueName());
        assertEquals(5, result.count());
    }

    @Test
    void messageCount_forNonExistentQueue_shouldReturnZero() {
        QueueCount result = rabbitService.messageCount("non-existent");

        assertEquals("non-existent", result.queueName());
        assertEquals(0, result.count());
    }

    @Test
    void message_shouldReturnMessageAtIndex() {
        String queueName = TestData.QUEUE_1;
        String expectedMessage = TestData.MESSAGE;

        RabbitListener listener = mock(RabbitListener.class);
        when(listener.getEntry(0)).thenReturn(expectedMessage);

        Map<String, RabbitListener> listeners = new ConcurrentHashMap<>();
        listeners.put(queueName, listener);
        ReflectionTestUtils.setField(rabbitService, "listeners", listeners);

        String result = rabbitService.message(queueName, 0);

        assertEquals(expectedMessage, result);
    }

    @Test
    void reset_shouldDisposeAllListenersAndClearCounter() {
        RabbitListener listener1 = mock(RabbitListener.class);
        RabbitListener listener2 = mock(RabbitListener.class);

        Map<String, RabbitListener> listeners = new ConcurrentHashMap<>();
        listeners.put(TestData.QUEUE_1, listener1);
        listeners.put(TestData.QUEUE_2, listener2);
        ReflectionTestUtils.setField(rabbitService, "listeners", listeners);

        AtomicLong counter = new AtomicLong(10);
        ReflectionTestUtils.setField(rabbitService, "counter", counter);

        rabbitService.reset();

        verify(listener1).dispose(true);
        verify(listener2).dispose(true);
        assertTrue(listeners.isEmpty());

        assertEquals(0, counter.get());
    }

    @Test
    void findMessagesByJsonPath_shouldFilterMessages() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String queueName = TestData.QUEUE_1;
        String jsonPath = "$.name";
        List<String> messages = List.of(
            objectMapper.writeValueAsString(MessageWithId.builder()
                .id(1L)
                .queueName(queueName)
                .message("{\"name\":\"John\"}")
                .build()),
            objectMapper.writeValueAsString(MessageWithId.builder()
                .id(1L)
                .queueName(queueName)
                .message("{\"name\":\"Jane\"}")
                .build()),
            objectMapper.writeValueAsString(MessageWithId.builder()
                .id(1L)
                .queueName(queueName)
                .message("{\"age\":30}")
                .build())
        );

        RabbitListener listener = mock(RabbitListener.class);
        when(listener.getMessages()).thenReturn(messages);

        Map<String, RabbitListener> listeners = new ConcurrentHashMap<>();
        listeners.put(queueName, listener);
        ReflectionTestUtils.setField(rabbitService, "listeners", listeners);

        String result = rabbitService.findMessagesByJsonPath(queueName, jsonPath);

        assertTrue(result.contains("{\"name\":\"John\"}"));
        assertTrue(result.contains("{\"name\":\"Jane\"}"));
        assertFalse(result.contains("{\"age\":30}"));
    }

    @Test
    void findMessagesByJsonPath_withNonExistentQueue_shouldReturnEmptyArray() {
        String result = rabbitService.findMessagesByJsonPath("non-existent", "$.name");

        assertEquals("[]", result);
    }
}