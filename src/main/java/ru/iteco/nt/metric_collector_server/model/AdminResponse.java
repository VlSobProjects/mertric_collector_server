package ru.iteco.nt.metric_collector_server.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminResponse {

    private final boolean success;
    private final String message;
    private final Object content;
}
