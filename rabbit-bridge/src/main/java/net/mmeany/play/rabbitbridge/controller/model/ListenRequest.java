package net.mmeany.play.rabbitbridge.controller.model;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Jacksonized
public record ListenRequest(
    List<String> queueNames
) {}
