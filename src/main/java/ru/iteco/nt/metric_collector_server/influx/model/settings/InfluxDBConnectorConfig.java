package ru.iteco.nt.metric_collector_server.influx.model.settings;


import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
public class InfluxDBConnectorConfig {
    private String user;
    private String pass;
    private String url;
    private String dataBase;
    private String retentionPolicy;
    @EqualsAndHashCode.Exclude
    private int minBatchSize = 100;
    @EqualsAndHashCode.Exclude
    private long periodSeconds;

}
