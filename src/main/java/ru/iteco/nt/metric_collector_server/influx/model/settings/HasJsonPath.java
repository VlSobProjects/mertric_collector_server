package ru.iteco.nt.metric_collector_server.influx.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import ru.iteco.nt.metric_collector_server.utils.Utils;

public interface HasJsonPath {

    String getPath();

    @JsonIgnore
    default boolean hasPath(){
        return getPath()!=null && !getPath().trim().isEmpty() && getPath().startsWith("/");
    }

    default JsonNode getByPath(JsonNode data){
        if(data==null) return null;
        if(!hasPath()) return data;
        return Utils.getFromJsonNode(data,getPath());
    }
}
