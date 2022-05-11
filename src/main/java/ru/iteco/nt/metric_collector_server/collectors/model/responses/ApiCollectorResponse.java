package ru.iteco.nt.metric_collector_server.collectors.model.responses;


import lombok.Getter;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCollector;

@Getter
@SuperBuilder
public class ApiCollectorResponse extends DataResponse<ApiCollector> {
    private final boolean collecting;

    public static Mono<ApiCollectorResponse> factoryError(String source, String message, Object ...objects){
        return Mono.just(factoryError(source, message,builder(),objects));
    }
}
