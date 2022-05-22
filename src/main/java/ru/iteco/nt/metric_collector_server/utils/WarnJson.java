package ru.iteco.nt.metric_collector_server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class WarnJson {
    @Builder.Default
    private long time = System.currentTimeMillis();
    private String warSource;
    private String warMessage;
    private JsonNode data;
    private boolean warnError;
}
