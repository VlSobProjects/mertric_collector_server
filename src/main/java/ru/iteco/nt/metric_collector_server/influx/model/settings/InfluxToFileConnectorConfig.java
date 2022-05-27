package ru.iteco.nt.metric_collector_server.influx.model.settings;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class InfluxToFileConnectorConfig implements WriterConfig {

    @EqualsAndHashCode.Exclude
    private int minBatchSize = 100;
    @EqualsAndHashCode.Exclude
    private long periodSeconds;
    private String filePath;
    private String fileName = "influx_points.txt";
    private boolean addDateTime;
    private String dateTimeFormat;
}
