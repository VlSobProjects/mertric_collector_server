package ru.iteco.nt.metric_collector_server.collectors.web_client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ErrorJson {
    @Builder.Default
    private long time = System.currentTimeMillis();
    private String errorSource;
    private String errorMessage;
    private JsonNode data;
    private boolean serverError;
}
