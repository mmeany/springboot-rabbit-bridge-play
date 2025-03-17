package net.mmeany.play.rabbitbridge.controller;

import net.mmeany.play.rabbitbridge.TestData;
import net.mmeany.play.rabbitbridge.controller.model.CountsResponse;
import net.mmeany.play.rabbitbridge.controller.model.JsonPathRequest;
import net.mmeany.play.rabbitbridge.controller.model.ListenRequest;
import net.mmeany.play.rabbitbridge.model.QueueCount;
import net.mmeany.play.rabbitbridge.model.SimpleMessage;
import net.mmeany.play.rabbitbridge.service.RabbitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ControllerTest {

    @Mock
    private RabbitService rabbitService;

    @InjectMocks
    private Controller controller;

    @Test
    void reset_shouldReturnOk() {
        ResponseEntity<?> response = controller.reset();
        assertEquals(ResponseEntity.ok().build(), response);
        verify(rabbitService, times(1)).reset();
    }

    @Test
    void sendMessage_shouldReturnOk() {
        SimpleMessage message = new SimpleMessage(TestData.QUEUE_1, TestData.MESSAGE);
        ResponseEntity<?> response = controller.sendMessage(message);
        assertEquals(ResponseEntity.ok().build(), response);
        verify(rabbitService, times(1)).sendMessage(message);
    }

    @Test
    void listenForMessages_shouldReturnOk() {
        ListenRequest request = new ListenRequest(Collections.singletonList(TestData.QUEUE_1));
        ResponseEntity<?> response = controller.listenForMessages(request);
        assertEquals(ResponseEntity.ok().build(), response);
        verify(rabbitService, times(1)).listenForMessages(request.queueNames());
    }

    @Test
    void stopListeningOnAllQueues_shouldReturnOk() {
        ResponseEntity<?> response = controller.stopListeningOnAllQueues();
        assertEquals(ResponseEntity.ok().build(), response);
        verify(rabbitService, times(1)).stopListeningToAllQueues();
    }

    @Test
    void messageCountAll_shouldReturnCountsResponse() {
        when(rabbitService.messageCount()).thenReturn(Collections.emptyList());
        ResponseEntity<CountsResponse> response = controller.messageCountAll();
        assertEquals(ResponseEntity.ok(CountsResponse.builder().counts(Collections.emptyList()).build()), response);
    }

    @Test
    void messageCount_shouldReturnQueueCount() {
        QueueCount queueCount = new QueueCount(TestData.QUEUE_1, TestData.QUEUE_COUNT);
        when(rabbitService.messageCount(TestData.QUEUE_1)).thenReturn(queueCount);
        ResponseEntity<QueueCount> response = controller.messageCount(TestData.QUEUE_1);
        assertEquals(ResponseEntity.ok(queueCount), response);
    }

    @Test
    void message_shouldReturnMessage() {
        String message = TestData.MESSAGE;
        when(rabbitService.message(TestData.QUEUE_1, TestData.MESSAGE_INDEX)).thenReturn(message);
        ResponseEntity<?> response = controller.message(TestData.QUEUE_1, TestData.MESSAGE_INDEX);
        assertEquals(ResponseEntity.ok(message), response);
    }

    @Test
    void searchByJsonPath_shouldReturnMessages() {
        JsonPathRequest request = new JsonPathRequest(TestData.QUEUE_1, TestData.JSON_PATH);
        when(rabbitService.findMessagesByJsonPath(TestData.QUEUE_1, TestData.JSON_PATH)).thenReturn(TestData.MESSAGES);
        ResponseEntity<?> response = controller.searchByJsonPath(request);
        assertEquals(ResponseEntity.ok(TestData.MESSAGES), response);
    }
}