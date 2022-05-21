package ru.iteco.nt.metric_collector_server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import ru.iteco.nt.metric_collector_server.influx.model.responses.ResponseWithMessage;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@Getter
@SuperBuilder
public abstract class DataResponse<S> {

    private final int id;
    private final S settings;
    private JsonNode data;
    private final long time;
    private final boolean fail;

    public static <S,T extends DataResponse<S>> T factoryError(String source, String message, DataResponseBuilder<S,T,?> builder, Object ...objects){
        return builder.fail(true).time(System.currentTimeMillis()).data(Utils.getError(source, message, objects)).build();
    }

    public void dataArray(Object...objects){
        List<JsonNode> list = new ArrayList<>();
        Utils.collectData(list,objects);
        if(list.size()==1) data = list.get(0);
        else data = Utils.valueToTree(list);
    }

    public void dataList(List<Object> objects){
        dataArray(objects.toArray());
    }

    public void modifyAndSetData(UnaryOperator<JsonNode> transformAndSet){
        data = transformAndSet.apply(data);
    }

    public void modifyData(Consumer<JsonNode> transform){
        transform.accept(data);
    }




}
