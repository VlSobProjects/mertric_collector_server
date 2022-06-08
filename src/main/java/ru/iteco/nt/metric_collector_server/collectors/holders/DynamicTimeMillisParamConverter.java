package ru.iteco.nt.metric_collector_server.collectors.holders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.iteco.nt.metric_collector_server.aspect.annotations.LogMethodReturn;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.DynamicTimeMillisParamConfig;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DynamicTimeMillisParamConverter {

    @LogMethodReturn(isInfo = true)
    public String uriWithParams(List<DynamicTimeMillisParamConfig> configs, String uri, JsonNode data, Consumer<JsonNode> onError){
        if(configs==null || configs.isEmpty()) return uri;
        String prefix = uri+(uri.contains("?") ? "&" : "?");
        return configs.stream().map(c->getParam(c,data,onError)).collect(Collectors.joining("&",prefix,""));

    }


    private String getParam(DynamicTimeMillisParamConfig config,JsonNode data,Consumer<JsonNode> onError){
        JsonNode source = config.getByPath(data);
        long time = config.hasKey() ?
                source==null ? System.currentTimeMillis()+config.getAddToCurIfNull() :
                        getFromData(source,config.getKey(),onError) +config.getAddMillis() :
                System.currentTimeMillis()+config.getAddMillis();
        return String.format("%s=%s",config.getName(),time);
    }

    private long getFromData(JsonNode data,String key, Consumer<JsonNode> onError){
        return data.findValues(key).stream().mapToLong(j->getMaxMillis(j,onError)).min().orElseGet(()->{
            onError.accept(Utils.getError(getClass().getSimpleName(),"fail to found longs by key: "+key,data));
            return 0;
        });
    }

    private long getMaxMillis(JsonNode timeData, Consumer<JsonNode> onError){
        if(timeData==null || timeData.isNull() || timeData.isMissingNode() || (timeData instanceof ArrayNode && timeData.isEmpty())){
            onError.accept(Utils.getError(getClass().getSimpleName(),"time data is missing",timeData));
            return 0;
        } else if(timeData instanceof ArrayNode){
            return Utils.getStreamFromJson(timeData).filter(JsonNode::isLong).mapToLong(JsonNode::asLong).max().orElse(0);
        } else if(timeData.isLong()){
            return timeData.asLong();
        } else {
            onError.accept(Utils.getError(getClass().getSimpleName(),"time data not array and not long value",timeData));
            return 0;
        }
    }

}
