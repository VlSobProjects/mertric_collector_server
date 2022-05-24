package ru.iteco.nt.metric_collector_server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.influxdb.dto.Point;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    private static final String json = "[[{\"error\":{\"time\":1653145624789,\"errorSource\":\"InfluxFieldsCollector.influxFiledDataError\",\"errorMessage\":\"Utils.validatePath fail JsonNode by field path null or empty or missing or contains only nulls\",\"data\":[{\"path\":\"/peakThreadsCount\",\"name\":\"value\",\"tag\":false,\"time\":false,\"value\":{},\"children\":0,\"hasValue\":false},{\"heapSize\":-1194567692,\"heapMaxSize\":-2031172397,\"heapFreeSize\":-1620846518,\"threadsCount\":11,\"systemCpuLoad\":0.6497151919783221}],\"serverError\":false}}]]";


    @Test
    void collectDataToList() throws JsonProcessingException {
        List<JsonNode> list = Utils.collectDataToList(j->j.has("error"), Arrays.asList(Utils.stringToTree(json),Utils.stringToTree(json)));
        list.forEach(System.out::println);

    }

    @Test
    void testInfluxPointTime() {
        Point point = Point.measurement("test")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .tag("metric","m1")
                .addField("count",10)
                .addField("sum",100)
                .tag("system","testSystem").build();
        System.out.println(Utils.getInfluxPointTime(point));
    }
}