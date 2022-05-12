package ru.iteco.nt.metric_collector_server.influx.model.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
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
