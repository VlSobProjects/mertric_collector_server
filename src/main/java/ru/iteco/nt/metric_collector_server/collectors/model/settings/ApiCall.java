package ru.iteco.nt.metric_collector_server.collectors.model.settings;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ApiCall {
    private int clientId;
    private String name;
    private String method;
    private String uri;
    private JsonNode data;
    private int retry;
    private long retryPeriod;
    private boolean retryBackoff;
    private long checkPeriod;
}
