package ru.iteco.nt.metric_collector_server;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.influx.model.responses.WriterResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.WriterConfig;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public abstract class MetricWriter<T,S extends WriterConfig,R extends WriterResponse<S>,B  extends WriterResponse.WriterResponseBuilder<S,R,?>> {

    private final static AtomicInteger ID_SOURCE = new AtomicInteger();
    @Getter
    private final int id;
    @Getter(AccessLevel.PROTECTED)
    private final S config;
    private final LinkedBlockingDeque<T> pointQueue = new LinkedBlockingDeque<>();
    private Disposable writing;
    private int lastDataSize;
    private int maxDataSize;
    private int maxQueueSize;

    protected MetricWriter(S config) {
        this.config = config;
        id= ID_SOURCE.incrementAndGet();
        writing = startWriter();
    }

    private Disposable startWriter(){
        return Mono.fromSupplier(this::getData)
                .delayElement(Duration.ofSeconds(config.getPeriodSeconds()))
                .filter(l->l.size()>0)
                .repeat()
                .subscribe(this::writeData);
    }

    public abstract B getBuilder();

    @SuppressWarnings("unchecked")
    protected B getSetBuilder(){
        return (B) getBuilder()
                .time(System.currentTimeMillis())
                .writing(isWriting())
                .lastDataSize(lastDataSize)
                .maxDataSize(maxDataSize)
                .maxQueueSize(maxQueueSize)
                .queueSize(pointQueue.size())
                .settings(config)
                .id(id);
    }

    public boolean isWriting(){
        return writing!=null && !writing.isDisposed();
    }

    public synchronized Mono<R> startWrite(){
        return Mono.fromRunnable(this::start).then(responseMono());
    }

    public synchronized Mono<R> stopWrite(){
        return Mono.fromRunnable(this::stop).then(responseMono());
    }

    public synchronized void start(){
        if(!isWriting())
            writing = startWriter();
    }

    public synchronized void stop(){
        if(isWriting())
            writing.dispose();
        writing = null;
        List<T> list = new ArrayList<>();
        pointQueue.drainTo(list);
        writeData(list);
    }

    public R response(){
        return getSetBuilder().success(check()).build();
    }

    public Mono<R> responseMono(){
        return Mono.fromSupplier(this::response);
    }

    protected abstract void writeData(Collection<T> data);

    protected abstract boolean check();

    private List<T> getData(){
        maxQueueSize = Integer.max(maxQueueSize,pointQueue.size());
        List<T> list = new ArrayList<>();
        pointQueue.drainTo(list,5000);
        lastDataSize = list.size();
        maxDataSize = Integer.max(maxDataSize,lastDataSize);
        log.debug("Data write: {} point times: {}", Utils.valueToTree(response()),list.stream().map(this::getMetricTime).collect(Collectors.toList()));
        return list;
    }

    protected abstract LocalDateTime getMetricTime(T point);

    public void addPoints(List<T> points){
        pointQueue.addAll(points);
        maxDataSize = Integer.max(maxQueueSize,pointQueue.size());
        log.debug("Data add: {}, list.size: {}, point times: {}", Utils.valueToTree(response()),points.size(),points.stream().map(this::getMetricTime).collect(Collectors.toList()));
    }

    public boolean isSameConfig(Object config){
        if(config==null && this.config==null) return true;
        else if(config==null || this.config==null) return false;
        return config.equals(this.config);
    }

}
