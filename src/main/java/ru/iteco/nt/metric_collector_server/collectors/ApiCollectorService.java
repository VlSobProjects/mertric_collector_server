package ru.iteco.nt.metric_collector_server.collectors;

import lombok.*;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;


import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.collectors.holders.ApiCallHolder;
import ru.iteco.nt.metric_collector_server.collectors.holders.ApiClientHolder;
import ru.iteco.nt.metric_collector_server.collectors.holders.ApiCollectorHolder;
import ru.iteco.nt.metric_collector_server.collectors.holders.DataCollector;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCallResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiClientResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCollectorResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCallConfig;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiClientConfig;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCollectorConfig;
import ru.iteco.nt.metric_collector_server.utils.Utils;


import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ApiCollectorService {

    private final ApiClientService apiClientService;

    private final Map<Integer, ApiClientHolder> clientMap = new ConcurrentHashMap<>();



    public Mono<ApiClientResponse> setApiClient(ApiClientConfig apiClientConfig){
        return getErrorIfExist(apiClientConfig).orElseGet(()->Mono.fromSupplier(()->new ApiClientHolder(apiClientService.getBuilder(), apiClientConfig))
                .doOnNext(h->clientMap.put(h.getId(),h))
                .map(ApiClientHolder::response)
        );
    }

    public Mono<ApiClientResponse> getApiClientById(int clientId){
        return Optional.ofNullable(clientMap.get(clientId)).map(ApiClientHolder::monoResponse)
                .orElseGet(()->ApiClientResponse.factoryError("ApiCollectorService.getApiClientById","Api client id: "+clientId+" not found"));
    }

    public Mono<List<ApiClientResponse>> getAll(){
        return Mono.fromSupplier(()->clientMap.values().stream().map(ApiClientHolder::response).collect(Collectors.toList()));
    }

    public Mono<List<ApiClientResponse>> deleteClientById(int clientId){
        Optional.ofNullable(clientMap.remove(clientId)).ifPresent(ApiClientHolder::deleteAll);
        return getAll();
    }

    public Mono<ApiClientResponse> deleteCallById(int apiCallId){
        return clientMap.values().stream().filter(c->c.isApiCall(apiCallId)).findFirst().map(c->c.removeApiCallById(apiCallId))
                .orElseGet(()->ApiClientResponse.factoryError("ApiCollectorService.deleteCallById","Api call id: "+apiCallId+" not found"));
    }


    public Mono<ApiCallResponse> setApiCall(ApiCallConfig apiCallConfig){
        return Optional.ofNullable(clientMap.get(apiCallConfig.getClientId()))
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
        return getErrorIfExist(apiCallId,apiCollectorConfig).orElseGet(()->getApiClientHolder(apiCallId).map(h-> {
                h.getCollectorHolder().stop();
                return Mono.fromSupplier(()->h.setCollector(apiCollectorConfig));
            }).orElseGet(()->getErrorApiCall("client", apiCallId))
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


    public Mono<Void> deleteAll(){
        return Mono.fromRunnable(()->{
            clientMap.values().forEach(ApiClientHolder::deleteAll);
            clientMap.clear();
        });
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
        return clientMap.values()
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
        return clientMap.values().stream().map(c->c.errorIfExist(config)).filter(Objects::nonNull).findFirst();
    }

    private Optional<Mono<ApiCallResponse>> getErrorIfExist(int apiCallId,ApiCallConfig config){
        return getApiClientHolder(apiCallId).map(h->h.errorIfExist(config));
    }
    private Optional<Mono<ApiCallResponse>> getErrorIfExist(int apiCallId,ApiCollectorConfig config){
        return getApiClientHolder(apiCallId)
                .map(h->h.getCollectorHolder()==null? null :
                        h.getCollectorHolder().errorIfExist(config).flatMap(ce-> ApiCallResponse.factoryError("ApiCollectorService","duplicated ApiCollectorConfig",config,ce.getSettings())));
    }

}
