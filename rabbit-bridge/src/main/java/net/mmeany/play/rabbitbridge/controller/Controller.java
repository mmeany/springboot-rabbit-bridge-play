package net.mmeany.play.rabbitbridge.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class Controller {

    private final RabbitService rabbitService;

    @PostMapping("reset")
    public ResponseEntity<?> reset() {
        log.debug("reset");
        rabbitService.reset();
        return ResponseEntity.ok().build();
    }

    @PostMapping("send")
    public ResponseEntity<?> sendMessage(@RequestBody SimpleMessage request) {
        log.debug("send message");
        rabbitService.sendMessage(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("start-listening")
    public ResponseEntity<?> listenForMessages(@RequestBody ListenRequest request) {
        log.debug("start listening");
        rabbitService.listenForMessages(request.queueNames());
        return ResponseEntity.ok().build();
    }

    @PostMapping("stop-listening")
    public ResponseEntity<?> stopListeningOnAllQueues() {
        log.debug("stop listening");
        rabbitService.stopListeningToAllQueues();
        return ResponseEntity.ok().build();
    }

    @GetMapping("count")
    public ResponseEntity<CountsResponse> messageCountAll() {
        log.debug("message count all");
        return ResponseEntity.ok(CountsResponse.builder().counts(rabbitService.messageCount()).build());
    }

    @GetMapping("count/{queueName}")
    public ResponseEntity<QueueCount> messageCount(@PathVariable("queueName") String queueName) {
        log.debug("message count");
        return ResponseEntity.ok(rabbitService.messageCount(queueName));
    }

    @GetMapping(value = "message/{queueName}/{index}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> message(@PathVariable("queueName") String queueName, @PathVariable("index") int index) {
        log.debug("message {}, {}", queueName, index);
        return ResponseEntity.ok(rabbitService.message(queueName, index));
    }

    @PostMapping(value = "search-by-path", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchByJsonPath(@RequestBody JsonPathRequest request) {
        log.debug("search by json path");
        return ResponseEntity.ok(rabbitService.findMessagesByJsonPath(request.queueName(), request.jsonPath()));
    }
}
