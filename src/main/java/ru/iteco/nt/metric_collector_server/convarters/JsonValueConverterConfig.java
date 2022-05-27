package ru.iteco.nt.metric_collector_server.convarters;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class JsonValueConverterConfig {
    private JsonValueCompareOperation compareOperation;
    private Map<String,String> replaceMap;
}
