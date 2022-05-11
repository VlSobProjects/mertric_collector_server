package ru.iteco.nt.metric_collector_server.influx;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxDbConnectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxDBConnectorConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;


public class InfluxDbConnector {
    private final static AtomicInteger ID_SOURCE = new AtomicInteger();


    private final InfluxDBConnectorConfig config;

    @Getter
    private final int id;
    private final LinkedBlockingDeque<Point> pointQueue = new LinkedBlockingDeque<>();
    private Disposable writeInflux;
    private int lastDataSize;
    private int maxDataSize;
    private int maxQueueSize;

    public InfluxDbConnector(InfluxDBConnectorConfig config){
        this.config = config;
        id = ID_SOURCE.incrementAndGet();
        writeInflux = startWrite();
    }

    private Disposable startWrite(){
        return Mono.fromSupplier(this::getPoints)
                .delayElement(Duration.ofSeconds(config.getPeriodSeconds()))
                .filter(l->l.size()>0)
                .subscribe(list-> {
                    if(list.size()>=config.getMinBatchSize()){
                        InfluxDB conn = getConnection(true);
                        conn.enableBatch();
                        getConnection(true).write(convert(list));
                    } else {
                        InfluxDB conn = getConnection(false);
                        conn.disableBatch();
                        list.forEach(conn::write);
                    }
                });
    }

    public Mono<InfluxDbConnectorResponse> startWriteToInflux(){
        if(writeInflux==null || writeInflux.isDisposed())
            writeInflux = startWrite();
        return responseMono();
    }

    private boolean check(){
        Pong p = getConnection(false).ping();
        return p!=null && p.isGood();
    }

    private synchronized InfluxDB getConnection(boolean batch){
        InfluxDB influxDB = InfluxDBFactory.connect(config.getUrl(), config.getUser(), config.getPass());
        if(!batch)
            return influxDB.setRetentionPolicy(config.getRetentionPolicy()).setDatabase(config.getDataBase());
        return influxDB;

    }

    public InfluxDbConnectorResponse response(){
        return InfluxDbConnectorResponse.builder()
                .id(id)
                .success(check())
                .lastDataSize(lastDataSize)
                .maxDataSize(maxDataSize)
                .maxQueueSize(maxQueueSize)
                .settings(config)
                .queueSize(pointQueue.size())
                .writing(writeInflux!=null && !writeInflux.isDisposed())
                .build();
    }

    public Mono<InfluxDbConnectorResponse> responseMono(){
        return Mono.fromSupplier(this::response);
    }

    public void addPoints(List<Point> points){
        pointQueue.addAll(points);
        maxDataSize = Integer.max(maxQueueSize,pointQueue.size());
    }

    private List<Point> getPoints(){
        maxQueueSize = Integer.max(maxQueueSize,pointQueue.size());
        List<Point> list = new ArrayList<>();
        pointQueue.drainTo(list,5000);
        lastDataSize = list.size();
        maxDataSize = Integer.max(maxDataSize,lastDataSize);
        return list;
    }


    private  BatchPoints convert(List<Point> points){
        return BatchPoints.database(config.getDataBase()).retentionPolicy(config.getRetentionPolicy()).points(points).build();
    }

    public boolean isSameConfig(InfluxDBConnectorConfig config){
        return config.equals(this.config);
    }



}
