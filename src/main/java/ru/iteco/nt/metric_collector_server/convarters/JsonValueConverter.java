package ru.iteco.nt.metric_collector_server.convarters;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;


import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class JsonValueConverter {

    private final JsonValueConverterConfig converterConfig;

    public String replace(JsonNode value){
        return getFromMap(value).map(Map.Entry::getValue).orElse(null);
    }

    public boolean validate(JsonNode value){
        return getFromMap(value).isPresent();
    }

    private Optional<Map.Entry<String,String>> getFromMap(JsonNode value){
        return converterConfig.getReplaceMap().entrySet().stream().filter(e->converterConfig.getCompareOperation().getCompare().test(e.getKey(),value)).findFirst();
    }

}
