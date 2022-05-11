package ru.iteco.nt.metric_collector_server.collectors;

import lombok.*;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;


import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import ru.iteco.nt.metric_collector_server.collectors.holders.ApiCallHolder;
import ru.iteco.nt.metric_collector_server.collectors.holders.ApiClientHolder;
import ru.iteco.nt.metric_collector_server.collectors.holders.ApiCollectorHolder;
import ru.iteco.nt.metric_collector_server.collectors.holders.DataCollector;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCallResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiClientResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCollectorResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCall;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiClient;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCollector;


import java.time.Duration;
import java.util.List;
import java.util.Map;
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



    public Mono<ApiClientResponse> setApiClient(ApiClient apiClient){
        return Mono.fromSupplier(()->new ApiClientHolder(apiClientService.getBuilder(),apiClient))
                .doOnNext(h->clientMap.put(h.getId(),h))
                .map(ApiClientHolder::response);
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



    public Mono<ApiCallResponse> setApiCall(ApiCall apiCall){
        return Optional.ofNullable(clientMap.get(apiCall.getClientId()))
                .map(h->h.addApiCall(apiCall))
                .orElseGet(()->getErrorApiCall("client",apiCall.getClientId()));
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

    public Mono<ApiCallResponse> setCollectorById(int apiCallId, ApiCollector apiCollector){
        return getApiCallResponse(apiCallId,call->Mono.fromSupplier(()->call.setCollector(apiCollector)));
    }


    public Mono<ApiCollectorResponse> startCollectorById(int collectorId){
        return getApiCollectorById(collectorId)
                .map(coll->Mono.fromSupplier(coll::startCollecting))
                .orElse(getErrorApiCollector("collector",collectorId));
    }

    public Mono<ApiCollectorResponse> stopCollectorById(int collectorId){
        return getApiCollectorById(collectorId)
                .map(coll->Mono.fromSupplier(coll::stopCollecting))
                .orElse(getErrorApiCollector("collector",collectorId));
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



    private Optional<ApiCollectorHolder> getApiCollectorById(int collectorId){
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

    private static Mono<ApiCollectorResponse> getErrorApiCollector(String notFoundName,int id){
        return ApiCollectorResponse.factoryError("ApiCollectorService",String.format("Api %s by id: %s",notFoundName,id));
    }


//
//    public List<ApiClientHolder> dropClient(int id){
//        clientMap.remove(id).apiCallMap.values().forEach(c->c.apiCollector.disposable.dispose());
//        return new ArrayList<>(clientMap.values());
//    }
//
//    public ApiClient getAppClientInfoById(int id){
//        return clientMap.get(id).settings;
//    }
//
//    public Mono<JsonNode> setGetCall(String name,int clientId, String uri){
//        return Optional.ofNullable(clientMap.get(clientId))
//                .map(ah-> {
//                            ApiCallHolder holder = ApiCallHolder.builder()
//                                    .clientId(clientId)
//                                    .uri(uri)
//                                    .id(idCallSource.incrementAndGet())
//                                    .name(name)
//                                    .requestCall(Utils.getWithOnHttpErrorResponseSpec("api call",ah.webClient.get().uri(uri).retrieve()))
//                                    .build();
//                            ah.addApiCall(holder);
//                            return holder.request();
//                        }
//                ).orElse(Mono.just(Utils.getError("ApiCollectorService",String.format("ApiClient id: %s not found for call %s : %s",clientId,name,uri))));
//    }
//
//
//    public Mono<JsonNode> apiCallById(int callId){
//        return clientMap.values().stream().filter(c->c.apiCallMap.containsKey(callId)).findFirst()
//                .map(c->c.apiCallMap.get(callId).request())
//                .orElseGet(()->Mono.just(Utils.getError("ApiCollectorService",String.format("ApiCall id: %s not found",callId))));
//    }
//
//    public Mono<JsonNode> setGetCall(ApiCall apiCall){
//        return setGetCall(apiCall.getName(),apiCall.getClientId(), apiCall.getUri());
//    }
//
//    public Flux<ApiCollector> getDataCall(int callId,long millis){
//        ApiCallHolder holder = clientMap
//                .values()
//                .stream()
//                .flatMap(c->c.apiCallMap.values().stream())
//                .filter(c->c.id==callId).findFirst().orElse(null);
//        if(holder==null){
//            return Flux.just(ApiCollector.errorFactory("ApiCallHolder is null id: "+callId));
//        }
//        if(holder.fail){
//            return Flux.just(ApiCollector.errorFactory("ApiCallHolder is fail id: "+callId,holder));
//        }
//
//        ApiCollector collector = Optional.ofNullable(holder.apiCollector).orElseGet(()->setCollector(holder,millis));
//        return collector == null? Flux.just(ApiCollector.errorFactory("Fail to create ApiCollector from ApiCallHolder",holder)): Flux.concat(Mono.just(collector),Mono.just(collector).delayElement(Duration.ofMillis(millis)).repeat());
//    }
//
//    private ApiCollector setCollector(ApiCallHolder holder,long millis){
//        if(holder.requestCall!=null){
//            ApiCollector collector = new ApiCollector(holder.id,idCollectorSource.incrementAndGet(),millis);
//            holder.setApiCollector(collector);
//            collector.setDisposable(
//                    Flux.concat(holder.requestCall,holder.requestCall.delayElement(Duration.ofMillis(millis)).repeat())
//                            .flatMap(j->{
//                                if(j.has("error")){
//                                    return Mono.error(new ApiCollectorException(j));
//                                } else return Mono.just(j);
//                            }).retryWhen(Retry.backoff(3,Duration.ofSeconds(2)))
//                            .doOnError(th->(th.getCause() instanceof ApiCollectorException),th-> {
//                                ApiCollectorException e = (ApiCollectorException) th.getCause();
//                                log.info("error: {}",e.getError());
//                                holder.setData(e.getError());
//                            }).subscribe((collector::setData))
//            );
//            return collector;
//        } else return null;
//    }

//
//    @Getter
//    public static class ApiClientHolder{
//        private final int id;
//        private final String name;
//        private final ApiClient settings;
//        @JsonIgnore
//        private final WebClient webClient;
//        private final Map<Integer, ApiCallHolder> apiCallMap;
//
//
//        @JsonCreator
//        public ApiClientHolder(
//                @JsonProperty("id") int id
//                , @JsonProperty("name") String name
//                , @JsonProperty("settings") ApiClient settings
//                , @JsonProperty("apiCallMap") Map<Integer, ApiCallHolder> apiCallMap)
//        {
//            this(id,name,settings,apiCallMap,null);
//        }
//
//        public ApiClientHolder(int id,String name,ApiClient settings,Map<Integer, ApiCallHolder> apiCallMap,WebClient webClient){
//            this.id = id;
//            this.name = name;
//            this.settings = settings;
//            this.apiCallMap = apiCallMap==null? new ConcurrentHashMap<>():new ConcurrentHashMap<>(apiCallMap);
//            this.webClient = webClient;
//
//        }
//
//        public void addApiCall(ApiCallHolder apiCall){
//            apiCallMap.put(apiCall.id,apiCall);
//        }
//
//        public ApiClientHolder(int id,String name,ApiClient settings,WebClient webClient){
//            this(id,name,settings,null,webClient);
//        }
//
//
//    }
//
//
//    @Setter
//    @Getter
//    @Builder
//    @AllArgsConstructor
//    public static class ApiCallHolder{
//        private final int id;
//        private final ApiCall settings;
//        @JsonIgnore
//        private final Supplier<Object> body;
//        @JsonIgnore
//        private final Mono<JsonNode> requestCall;
//        @JsonIgnore
//        private final Retry retry;
//        private JsonNode data;
//        private long lastCall;
//        private ApiCollector apiCollector;
//        private boolean fail;
//
//        @JsonCreator
//        public ApiCallHolder(
//                @JsonProperty("id") int id
//                , @JsonProperty("settings") ApiCall settings
//                , @JsonProperty("data") JsonNode data
//                , @JsonProperty("lastCall") long lastCall
//                , @JsonProperty("apiCollector") ApiCollector apiCollector
//        ){
//            this.settings = settings;
//            this.id= id;
//            this.data = data;
//            this.lastCall = lastCall;
//            this.apiCollector = apiCollector;
//            body=null;
//            requestCall=null;
//            retry = null;
//        }
//
//        public static ApiCallHolder factoryNew(ApiClientHolder holder,int id,ApiCall settings){
//            Mono<JsonNode> request = Utils.getWithOnHttpErrorResponseSpec("api call",holder.webClient.get().uri(settings.getUri()).retrieve());
//            Retry retry = settings.getRetry()>0 && settings.getRetryPeriod()>0 ?
//                    settings.isRetryBackoff() ?
//                            Retry.backoff(settings.getRetry(),Duration.ofSeconds(settings.getRetryPeriod())) :
//                            Retry.fixedDelay(settings.getRetry(),Duration.ofSeconds(settings.getRetryPeriod())):
//                    null;
//
//
//        }
//
//        private ApiCallHolder setData(JsonNode data){
//            this.data = data;
//            fail = data==null || data.has("error");
//            lastCall = System.currentTimeMillis();
//            return this;
//        }
//        private Mono<JsonNode> request(){
//            return requestCall!=null?
//                    requestCall.map(this::setData).map(Utils::valueToTree) :
//                    Mono.just(Utils.getError("ApiCallHolder","Unexpected requestCall is null",this));
//        }
//        public synchronized void setApiCollector(ApiCollector apiCollector){
//            this.apiCollector = apiCollector;
//        }
//        public synchronized void setApiCollector(int ){
//
//        }
//        public synchronized ApiCollector getApiCollector(){
//            return apiCollector;
//        }
//    }
//
//
//    @Getter
//    @Accessors(chain = true)
//    @RequiredArgsConstructor
//    public static class ApiCollector{
//        private final int callId;
//        private final int id;
//        private final long period;
//        @Setter
//        @JsonIgnore
//        private Disposable disposable;
//        private long lastTime;
//        private JsonNode lastData;
//
//        @JsonCreator
//        public ApiCollector(@JsonProperty("id") int id
//                ,@JsonProperty("callId") int callId
//                ,@JsonProperty("period") long period
//                ,@JsonProperty("lastTime") long lastTime
//                ,@JsonProperty("lastData") JsonNode lastData){
//            this.callId = callId;
//            this.id = id;
//            this.period = period;
//            this.lastTime = lastTime;
//            this.lastData = lastData;
//        }
//
//        public synchronized ApiCollector setData(JsonNode data){
//            lastTime = System.currentTimeMillis();
//            lastData = data;
//            return this;
//        }
//        public synchronized JsonNode getLastData(){
//            return lastData;
//        }
//
//        public static ApiCollector errorFactory(ErrorJson errorJson){
//            return new ApiCollector(0,0,0).setData(Utils.getError(errorJson));
//        }
//
//        public static ApiCollector errorFactory(String message,Object ... objects){
//            return new ApiCollector(0,0,0).setData(Utils.getError("ApiCollectorService.ApiCollector",message,objects));
//        }
//    }


}
