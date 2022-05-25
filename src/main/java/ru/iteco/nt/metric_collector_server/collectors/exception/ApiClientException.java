package ru.iteco.nt.metric_collector_server.collectors.exception;

import com.fasterxml.jackson.databind.JsonNode;

public class ApiClientException extends ApiCollectorException {
    public ApiClientException(JsonNode error) {
        super(error);
    }
}
