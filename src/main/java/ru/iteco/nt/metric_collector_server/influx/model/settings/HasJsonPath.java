package ru.iteco.nt.metric_collector_server.influx.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface HasJsonPath {

    String getPath();

    @JsonIgnore
    default boolean hasPath(){
        return getPath()!=null && !getPath().trim().isEmpty() && getPath().startsWith("/");
    }
}
