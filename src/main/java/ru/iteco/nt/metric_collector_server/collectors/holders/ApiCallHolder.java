package ru.iteco.nt.metric_collector_server.collectors.holders;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiDataResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCall;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCallResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCollector;
import ru.iteco.nt.metric_collector_server.collectors.web_client.Utils;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Getter
public class ApiCallHolder extends DataCollector<ApiCallResponse,ApiCall,ApiCallResponse.ApiCallResponseBuilder<ApiCallResponse,?>> {
    private static final AtomicInteger isSource = new AtomicInteger();
    private ApiCollectorHolder collectorHolder;
    private final Mono<JsonNode> request;
    private final Retry retry;
    private Disposable checkApiCall;




    public ApiCallHolder(ApiClientHolder apiClientHolder, ApiCall apiCall){
        super(apiCall,isSource.incrementAndGet());
        request = apiCall.getMethod().trim().equalsIgnoreCase("get") ?
                Utils.getWithOnHttpErrorResponseSpec("api call",apiClientHolder.getWebClient().get().uri(apiCall.getUri()).retrieve()) :
                Mono.just(Utils.getError("ApiCallHolder",String.format("Method: %s not implemented",apiCall.getMethod())))
        ;
        retry = apiCall.getRetry()>0 && apiCall.getRetryPeriod()>0 ?
                apiCall.isRetryBackoff() ?
                        Retry.backoff(apiCall.getRetry(), Duration.ofSeconds(apiCall.getRetryPeriod())) :
                        Retry.fixedDelay(apiCall.getRetry(),Duration.ofSeconds(apiCall.getRetryPeriod())):
                null;
        if(apiCall.getCheckPeriod()>0){
            setStartOnError(()->{
                        if(!isChecking()){
                            checkApiCall = setCheckApiCall(apiCall.getCheckPeriod(),request);
                        }
                    }
            );
            getStartOnError().run();
        }


    }

    private Disposable setCheckApiCall(long seconds,Mono<JsonNode> request){
        return request.delayElement(Duration.ofSeconds(seconds)).repeat(this::isFail).doOnComplete(()->{
            if(collectorHolder!=null)
                collectorHolder.startCollecting();
        }).subscribe(this::setData);
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

    public synchronized ApiCallResponse setCollector(ApiCollector apiCollector){
        if(collectorHolder==null){
            collectorHolder = new ApiCollectorHolder(this,apiCollector);
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
        return request.map(j->{
            setData(j);
            return response();
        });
    }

    public boolean isCollector(int collectorId){
        return collectorHolder!=null && collectorHolder.getId()==collectorId;
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
