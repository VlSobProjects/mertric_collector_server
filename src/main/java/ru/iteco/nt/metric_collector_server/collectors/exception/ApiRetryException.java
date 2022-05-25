package ru.iteco.nt.metric_collector_server.collectors.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ApiRetryException extends ApiCollectorException {

    private final long totalRetries;
    private final long totalRetriesInARow;

    public ApiRetryException(JsonNode error, long totalRetries, long totalRetriesInARow) {
        super(error);
        this.totalRetries = totalRetries;
        this.totalRetriesInARow = totalRetriesInARow;
    }

    @Override
    public JsonNode getError() {
        JsonNode jsonNode = super.getError();
        if(jsonNode instanceof ObjectNode){
            ((ObjectNode)jsonNode).put("totalRetries",totalRetries).put("totalRetriesInARow",totalRetriesInARow);
        }
        return jsonNode;
    }
}
