package ru.iteco.nt.metric_collector_server.collectors.model.settings;

import lombok.Data;

@Data
public class TimeValueConfig {
    private String valueKey;
    private String timeKey;
}
