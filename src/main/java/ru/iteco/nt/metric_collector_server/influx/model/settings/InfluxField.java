package ru.iteco.nt.metric_collector_server.influx.model.settings;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class InfluxField {
    private String path;
    private String name;
    private boolean tag;
    private boolean time;
    private JsonNode value;
    private List<InfluxField> children;
}
