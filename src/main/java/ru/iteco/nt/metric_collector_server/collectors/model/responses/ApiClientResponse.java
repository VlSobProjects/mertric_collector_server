package ru.iteco.nt.metric_collector_server.collectors.model.responses;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiClientConfig;

import java.util.List;
@Getter
@SuperBuilder
public class ApiClientResponse extends DataResponse<ApiClientConfig> {
    private final List<ApiCallResponse> apiCalls;

    public static Mono<ApiClientResponse> factoryError(String source, String message, Object ...objects){
        return Mono.just(factoryError(source, message,builder(),objects));
    }

}
