package net.mmeany.play.rabbitbridge.model;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import net.mmeany.play.rabbitbridge.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class RabbitListenerTest {

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private Connection connection;

    @Mock
    private Channel channel;

    private RabbitListener listener;

    @BeforeEach
    void setUp() {
        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connection.createChannel(anyBoolean())).thenReturn(channel);

        listener = new RabbitListener(connectionFactory, TestData.QUEUE_1);
    }

    @Test
    void startListening_registersConsumerWithChannel() throws IOException {
        whenChannelBasicConsume();

        listener.startListening();

        verify(channel).basicConsume(eq(TestData.QUEUE_1), eq(true), any(DeliverCallback.class), (CancelCallback) any());
    }

    @Test
    void stopListening_disposesConsumerAndMarksComplete() throws IOException {
        whenChannelBasicConsume();
        listener.startListening();

        listener.stopListening();

        verify(channel).basicCancel(TestData.CONSUMER_TAG);
        assertTrue(listener.getMessages().isEmpty());
    }

    @Test
    void messageProcessing_addsMessageToList() throws IOException {
        whenChannelBasicConsume();

        ArgumentCaptor<DeliverCallback> callbackCaptor = ArgumentCaptor.forClass(DeliverCallback.class);
        listener.startListening();
        verify(channel).basicConsume(anyString(), anyBoolean(), callbackCaptor.capture(), (CancelCallback) any());

        DeliverCallback callback = callbackCaptor.getValue();
        callback.handle(TestData.CONSUMER_TAG, new Delivery(null, null, TestData.MESSAGE.getBytes(StandardCharsets.UTF_8)));

        listener.stopListening();
        assertEquals(1, listener.getMessageCount());
        assertEquals(TestData.MESSAGE, listener.getEntry(0));
    }

    @Test
    void getMessageCount_beforeCapture_throwsException() {
        assertThrows(RuntimeException.class, () -> listener.getMessageCount());
    }

    @Test
    void getMessages_returnsUnmodifiableList() throws IOException {
        whenChannelBasicConsume();
        listener.startListening();
        listener.stopListening();

        assertThrows(UnsupportedOperationException.class, () -> listener.getMessages().add("should fail"));
    }

    @Test
    void startListening_afterCompletedCapture_throwsException() throws IOException {
        whenChannelBasicConsume();
        listener.startListening();
        listener.stopListening();

        assertThrows(RuntimeException.class, () -> listener.startListening());
    }

    @Test
    void dispose_closesAllResources() throws IOException, TimeoutException {
        whenChannelBasicConsume();
        when(connection.isOpen()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        listener.startListening();

        listener.dispose(true);

        verify(channel).basicCancel(TestData.CONSUMER_TAG);
        verify(channel).close();
        verify(connection).close();
    }

    @Test
    void dispose_withDisposeState_resetsInternalState() throws IOException {
        whenChannelBasicConsume();
        ArgumentCaptor<DeliverCallback> callbackCaptor = ArgumentCaptor.forClass(DeliverCallback.class);
        listener.startListening();
        verify(channel).basicConsume(anyString(), anyBoolean(), callbackCaptor.capture(), (CancelCallback) any());

        DeliverCallback callback = callbackCaptor.getValue();
        callback.handle(TestData.CONSUMER_TAG, new Delivery(null, null, TestData.MESSAGE.getBytes(StandardCharsets.UTF_8)));
        listener.stopListening();

        listener.dispose(true);

        assertThrows(RuntimeException.class, () -> listener.getMessageCount());
    }

    private void whenChannelBasicConsume() throws IOException {
        when(channel.basicConsume(anyString(), anyBoolean(), any(DeliverCallback.class), (CancelCallback) any())).thenReturn(TestData.CONSUMER_TAG);
    }
}