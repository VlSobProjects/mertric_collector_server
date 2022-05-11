package ru.iteco.nt.metric_collector_server.influx.model.settings;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class InfluxMetricCollectorConfig {
    private int apiCollectorId;
    private int influxDbId;
    private List<InfluxField> fields;
    private String measurement;
    private String path;
    private boolean setTime;

}
