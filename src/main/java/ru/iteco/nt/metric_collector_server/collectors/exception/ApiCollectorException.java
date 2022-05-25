package ru.iteco.nt.metric_collector_server.collectors.exception;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

public abstract class ApiCollectorException extends Exception{
    @Getter
    private final JsonNode error;

    public ApiCollectorException(JsonNode error){
        super();
        this.error = error;
    }
}
