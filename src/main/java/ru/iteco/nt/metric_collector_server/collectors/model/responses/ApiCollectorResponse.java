package ru.iteco.nt.metric_collector_server.collectors.model.responses;


import lombok.Getter;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCollector;
import ru.iteco.nt.metric_collector_server.influx.model.responses.ResponseWithMessage;

import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder
public class ApiCollectorResponse extends DataResponse<ApiCollector> implements ResponseWithMessage<ApiCollectorResponse> {
    private final boolean collecting;
    private final List<DataResponse<?>> influxCollectors;
    private String message;

    public static Mono<ApiCollectorResponse> factoryError(String source, String message, Object ...objects){
        return Mono.just(factoryError(source, message,builder(),objects));
    }

    @Override
    public ApiCollectorResponse setMessage(String message) {
        this.message = message;
        return this;
    }
}
