package ru.iteco.nt.metric_collector_server.collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;


import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import ru.iteco.nt.metric_collector_server.collectors.holders.*;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCallResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiClientResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCollectorResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCallConfig;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiClientConfig;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCollectorConfig;
import ru.iteco.nt.metric_collector_server.utils.Utils;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ApiCollectorService {

    private final ApiClientService apiClientService;

    private static final Map<Integer, ApiClientHolder> CLIENT_MAP = new ConcurrentHashMap<>();

    public Mono<ApiClientResponse> setApiClient(ApiClientConfig apiClientConfig){
        return getErrorIfExist(apiClientConfig).orElseGet(()->Mono.fromSupplier(()->new ApiClientHolder(apiClientService.getBuilder(), apiClientConfig))
                .doOnNext(h-> CLIENT_MAP.put(h.getId(),h))
                .map(ApiClientHolder::response)
        );
    }

    public Mono<ApiClientResponse> getApiClientById(int clientId){
        return Optional.ofNullable(CLIENT_MAP.get(clientId)).map(ApiClientHolder::monoResponse)
                .orElseGet(()->ApiClientResponse.factoryError("ApiCollectorService.getApiClientById","Api client id: "+clientId+" not found"));
    }

    public Mono<List<ApiClientResponse>> getAll(){
        return Mono.fromSupplier(()-> CLIENT_MAP.values().stream().map(ApiClientHolder::response).collect(Collectors.toList()));
    }

    public Mono<List<ApiClientResponse>> deleteClientById(int clientId){
        Optional.ofNullable(CLIENT_MAP.remove(clientId)).ifPresent(ApiClientHolder::deleteAll);
        return getAll();
    }

    public Mono<ApiClientResponse> deleteCallById(int apiCallId){
        return CLIENT_MAP.values().stream().filter(c->c.isApiCall(apiCallId)).findFirst().map(c->c.removeApiCallById(apiCallId))
                .orElseGet(()->ApiClientResponse.factoryError("ApiCollectorService.deleteCallById","Api call id: "+apiCallId+" not found"));
    }

    public Mono<ApiCallResponse> setApiCall(ApiCallConfig apiCallConfig){
        return Optional.ofNullable(CLIENT_MAP.get(apiCallConfig.getClientId()))
                .map(cl->getErrorIfExist(cl.getId(),apiCallConfig)
                        .orElseGet(()->cl.addApiCall(apiCallConfig))
                ).orElseGet(()->getErrorApiCall("client", apiCallConfig.getClientId()));
    }

    public Mono<ApiCallResponse> checkApiClientById(int apiCallId){
        return getApiCallResponse(apiCallId, ApiCallHolder::getApiResponse);
    }

    public Mono<ApiCallResponse> getApiCallById(int apiCallId){
        return getApiCallResponse(apiCallId,ApiCallHolder::monoResponse);
    }

    public Mono<ApiCallResponse> stopCheckApi(int apiCallId){
        return getApiCallResponse(apiCallId,ApiCallHolder::stopCheckApiCall);
    }

    public Mono<ApiCallResponse> stopCollectorByCallId(int apiCallId){
        return getApiCallResponse(apiCallId,ApiCallHolder::stopCollector);
    }

    public Mono<ApiCallResponse> deleteCollectorByCallId(int apiCallId){
        return getApiCallResponse(apiCallId,ApiCallHolder::removeCollector);
    }

    public Mono<ApiCallResponse> setCollectorById(int apiCallId, ApiCollectorConfig apiCollectorConfig){
        return getErrorIfExist(apiCallId,apiCollectorConfig).orElseGet(()->getApiClientHolder(apiCallId)
                .map(h-> Mono.fromSupplier(()->h.setCollector(apiCollectorConfig)))
                .orElseGet(()->getErrorApiCall("client", apiCallId))
        );
    }

    public Mono<ApiCollectorResponse> startCollectorById(int collectorId){
        return getApiCollectorById(collectorId)
                .map(coll->Mono.fromSupplier(coll::startCollecting))
                .orElse(getErrorApiCollector(collectorId));
    }

    public Mono<ApiCollectorResponse> stopCollectorById(int collectorId){
        return getApiCollectorById(collectorId)
                .map(coll->Mono.fromSupplier(coll::stopCollecting))
                .orElse(getErrorApiCollector(collectorId));
    }

    public Mono<ApiCallResponse> deleteCollectorById(int collectorId){
        return getFirstApiClientHolder(c->c.isCollector(collectorId))
                .map(ApiCallHolder::removeCollector)
                .orElse(getErrorApiCall("collector",collectorId));
    }

    public static Mono<Void> deleteAll(){
        return Mono.fromRunnable(()->{
            CLIENT_MAP.values().forEach(ApiClientHolder::deleteAll);
            CLIENT_MAP.clear();
        });
    }

    public static Mono<List<ApiClientResponse>> getAllClients(){
        return Mono.fromSupplier(()->CLIENT_MAP.values().stream().map(ApiHolder::response).collect(Collectors.toList()));
    }

    public Flux<DataCollector.ApiData> getCollectorData(int collectorId, long seconds){
        return getApiCollectorById(collectorId)
                .map(c->c.getApiData(seconds))
                .orElseGet(()->Flux.just(DataCollector.getErrorData("ApiCollectorService.getCollectorData","ApiCollector not found by id: "+collectorId)));
    }

    public Optional<ApiCollectorHolder> getApiCollectorById(int collectorId){
        return getFirstApiClientHolder(c->c.isCollector(collectorId)).map(ApiCallHolder::getCollectorHolder);
    }

    private Mono<ApiCallResponse> getApiCallResponse(int apiCallId,Function<ApiCallHolder,Mono<ApiCallResponse>> function){
        return getApiClientHolder(apiCallId)
                .map(function)
                .orElseGet(()-> getErrorApiCall("call",apiCallId));
    }

    private Optional<ApiCallHolder> getFirstApiClientHolder(Predicate<ApiCallHolder> predicate){
        return CLIENT_MAP.values()
                .stream()
                .flatMap(c->c.getApiCallMap().values().stream())
                .filter(predicate)
                .findFirst();
    }

    private Optional<ApiCallHolder> getApiClientHolder(int apiCallId){
        return getFirstApiClientHolder(call->call.getId()==apiCallId);
    }

    private static Mono<ApiCallResponse> getErrorApiCall(String notFoundName,int id){
        return ApiCallResponse.factoryError("ApiCollectorService",String.format("Api %s not found by id: %s",notFoundName,id));
    }

    private static Mono<ApiCollectorResponse> getErrorApiCollector(int id){
        return ApiCollectorResponse.factoryError("ApiCollectorService",String.format("Api collector not found by by id: %s",id));
    }

    private Optional<Mono<ApiClientResponse>> getErrorIfExist(ApiClientConfig config){
        return CLIENT_MAP.values().stream().map(c->c.errorIfExist(config)).filter(Objects::nonNull).findFirst();
    }

    private Optional<Mono<ApiCallResponse>> getErrorIfExist(int apiCallId,ApiCallConfig config){
        return getApiClientHolder(apiCallId).map(h->h.errorIfExist(config));
    }

    private Optional<Mono<ApiCallResponse>> getErrorIfExist(int apiCallId,ApiCollectorConfig config){
        return getApiClientHolder(apiCallId)
                .map(h->h.getCollectorHolder()==null? null :
                        Optional.ofNullable(h.getCollectorHolder().errorIfExist(config))
                                .map(m->m.flatMap(ce->
                                        Utils.modifyData(ApiCallResponse.factoryError("ApiCollectorService","duplicated ApiCollectorConfig"),j->((ObjectNode)j.get("error")).set("data",ce.getData().get("error").get("data")))
                                )).orElse(null)
                );
    }

}
