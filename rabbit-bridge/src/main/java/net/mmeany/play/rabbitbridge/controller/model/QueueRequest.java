package net.mmeany.play.rabbitbridge.controller.model;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record QueueRequest(
    String queueName
) {}
