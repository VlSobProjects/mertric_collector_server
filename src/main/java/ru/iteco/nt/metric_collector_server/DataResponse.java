package ru.iteco.nt.metric_collector_server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

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
        if(objects==null || objects.length==0) return;
        List<JsonNode> list = new ArrayList<>();
        Utils.collectData(list,objects);
        if(list.size()==1) data = list.get(0);
        else data = Utils.valueToTree(list);
    }

    public void addDataFromResponse(DataResponse<?> response){
        if(response.data!=null){
            if(data==null) data = response.data;
            else {
                ArrayNode arr = data instanceof ArrayNode ? (ArrayNode) data : response.data instanceof  ArrayNode ? (ArrayNode) response.data : Utils.toArrayNode(Collections.emptyList());
                if(data instanceof ArrayNode && response.data instanceof ArrayNode){
                    response.data.forEach(arr::add);
                } else {
                    if(data.equals(arr)) arr.add(response.data);
                    else if(response.data.equals(arr)) data = arr.add(data);
                    else data = arr.add(data).add(response.data);
                }
            }
        }
    }

    public void modifyAndSetData(UnaryOperator<JsonNode> transformAndSet){
        data = transformAndSet.apply(data);
    }

    public void modifyData(Consumer<JsonNode> transform){
        transform.accept(data);
    }

}
