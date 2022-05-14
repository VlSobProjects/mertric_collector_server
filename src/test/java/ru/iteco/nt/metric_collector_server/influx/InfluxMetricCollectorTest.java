package ru.iteco.nt.metric_collector_server.influx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import org.influxdb.dto.Point;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.utils.Utils;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxField;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorConfig;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class InfluxMetricCollectorTest {

    @Test
    void addPointFromData() throws JsonProcessingException {
        List<InfluxMetricCollector> collectors = Arrays.asList(
                getStubCollector("heapSize","/heapSize"),
                getStubCollector("heapMaxSize","/heapMaxSize"),
                getStubCollector("heapFreeSize","/heapFreeSize"),
                getStubCollector("threadsCount","/threadsCount"),
                getStubCollector("peakThreadsCount","/peakThreadsCount"),
                getStubCollector("systemCpuLoad","/systemCpuLoad")
        );

        List<Point> points = new ArrayList<>();
        Instant time = Instant.now();
        JsonNode data = Utils.stringToTree("{\n" +
                "  \"heapSize\": 349175808,\n" +
                "  \"heapMaxSize\": 5726797824,\n" +
                "  \"heapFreeSize\": 287103288,\n" +
                "  \"threadsCount\": 11,\n" +
                "  \"peakThreadsCount\": 11,\n" +
                "  \"systemCpuLoad\": 13.911578866085305\n" +
                "}");
        collectors.forEach(c->c.addPointFromData(data,points,time));

        assertFalse(points.isEmpty());

        points.forEach(System.out::println);
    }

    private InfluxMetricCollector getStubCollector(String name,String valuePath){
        return new InfluxMetricCollector(
                InfluxMetricCollectorConfig.builder()
                        .measurement("test")
                        .setTime(true)
                        .apiCollectorId(0)
                        .fields(Arrays.asList(
                                InfluxField.builder().tag(true).name("metric").value(Utils.valueToTree(name)).build()
                                ,InfluxField.builder().path(valuePath).name("value").build()
                                ))
                        .build()
        );
    }

    @Test
    void addPointFromData2(){
        JsonNode source = getTestMetricNode();
        InfluxMetricCollectorConfig collectorConfig =  getTestConfig();
        assertNotNull(source);
        assertNotNull(collectorConfig);
        InfluxMetricCollector collector = new InfluxMetricCollector(collectorConfig);
        List<Point> points = collector.getPointFromData(source);
        points.stream()
                .collect(Collectors.
                        groupingBy(p->p.lineProtocol().split(" ")[0]))
                .forEach((s,l)->System.out.printf("[%s] - %s\n",s,l.size()));



    }


    @SneakyThrows
    private JsonNode getTestMetricNode(){
        return Utils.stringToTree(new String(Files.readAllBytes(Paths.get("C:\\work\\mertric_collector_server\\src\\test\\resources\\test.json"))));
    }
    @SneakyThrows
    private InfluxMetricCollectorConfig getTestConfig(){
        JsonNode node = Utils.stringToTree(new String(Files.readAllBytes(Paths.get("C:\\work\\mertric_collector_server\\src\\test\\resources\\InfluxCollectorConfig.json"))));
        return Utils.getFromJsonNode(node,InfluxMetricCollectorConfig.class);
    }


    @Test
    void validate() throws InterruptedException {
        InfluxMetricCollectorConfig collectorConfig =  getTestConfig();
        assertNotNull(collectorConfig);
        InfluxMetricCollector collector = new InfluxMetricCollector(collectorConfig);
        Disposable d = collector.validate().subscribe(l->l.forEach(n->System.out.println(n.toPrettyString())));
        while (!d.isDisposed()){
            TimeUnit.MILLISECONDS.wait(500);
        }

    }

    @Test
    void validate2() throws InterruptedException {
        List<InfluxMetricCollector> collectors = Arrays.asList(
                getStubCollector("heapSize","/heapSize"),
                getStubCollector("heapMaxSize","/heapMaxSize"),
                getStubCollector("heapFreeSize","/heapFreeSize"),
                getStubCollector("threadsCount","/threadsCount"),
                getStubCollector("peakThreadsCount","/peakThreadsCount"),
                getStubCollector("systemCpuLoad","/systemCpuLoad")
        );
        Disposable d = collectors.stream().map(InfluxMetricCollector::validate).reduce(Flux.empty(), Flux::concat, Flux::concat).subscribe(System.out::println);
        while (!d.isDisposed()){
            TimeUnit.MILLISECONDS.wait(500);
        }

    }

    @Test
    void validateData() throws InterruptedException {
        JsonNode source = getTestMetricNode();
        assertNotNull(source);
        InfluxMetricCollectorConfig collectorConfig =  getTestConfig();
        assertNotNull(collectorConfig);
        InfluxMetricCollector collector = new InfluxMetricCollector(collectorConfig);
        Disposable d = collector.validateData(source).subscribe(l->l.forEach(n->System.out.println(n.toPrettyString())));
        while (!d.isDisposed()){
            TimeUnit.MILLISECONDS.wait(500);
        }
    }

    @Test
    void validateData2() throws InterruptedException, JsonProcessingException {
        JsonNode source = Utils.stringToTree("{\n" +
                "  \"heapSize\": 349175808,\n" +
                "  \"heapMaxSize\": 5726797824,\n" +
                "  \"heapFreeSize\": 287103288,\n" +
                "  \"threadsCount\": 11,\n" +
                "  \"peakThreadsCount\": 11,\n" +
                "  \"systemCpuLoad\": 13.911578866085305\n" +
                "}");
        assertNotNull(source);
        List<InfluxMetricCollector> collectors = Arrays.asList(
                getStubCollector("heapSize","/heapSize"),
                getStubCollector("heapMaxSize","/heapMaxSize"),
                getStubCollector("heapFreeSize","/heapFreeSize"),
                getStubCollector("threadsCount","/threadsCount"),
                getStubCollector("peakThreadsCount","/peakThreadsCount"),
                getStubCollector("systemCpuLoad","/systemCpuLoad")
        );
        Disposable d = collectors.stream().map(c->c.validateData(source)).reduce(Flux.empty(), Flux::concat, Flux::concat).subscribe(System.out::println);
        while (!d.isDisposed()){
            TimeUnit.MILLISECONDS.wait(500);
        }
    }
}

/*
{
  "heapSize": 349175808,
  "heapMaxSize": 5726797824,
  "heapFreeSize": 287103288,
  "threadsCount": 11,
  "peakThreadsCount": 11,
  "systemCpuLoad": 13.911578866085305
}
 */