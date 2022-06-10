package ru.iteco.nt.metric_collector_server.collectors.holders;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import ru.iteco.nt.metric_collector_server.SpringContext;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCallConfig;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCallResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCollectorConfig;

import ru.iteco.nt.metric_collector_server.collectors.model.settings.TimeValueConfig;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;


@Slf4j
@Getter
public class ApiCallHolder extends DataCollector<ApiCallResponse, ApiCallConfig,ApiCallResponse.ApiCallResponseBuilder<ApiCallResponse,?>> {
    private static final AtomicInteger isSource = new AtomicInteger();
    private ApiCollectorHolder collectorHolder;
    private final Mono<JsonNode> request;
    private Disposable checkApiCall;


    public ApiCallHolder(ApiClientHolder apiClientHolder, ApiCallConfig apiCallConfig){
        super(apiCallConfig,isSource.incrementAndGet());
        request = Mono.fromSupplier(()->getRequest(apiClientHolder)).flatMap(Function.identity()).doOnNext(this::clearNullValue).doOnNext(this::setData);
        setDoOnError(Utils.runWithErrorLog(()->{
            log.debug("Do on Error data: {}",getData().getData());
            if(collectorHolder!=null && collectorHolder.isCollecting()){
                collectorHolder.stop();
                log.debug("Do on Error data: collectorHolder is not null and isCollecting: {}",collectorHolder.isCollecting());
            }
            log.debug("Do on Error isChecking: {} apiCallConfig.getCheckPeriod(): {}",isChecking(),apiCallConfig.getCheckPeriod());
            if(!isChecking() && apiCallConfig.getCheckPeriod()>0){
                log.debug("set up setCheckApiCall: {} sec",apiCallConfig.getCheckPeriod());
                checkApiCall = setCheckApiCall(apiCallConfig.getCheckPeriod(),this.request);
            }
        }));
        setDoAfterError(Utils.runWithErrorLog(()->{
            log.debug("Do After Error data: {}",getData().getData());
            if(isChecking()) checkApiCall.dispose();
            if(collectorHolder!=null) {
                log.debug("Do After Error data: collectorHolder is not null and isCollecting: {}",collectorHolder.isCollecting());
                collectorHolder.validateMetricCollectors(getData().getData());
                collectorHolder.start();
            }

        }));
        this.request.subscribe();
    }

    private Mono<JsonNode> getRequest(ApiClientHolder apiClientHolder){
        if(!getSettings().getMethod().trim().equalsIgnoreCase("get"))
            return Mono.just(Utils.getError("ApiCallHolder.getRequest",String.format("Method: %s not implemented", getSettings().getMethod()),getSettings()));
        List<JsonNode> errors = new ArrayList<>();
        WebClient.ResponseSpec response = apiClientHolder.getWebClient().get().uri(getUri(errors::add)).retrieve();
        if(errors.size()>0) return Mono.just(Utils.getError("ApiCallHolder.getRequest","fail to create dynamic timestamp request param",getSettings(),errors));
        if(getSettings().isRetrySet()){
            return getSettings().isRetryBackoff() ?
                   Utils.getWithOnHttpErrorResponseSpec(getSettings().getApiCallInfo(),response,Retry.backoff(
                           getSettings().getRetry()
                           ,Duration.ofSeconds(getSettings().getRetryPeriod()))
                           .transientErrors(getSettings().isRetryTransient())) :
                    Utils.getWithOnHttpErrorResponseSpec(getSettings().getApiCallInfo(),response,Retry.fixedDelay(
                                    getSettings().getRetry()
                                    ,Duration.ofSeconds(getSettings().getRetryPeriod()))
                            .transientErrors(getSettings().isRetryTransient()));
        } else return Utils.getWithOnHttpErrorResponseSpec(getSettings().getApiCallInfo(),response);
    }

    private String getUri(Consumer<JsonNode> onError){
       if(getSettings().getTimeMillisParamConfigs()==null || getSettings().getTimeMillisParamConfigs().isEmpty())
           return getSettings().getUri();
       return SpringContext.getBean(DynamicTimeMillisParamConverter.class).uriWithParams(getSettings().getTimeMillisParamConfigs(),getSettings().getUri(),getLastGood(),onError);
    }

    private Disposable setCheckApiCall(long seconds,Mono<JsonNode> request){
        return request.delayElement(Duration.ofSeconds(seconds)).doOnNext(j->log.debug("CheckApiCall: {}",j)).repeat(this::isFail).subscribe();
    }

    private boolean isChecking(){
        return checkApiCall!=null && !checkApiCall.isDisposed();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ApiCallResponse.ApiCallResponseBuilder<ApiCallResponse, ?> getBuilder() {
        return (ApiCallResponse.ApiCallResponseBuilder<ApiCallResponse, ?>)
                ApiCallResponse.builder()
                .collectorResponse(collectorHolder==null ? null : collectorHolder.response())
                .isChecking(isChecking());
    }

    public synchronized ApiCallResponse setCollector(ApiCollectorConfig apiCollectorConfig){
        if(collectorHolder==null || !collectorHolder.getSettings().equals(apiCollectorConfig)){
            if(collectorHolder!=null) collectorHolder.stop();
            collectorHolder = new ApiCollectorHolder(this, apiCollectorConfig);
            collectorHolder.start();
        }
        return response();
    }

    public synchronized Mono<ApiCallResponse> stopCollector(){
        if(collectorHolder!=null){
            collectorHolder.stopCollecting();
        }
        return monoResponse();
    }

    public Mono<ApiCallResponse> getApiResponse(){
        return request.then(monoResponse());
    }

    public boolean isCollector(int collectorId){
        return collectorHolder!=null && collectorHolder.getId()==collectorId;
    }

    public boolean isCollector(){
        return collectorHolder!=null;
    }

    public synchronized Mono<ApiCallResponse> removeCollector(){
        deleteCollector();
        return monoResponse();
    }

    private void deleteCollector(){
        if(collectorHolder!=null){
            collectorHolder.stopCollecting();
            collectorHolder = null;
        }
    }

    public int delete(){
        stopCheck();
        deleteCollector();
        return getId();
    }

    public Mono<JsonNode> lastApiCall(){
        ApiData data = getData();
        if(data.getData()==null) return request;
        else return Mono.just(data.getData());
    }

    public Mono<ApiCallResponse> stopCheckApiCall(){
        stopCheck();
        return monoResponse();
    }

    public void stopCheck(){
        if(checkApiCall!=null){
            checkApiCall.dispose();
            checkApiCall = null;
        }
    }

    private void clearNullValue(JsonNode source){
        if(getSettings().getTimeValueConfig()==null || source==null) return;
        TimeValueConfig timeValueConfig = getSettings().getTimeValueConfig();
        List<JsonNode> values = source.findValues(timeValueConfig.getValueKey());
        List<JsonNode> timestamps = source.findValues(timeValueConfig.getTimeKey());
        for (int i = 0; i < values.size(); i++) {
            JsonNode v = values.get(i);
            JsonNode t = timestamps.get(i);
            if(v instanceof ArrayNode && t instanceof ArrayNode){
                Utils.clearNulls((ArrayNode)v,(ArrayNode)t);
            }
        }

    }

}
