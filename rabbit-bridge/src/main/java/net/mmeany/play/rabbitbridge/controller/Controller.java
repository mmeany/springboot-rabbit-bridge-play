package net.mmeany.play.rabbitbridge.controller;

import lombok.RequiredArgsConstructor;
import net.mmeany.play.rabbitbridge.controller.model.CountsResponse;
import net.mmeany.play.rabbitbridge.controller.model.JsonPathRequest;
import net.mmeany.play.rabbitbridge.controller.model.ListenRequest;
import net.mmeany.play.rabbitbridge.model.QueueCount;
import net.mmeany.play.rabbitbridge.model.SimpleMessage;
import net.mmeany.play.rabbitbridge.service.RabbitService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class Controller {

    private final RabbitService rabbitService;

    @PostMapping("reset")
    public ResponseEntity<?> reset() {
        rabbitService.reset();
        return ResponseEntity.ok().build();
    }

    @PostMapping("send")
    public ResponseEntity<?> sendMessage(@RequestBody SimpleMessage request) {
        rabbitService.sendMessage(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("start-listening")
    public ResponseEntity<?> listenForMessages(@RequestBody ListenRequest request) {
        rabbitService.listenForMessages(request.queueNames());
        return ResponseEntity.ok().build();
    }

    @PostMapping("stop-listening")
    public ResponseEntity<?> stopListeningOnAllQueues() {
        rabbitService.stopListeningToAllQueues();
        return ResponseEntity.ok().build();
    }

    @GetMapping("count")
    public ResponseEntity<CountsResponse> messageCountAll() {
        return ResponseEntity.ok(CountsResponse.builder().counts(rabbitService.messageCount()).build());
    }

    @GetMapping("count/{queueName}")
    public ResponseEntity<QueueCount> messageCount(@PathVariable("queueName") String queueName) {
        return ResponseEntity.ok(rabbitService.messageCount(queueName));
    }

    @GetMapping(value = "message/{queueName}/{index}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> message(@PathVariable("queueName") String queueName, @PathVariable("index") int index) {
        return ResponseEntity.ok(rabbitService.message(queueName, index));
    }

    @PostMapping(value = "search-by-path", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchByJsonPath(@RequestBody JsonPathRequest request) {
        return ResponseEntity.ok(rabbitService.findMessagesByJsonPath(request.queueName(), request.jsonPath()));
    }
}
