package ru.iteco.nt.metric_collector_server.collectors.exception;

import com.fasterxml.jackson.databind.JsonNode;

public class ApiServerException extends ApiCollectorException {
    public ApiServerException(JsonNode error) {
        super(error);
    }
}
