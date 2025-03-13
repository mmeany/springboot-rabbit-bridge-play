package net.mmeany.play.rabbitbridge.model;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder(toBuilder = true)
@Jacksonized
public record QueueCount(
    String queueName,
    int count
) {}
