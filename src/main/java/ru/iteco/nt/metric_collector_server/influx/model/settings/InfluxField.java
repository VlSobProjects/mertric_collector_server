package ru.iteco.nt.metric_collector_server.influx.model.settings;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.influxdb.dto.Point;
import ru.iteco.nt.metric_collector_server.utils.FieldValueConvertor;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class InfluxField {
    private String path;
    private String name;
    private boolean tag;
    private boolean time;
    private JsonNode value;
    private List<InfluxField> children;

}
