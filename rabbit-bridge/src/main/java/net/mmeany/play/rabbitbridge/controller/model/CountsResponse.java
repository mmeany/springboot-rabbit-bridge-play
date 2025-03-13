package net.mmeany.play.rabbitbridge.controller.model;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import net.mmeany.play.rabbitbridge.model.QueueCount;

import java.util.List;

@Builder
@Jacksonized
public record CountsResponse(
    List<QueueCount> counts
) {}
