package net.mmeany.play.rabbitbridge.model;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder(toBuilder = true)
@Jacksonized
public record SimpleMessage(
    String queueName,
    String message
) {}
