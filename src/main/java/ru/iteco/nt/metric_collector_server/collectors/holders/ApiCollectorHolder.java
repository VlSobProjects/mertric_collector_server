package ru.iteco.nt.metric_collector_server.collectors.holders;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCollector;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCollectorResponse;
import ru.iteco.nt.metric_collector_server.collectors.exception.ApiCollectorException;
import ru.iteco.nt.metric_collector_server.influx.InfluxCollector;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@Slf4j
public class ApiCollectorHolder extends DataCollector<ApiCollectorResponse,ApiCollector,ApiCollectorResponse.ApiCollectorResponseBuilder<ApiCollectorResponse,?>> {
    private static final AtomicInteger isSource = new AtomicInteger();
    private final Flux<JsonNode> collector;
    private Disposable collecting;
    private final List<InfluxCollector<?>> influxCollectors = new CopyOnWriteArrayList<>();


    public ApiCollectorHolder(ApiCallHolder apiCallHolder, ApiCollector apiCollector) {
        super(apiCollector,isSource.incrementAndGet());
        Flux<JsonNode> flux = Flux.concat(apiCallHolder.getRequest()
                        ,apiCallHolder.getRequest().delayElement(Duration.ofMillis(apiCollector.getPeriodMillis())).repeat()
                ).flatMap(j->{
                    if(j.has("error")){
                        return Mono.error(new ApiCollectorException(j));
                    } else return Mono.just(j);
                });
        if(apiCallHolder.getRetry()!=null){
            flux = flux.retryWhen(apiCallHolder.getRetry());
        }
        collector = flux
                .doOnNext(this::setData)
                .doOnError(th->(th.getCause() instanceof ApiCollectorException),th-> {
                    ApiCollectorException e = (ApiCollectorException) th.getCause();
                    log.debug("error: {}",e.getError());
                    apiCallHolder.setData(e.getError());
                }).share();

    }



    public ApiCollectorResponse addAndStartInfluxCollector(InfluxCollector<?> influxCollector){
        influxCollectors.add(influxCollector);
        influxCollector.startCollecting(collector);
        return startCollecting();
    }

    public synchronized ApiCollectorResponse startCollecting(){
        if(!isCollecting()) {
            collecting = collector.subscribe();
        }
        return response();
    }

    public synchronized ApiCollectorResponse stopCollecting(){
        if(isCollecting()){
            collecting.dispose();
            collecting = null;
            influxCollectors.forEach(InfluxCollector::stop);
        }
        return response();
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
                        .influxCollectors(influxCollectors.stream().map(InfluxCollector::response).collect(Collectors.toList()))
                        .collecting(isCollecting())
                ;
    }
}
