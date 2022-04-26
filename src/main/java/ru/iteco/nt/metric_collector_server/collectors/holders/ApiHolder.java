package ru.iteco.nt.metric_collector_server.collectors.holders;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiDataResponse;
import ru.iteco.nt.metric_collector_server.collectors.web_client.Utils;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@Getter
public abstract class ApiHolder<R extends ApiDataResponse<S>,S,B extends ApiDataResponse.ApiDataResponseBuilder<S,R,?>> {

    private final S settings;
    private final int id;

    protected ApiHolder(S settings, int id) {
        this.settings = settings;
        this.id = id;
    }

    public R response(){
        return getSetBuilder().build();
    }

    public Mono<R> responseError(String source, String message, Object ...objects){
        return Mono.just(getBuilder().fail(true).time(System.currentTimeMillis()).data(Utils.getError(source, message, objects)).build());
    }

    @SuppressWarnings("unchecked")
    protected B getSetBuilder(){
        return (B) getBuilder().settings(settings).id(id);
    }

    public abstract B getBuilder();

    public Mono<R> monoResponse(){
        return Mono.fromSupplier(this::response);
    }


}
