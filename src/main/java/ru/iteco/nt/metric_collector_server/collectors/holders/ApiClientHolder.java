package ru.iteco.nt.metric_collector_server.collectors.holders;

import lombok.Getter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.holders.ApiCallHolder;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiDataResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCall;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCallResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiClient;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiClientResponse;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
public class ApiClientHolder extends ApiHolder<ApiClientResponse,ApiClient,ApiClientResponse.ApiClientResponseBuilder<ApiClientResponse,?>> {
    private static final AtomicInteger isSource = new AtomicInteger();
    private final WebClient webClient;
    private final Map<Integer, ApiCallHolder> apiCallMap = new ConcurrentHashMap<>();

    public ApiClientHolder(WebClient.Builder builder,ApiClient apiClient){
        super(apiClient,isSource.incrementAndGet());
        this.webClient = apiClient.getClient(builder);

    }

    @SuppressWarnings("unchecked")
    @Override
    public ApiClientResponse.ApiClientResponseBuilder<ApiClientResponse, ?> getBuilder() {
        return (ApiClientResponse.ApiClientResponseBuilder<ApiClientResponse, ?> )
                ApiClientResponse
                        .builder()
                        .apiCalls(apiCallMap.values().stream().map(ApiCallHolder::response).collect(Collectors.toList()));
    }

    public void deleteAll(){
        apiCallMap.values().stream().map(ApiCallHolder::delete).forEach(apiCallMap::remove);
    }

    public Mono<ApiClientResponse> removeApiCallById(int apiCallId){
        return Optional.ofNullable(apiCallMap.remove(apiCallId)).map(ApiCallHolder::delete).map(id->monoResponse())
                .orElseGet(()->responseError("ApiClientHolder.removeApiCallById",String.format("Api call id %s not found",apiCallId)));
    }

    public boolean isApiCall(int apiCallId){
        return apiCallMap.containsKey(apiCallId);
    }

    public Mono<ApiCallResponse> addApiCall(ApiCall apiCall){
        return Mono.fromSupplier(()->{
            ApiCallHolder holder = new ApiCallHolder(this,apiCall);
            apiCallMap.put(holder.getId(),holder);
            return holder.response();
        });
    }


}
