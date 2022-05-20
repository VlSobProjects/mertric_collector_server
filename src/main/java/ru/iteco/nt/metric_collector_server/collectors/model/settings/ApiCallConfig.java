package ru.iteco.nt.metric_collector_server.collectors.model.settings;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ApiCallConfig {
    @EqualsAndHashCode.Include
    private int clientId;
    private String name;
    @EqualsAndHashCode.Include
    private String method;
    @EqualsAndHashCode.Include
    private String uri;
    @EqualsAndHashCode.Include
    private JsonNode data;
    private int retry;
    private long retryPeriod;
    private boolean retryBackoff;
    private long checkPeriod;
}
