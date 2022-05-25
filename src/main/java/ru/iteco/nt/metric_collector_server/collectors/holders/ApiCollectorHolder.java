package ru.iteco.nt.metric_collector_server.collectors.holders;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCollectorConfig;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCollectorResponse;
import ru.iteco.nt.metric_collector_server.MetricCollector;


import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@Slf4j
public class ApiCollectorHolder extends DataCollector<ApiCollectorResponse, ApiCollectorConfig,ApiCollectorResponse.ApiCollectorResponseBuilder<ApiCollectorResponse,?>> {
    private static final AtomicInteger isSource = new AtomicInteger();
    private final Flux<JsonNode> collector;
    private final ApiCallHolder apiCallHolder;
    private Disposable collecting;
    private final List<MetricCollector<?,?,?,?>> metricCollectors = new CopyOnWriteArrayList<>();


    public ApiCollectorHolder(ApiCallHolder apiCallHolder, ApiCollectorConfig apiCollectorConfig) {
        super(apiCollectorConfig,isSource.incrementAndGet());
        this.apiCallHolder = apiCallHolder;
        collector = Flux.concat(apiCallHolder.lastApiCall(),Mono.delay(Duration.ofMillis(apiCollectorConfig.getPeriodMillis())).then(apiCallHolder.getRequest()).repeat()).filter(j->!j.has("error")).doOnNext(this::validateNewMetricCollectors).doOnNext(this::setData).share();
    }

    public Mono<ApiCollectorResponse> addAndStarMetricCollector(MetricCollector<?,?,?,?> metricCollector){
        return Mono.fromSupplier(()->{
            metricCollectors.add(metricCollector);
            return response();
        });
    }

    public ApiCollectorResponse startCollecting(){
        start();
        return response();
    }

    public synchronized void start(){
        if(!apiCallHolder.isFail()){
            if(!isCollecting()) {
                collecting = collector.subscribe();
            }
        }
    }

    private void validateNewMetricCollectors(JsonNode data){
        metricCollectors.stream().filter(m->!m.isDataValidated()).forEach(m->m.validateDataAndSet(data));
        metricCollectors.stream().filter(m->!m.isRunning()).filter(MetricCollector::isValidateDataPass).forEach(m->m.start(collector));
    }

    public void validateMetricCollectors(JsonNode jsonNode){
        metricCollectors.forEach(c->c.validateDataAndSet(jsonNode));
    }

    public ApiCollectorResponse stopCollecting(){
        stop();
        return response();
    }

    public synchronized void stop(){
        if(isCollecting()){
            collecting.dispose();
            collecting = null;
            metricCollectors.forEach(MetricCollector::stop);
        }
    }

    public Flux<ApiData> getApiData(long periodSeconds){
        Duration delay = Duration.ofMillis(Math.max(periodSeconds * 1000, getSettings().getPeriodMillis()));
        return Flux.concat(Mono.fromSupplier(this::getData).delayElement(delay).repeat());
    }

    public boolean isCollecting(){
        return collecting != null && !collecting.isDisposed();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ApiCollectorResponse.ApiCollectorResponseBuilder<ApiCollectorResponse, ?> getBuilder() {
        return (ApiCollectorResponse.ApiCollectorResponseBuilder<ApiCollectorResponse, ?>)
                ApiCollectorResponse
                        .builder()
                        .metricCollectors(metricCollectors.stream().map(MetricCollector::response).collect(Collectors.toList()))
                        .collecting(isCollecting())
                ;
    }
}
