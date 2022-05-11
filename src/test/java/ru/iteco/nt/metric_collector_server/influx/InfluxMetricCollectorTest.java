package ru.iteco.nt.metric_collector_server.influx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializable;
import org.influxdb.dto.Point;
import org.junit.jupiter.api.Test;
import ru.iteco.nt.metric_collector_server.collectors.web_client.Utils;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxField;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorConfig;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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