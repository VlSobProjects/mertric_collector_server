package ru.iteco.nt.metric_collector_server.collectors.holders;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCallConfig;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCallResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCollectorConfig;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Getter
public class ApiCallHolder extends DataCollector<ApiCallResponse, ApiCallConfig,ApiCallResponse.ApiCallResponseBuilder<ApiCallResponse,?>> {
    private static final AtomicInteger isSource = new AtomicInteger();
    private ApiCollectorHolder collectorHolder;
    private final Mono<JsonNode> request;
    private final Retry retry;
    private Disposable checkApiCall;

    public ApiCallHolder(ApiClientHolder apiClientHolder, ApiCallConfig apiCallConfig){
        super(apiCallConfig,isSource.incrementAndGet());
        request = apiCallConfig.getMethod().trim().equalsIgnoreCase("get") ?
                Utils.getWithOnHttpErrorResponseSpec("api call",apiClientHolder.getWebClient().get().uri(apiCallConfig.getUri()).retrieve()).doOnNext(this::setData) :
                Mono.just(Utils.getError("ApiCallHolder",String.format("Method: %s not implemented", apiCallConfig.getMethod())))
        ;
        retry = apiCallConfig.getRetry()>0 && apiCallConfig.getRetryPeriod()>0 ?
                apiCallConfig.isRetryBackoff() ?
                        Retry.backoff(apiCallConfig.getRetry(), Duration.ofSeconds(apiCallConfig.getRetryPeriod())) :
                        Retry.fixedDelay(apiCallConfig.getRetry(),Duration.ofSeconds(apiCallConfig.getRetryPeriod())):
                null;

        setDoOnError(()->{
            log.debug("Do on Error");
            if(collectorHolder!=null && collectorHolder.isCollecting())
                collectorHolder.stop();
            if(!isChecking() && apiCallConfig.getCheckPeriod()>0){
                checkApiCall = setCheckApiCall(apiCallConfig.getCheckPeriod(),request);
            }
        });
        setDoAfterError(()->{
            log.debug("Do After Error");
            if(isChecking()) checkApiCall.dispose();
            if(collectorHolder!=null && !collectorHolder.isCollecting()) collectorHolder.start();
        });
        startOnError();
    }

    private Disposable setCheckApiCall(long seconds,Mono<JsonNode> request){
        return request.delayElement(Duration.ofSeconds(seconds)).repeat(this::isFail).subscribe();
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
        if(data.getData()==null || isFail()){
            return request;
        } else return Mono.just(data.getData());
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

}
