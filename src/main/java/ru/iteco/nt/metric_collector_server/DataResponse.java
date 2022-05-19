package ru.iteco.nt.metric_collector_server;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.util.List;

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
       if(objects!=null && objects.length!=0){
           if(objects.length==1) data = Utils.valueToTree(objects[0]);
           else data = Utils.valueToTree(objects);
       }
    }

    public void dataList(List<Object> objects){
        dataArray(objects.toArray());
    }


}
