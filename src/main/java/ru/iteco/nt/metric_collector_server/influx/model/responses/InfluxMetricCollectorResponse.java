package ru.iteco.nt.metric_collector_server.influx.model.responses;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.DataResponse;

@Getter
@SuperBuilder
public class InfluxMetricCollectorResponse extends DataResponse<JsonNode> implements ResponseWithMessage<InfluxMetricCollectorResponse> {

    private final boolean collecting;
    private final WriterResponse<?> writer;
    private String message;

    public static Mono<InfluxMetricCollectorResponse> factoryError(String source, String message, Object ...objects){
        return Mono.just(factoryError(source, message,builder(),objects));
    }

    public InfluxMetricCollectorResponse setMessage(String message){
        this.message = message;
        return this;
    }

}
