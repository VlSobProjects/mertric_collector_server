package ru.iteco.nt.metric_collector_server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ErrorJson {
    @Builder.Default
    private long time = System.currentTimeMillis();
    private String errorSource;
    private String errorMessage;
    private JsonNode data;
    private boolean serverError;

    public boolean isSameMessageAndSource(ErrorJson err){
        if(err==null) return false;
        return errorSource.equals(err.errorSource) && errorMessage.equals(err.errorMessage);
    }
}
