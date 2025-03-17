package net.mmeany.play.rabbitbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.mmeany.play.rabbitbridge.TestData;
import net.mmeany.play.rabbitbridge.controller.model.CountsResponse;
import net.mmeany.play.rabbitbridge.controller.model.JsonPathRequest;
import net.mmeany.play.rabbitbridge.controller.model.ListenRequest;
import net.mmeany.play.rabbitbridge.model.QueueCount;
import net.mmeany.play.rabbitbridge.model.SimpleMessage;
import net.mmeany.play.rabbitbridge.service.RabbitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Controller.class)
class ControllerMockMvcTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RabbitService rabbitService;

    @Test
    void reset_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/reset"))
            .andExpect(status().isOk());
        verify(rabbitService, times(1)).reset();
    }

    @Test
    void sendMessage_shouldReturnOk() throws Exception {
        SimpleMessage message = new SimpleMessage(TestData.QUEUE_1, TestData.MESSAGE);
        String json = objectMapper.writeValueAsString(message);
        mockMvc.perform(post("/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk());
        verify(rabbitService, times(1)).sendMessage(message);
    }

    @Test
    void listenForMessages_shouldReturnOk() throws Exception {
        List<String> queues = List.of(TestData.QUEUE_1);
        ListenRequest request = new ListenRequest(queues);
        String json = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/start-listening")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk());
        verify(rabbitService, times(1)).listenForMessages(queues);
    }

    @Test
    void stopListeningOnAllQueues_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/stop-listening"))
            .andExpect(status().isOk());
        verify(rabbitService, times(1)).stopListeningToAllQueues();
    }

    @Test
    void messageCountAll_shouldReturnCountsResponse() throws Exception {
        List<QueueCount> counts = List.of();
        CountsResponse countsResponse = CountsResponse.builder().counts(counts).build();
        String response = objectMapper.writeValueAsString(countsResponse);
        when(rabbitService.messageCount()).thenReturn(counts);
        mockMvc.perform(get("/count"))
            .andExpect(status().isOk())
            .andExpect(content().json(response));
    }

    @Test
    void messageCount_shouldReturnQueueCount() throws Exception {
        QueueCount queueCount = new QueueCount(TestData.QUEUE_1, TestData.QUEUE_COUNT);
        String response = objectMapper.writeValueAsString(queueCount);
        when(rabbitService.messageCount(TestData.QUEUE_1)).thenReturn(queueCount);
        mockMvc.perform(get("/count/{queue}", TestData.QUEUE_1))
            .andExpect(status().isOk())
            .andExpect(content().json(response));
    }

    @Test
    void message_shouldReturnMessage() throws Exception {
        String message = TestData.MESSAGE;
        when(rabbitService.message(TestData.QUEUE_1, TestData.MESSAGE_INDEX)).thenReturn(message);
        mockMvc.perform(get("/message/{queue}/{index}", TestData.QUEUE_1, TestData.MESSAGE_INDEX))
            .andExpect(status().isOk())
            .andExpect(content().string(TestData.MESSAGE));
    }

    @Test
    void searchByJsonPath_shouldReturnMessages() throws Exception {
        JsonPathRequest request = new JsonPathRequest(TestData.QUEUE_1, TestData.JSON_PATH);
        String json = objectMapper.writeValueAsString(request);
        String messages = TestData.MESSAGES;
        when(rabbitService.findMessagesByJsonPath(TestData.QUEUE_1, TestData.JSON_PATH)).thenReturn(messages);
        mockMvc.perform(post("/search-by-path")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(content().string(TestData.MESSAGES));
    }
}