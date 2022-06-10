package ru.iteco.nt.metric_collector_server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.influxdb.dto.Point;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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

    @Test
    void fiendValues() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        Resource resource = new ClassPathResource("dynatrace_metric.json");
        JsonNode metric = objectMapper.readTree(resource.getInputStream());

        metric.findValues("timestamps").forEach(j->System.out.println(j.toPrettyString()));
    }

    @Test
    void testFiendByKey() throws JsonProcessingException {
        String json = "{\n" +
                "\t\"data\":[\n" +
                "\t\t{\n" +
                "\t\t\t\"timestamps\":[1,2,3,4,5,6,7],\n" +
                "\t\t\t\"values\":[0.1,0.2,0.3,null,null]\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"timestamps\":[1,2,3,4,5],\n" +
                "\t\t\t\"values\":[0.2,null,0.3,0.7,null,0.5,0.3]\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";
        JsonNode j = Utils.stringToTree(json);
        List<JsonNode> values = j.findValues("values");
        List<JsonNode> timestamps = j.findValues("timestamps");
        System.out.println(values);
        System.out.println(timestamps);
        for (int i = 0; i < values.size(); i++) {
            JsonNode v = values.get(i);
            JsonNode t = timestamps.get(i);
            System.out.println(v);
            System.out.println(t);
            if(v instanceof ArrayNode && t instanceof ArrayNode){
                clearNulls((ArrayNode)v,(ArrayNode)t);
            }
        }
        System.out.println(j.toPrettyString());
    }

    private void clearNulls(ArrayNode values,ArrayNode time){
        setArrEqualsSize(values,time);
        IntStream.range(0,values.size())
                .filter(i->values.get(i) instanceof NullNode)
                .findFirst().ifPresent(i->{
                    values.remove(i);
                    time.remove(i);
                });
        if(Utils.getStreamFromJson(values).anyMatch(j->j instanceof NullNode))
            clearNulls(values,time);
    }

    private void setArrEqualsSize(ArrayNode arr1,ArrayNode arr2){
        if(arr1.size()==arr2.size()) return;
        ArrayNode b  = arr1.size()>arr2.size() ?arr1:arr2;
        while (arr1.size()!=arr2.size()){
            b.remove(b.size()-1);
        }
    }
}