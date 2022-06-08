package ru.iteco.nt.metric_collector_server.collectors.model.settings;

import lombok.Data;
import ru.iteco.nt.metric_collector_server.influx.model.settings.HasJsonPath;

@Data
public class DynamicTimeMillisParamConfig implements HasJsonPath {
    private String name;
    private String path;
    private String key;
    private long addMillis;
    private long addToCurIfNull;

    public boolean hasKey(){
        return key!=null && !key.trim().isEmpty();
    }
}
