package ru.iteco.nt.metric_collector_server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.Point;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.ResponseWithMessage;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
public class Utils {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Getter
    private final static Function<WebClientResponseException,String> WEB_CLIENT_RESPONSE_EXCEPTION_CONVERTER = ex->String.format("Request: %s, Status: %s, Response:%s",ex.getRequest(),ex.getStatusCode(),ex.getResponseBodyAsString());

    private final static Consumer<WebClientResponseException> LOG_REQ_EX = ex -> log.error("{}",WEB_CLIENT_RESPONSE_EXCEPTION_CONVERTER.apply(ex),ex);

    @Setter
    private static Consumer<WebClientResponseException> GLOBAL_REQ_ERR;

    public ExchangeFilterFunction logRequest(){
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if(log.isDebugEnabled()){
                StringBuilder builder = new StringBuilder("Req: ").append(clientRequest.method()).append(" ").append(clientRequest.url()).append("\n");
                builder.append("Headers:\n");
                clientRequest.headers().forEach((n,v)->builder.append(String.format("%s = %s",n,v)));
                builder.append("\n");
                builder.append("Cookies:\n");
                builder.append(clientRequest.cookies()).append("\n");
                builder.append("Attributes:\n");
                builder.append(clientRequest.attributes()).append("\n");
                log.debug(builder.toString());
            }
            return Mono.just(clientRequest);
        });
    }

    public String convertException(String req,Throwable ex){
        if(ex instanceof WebClientResponseException){
            return "("+req+") - "+WEB_CLIENT_RESPONSE_EXCEPTION_CONVERTER.apply((WebClientResponseException)ex);
        }
        return getExceptionWithCause(ex,new StringBuilder(req+" Exception: "));
    }

    private static String getExceptionWithCause(Throwable ex,StringBuilder builder){
        builder.append(ex.toString());
        if(ex.getCause()!=null){
            builder.append(" Cause: ");
            return getExceptionWithCause(ex.getCause(),builder);
        }
        return builder.toString();
    }

    private static ArrayNode convertList(List<?> list){
        return OBJECT_MAPPER.createArrayNode().addAll(list.stream().filter(Objects::nonNull).map(Utils::valueToTree).collect(Collectors.toList()));
    }

    public String getInfo(ClientResponse clientResponse){
        StringBuilder builder = new StringBuilder("Resp Code ").append(clientResponse.rawStatusCode()).append("\n");
        builder.append("Headers:\n");
        clientResponse.headers().asHttpHeaders().forEach((n,v)->builder.append(String.format("%s = %s",n,v)).append("\n"));
        builder.append("\n");
        builder.append("Cookies:\n");
        builder.append(clientResponse.cookies()).append("\n");
        return builder.toString();
    }

    public ExchangeFilterFunction logResponse(){
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if(clientResponse.statusCode().is5xxServerError()){
                log.error("Server error!: {}",clientResponse.statusCode());
            }
            if(log.isDebugEnabled()) {
                log.debug(getInfo(clientResponse));
            }
            return Mono.just(clientResponse);
        });
    }

    public <T> Mono<T> exchangeToMono(WebClient.RequestHeadersSpec<?> requestHeadersSpec,Class<T> tClass,UnaryOperator<JsonNode> extractor,Consumer<ClientResponse> responseConsumer){
        return exchangeToMono(requestHeadersSpec,responseConsumer).map(j->getFromJsonNode(j,tClass,extractor));
    }
    public <T> Mono<T> exchangeMono(WebClient.RequestHeadersSpec<?> requestHeadersSpec,Class<T> tClass,UnaryOperator<JsonNode> extractor){
        return exchangeToMono(requestHeadersSpec,null).map(j->getFromJsonNode(j,tClass,extractor));
    }
    public <T> Mono<List<T>> exchangeMonoList(WebClient.RequestHeadersSpec<?> requestHeadersSpec,Class<T> tClass,UnaryOperator<JsonNode> extractor){
        return exchangeToMono(requestHeadersSpec,null).map(j->getListFromJsonNode(j,tClass,extractor));
    }
    public <T> Mono<T> exchangeMono(WebClient.RequestHeadersSpec<?> requestHeadersSpec,Class<T> tClass){
        return exchangeToMono(requestHeadersSpec,null).map(j->getFromJsonNode(j,tClass,null));
    }
    public <T> Mono<T> exchangeMono(WebClient.RequestHeadersSpec<?> requestHeadersSpec, TypeReference<T> typeReference){
        return exchangeToMono(requestHeadersSpec,null).map(j->getFromJsonNode(j,typeReference,null));
    }
    public <T> Mono<T> exchangeMono(WebClient.RequestHeadersSpec<?> requestHeadersSpec, TypeReference<T> typeReference,UnaryOperator<JsonNode> extractor){
        return exchangeToMono(requestHeadersSpec,null).map(j->getFromJsonNode(j,typeReference,extractor));
    }

    public Mono<JsonNode> exchangeMono(WebClient.RequestHeadersSpec<?> requestHeadersSpec){
        return exchangeToMono(requestHeadersSpec,null);
    }

    public Mono<JsonNode> exchangeToMono(WebClient.RequestHeadersSpec<?> requestHeadersSpec,Consumer<ClientResponse> responseConsumer){
        return requestHeadersSpec.exchangeToMono(clientResponse -> {
            if(clientResponse.statusCode().isError()){
                Mono<WebClientResponseException> error = clientResponse.createException().doOnNext(LOG_REQ_EX);
                if(GLOBAL_REQ_ERR!=null)error = error.doOnNext(GLOBAL_REQ_ERR);
                return error.flatMap(Mono::error);
            } else if(responseConsumer!=null){
                responseConsumer.accept(clientResponse);
            }
            return clientResponse.bodyToMono(JsonNode.class);
        });
    }

    public <T> List<T> getListFromJson(JsonNode arrayNode, Class<T> tClass){
        List<T> list = new ArrayList<>();
        arrayNode.forEach(nod-> {
            try {
                list.add(OBJECT_MAPPER.treeToValue(nod,tClass));
            } catch (JsonProcessingException e) {
                log.error("Fail to convert array {} to List of {}",arrayNode,tClass);
            }
        });
        return list;
    }

    public <T> JsonNode valueToTree(T value){
        try {
            return OBJECT_MAPPER.readTree(valueToString(value));
        } catch (JsonProcessingException e) {
            log.error("Fail to read value: {}",value,e);
            return getError("valueToTree","Fail to covert value",value);
        }
    }

    public <T> String valueToString(T t){
        if(t==null)return null;
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(t);
        } catch (JsonProcessingException e) {
            log.error("Fail convert obj: "+t);
            return null;
        }
    }

    public JsonNode getError(String source,String message,Object ... objects){
        return getError(ErrorJson.builder()
                .data(convertList(Arrays.asList(objects)))
                .errorSource(source)
                .errorMessage(message)
                .build());
    }

    public <T> T getFromJsonNode(JsonNode node, TypeReference<T> typeReference, UnaryOperator<JsonNode> extractor){
        try {
            return extractor == null ? OBJECT_MAPPER.convertValue(node,typeReference) : OBJECT_MAPPER.convertValue(extractor.apply(node),typeReference);
        } catch (Exception e) {
            log.error("Fail convert to class:{} jNode:{} ",typeReference,node);
            throw new RuntimeException(String.format("Fail to covert json %s to class:%s, Exception: %s",node,typeReference,convertException("",e)));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getFromJsonNode(JsonNode node, Class<T> tClass, UnaryOperator<JsonNode> extractor){
        try {
            if(JsonNode.class.isAssignableFrom(tClass)){
                return (T)(extractor==null? node : extractor.apply(node));
            }
            if(tClass == String.class){
                return (T)(extractor==null? node.textValue() : extractor.apply(node).textValue());
            }
            return OBJECT_MAPPER.treeToValue(extractor==null? node : extractor.apply(node),tClass);
        } catch (Exception e) {
            log.error("Fail convert to class:{} jNode:{} ",tClass,node);
            throw new RuntimeException(String.format("Fail to covert json %s to class:%s, Exception: %s",node,tClass,convertException("",e)));
        }
    }
    public <T> T getFromJsonNode(JsonNode node, Class<T> tClass){
        return getFromJsonNode(node, tClass,null);
    }

    public <T> List<T> getListFromJsonNode(JsonNode node, Class<T> tClass, UnaryOperator<JsonNode> extractor){
        try {
            return getListFromJson(extractor==null?node:extractor.apply(node),tClass);
        } catch (Exception e) {
            log.error("Fail convert to List of object class:{} jNode:{} ",tClass,node);
            throw new RuntimeException(String.format("Fail to covert json %s to List of class:%s",node,tClass));
        }
    }

    public <T> List<T> getListFromJsonNode(JsonNode node, Class<T> tClass){
        return getListFromJsonNode(node, tClass,null);
    }

    public <T> T getFromJsonNode(Supplier<JsonNode> supplier, Class<T> tClass){
        JsonNode node = supplier.get();
        try {
            return OBJECT_MAPPER.treeToValue(node,tClass);
        } catch (JsonProcessingException e) {
            log.error("Fail to convert to class:{} jNode:{} ",tClass,node);
            return null;
        }
    }

    public ObjectNode getObjectNode(String key,JsonNode value){
        return OBJECT_MAPPER.createObjectNode().set(key, value);
    }

    public ObjectNode getObjectNode(String key,String value){
        return OBJECT_MAPPER.createObjectNode().put(key, value);
    }

    public JsonNode getError(ErrorJson errorJson){
        return getObjectNode("error",valueToTree(errorJson));
    }

    public Mono<JsonNode> getWithOnHttpErrorResponseSpec(String source, WebClient.ResponseSpec responseSpec){
        return responseSpec
                .onStatus(HttpStatus::isError, ClientResponse::createException).bodyToMono(JsonNode.class)
                .onErrorResume(th->Mono.just(getError(source,th.toString())));
    }

    public JsonNode getFromJsonNode(JsonNode jsonNode,String expression){
        if(jsonNode instanceof ArrayNode){
            ArrayNode result = OBJECT_MAPPER.createArrayNode();
            jsonNode.forEach(j->result.add(j.at(expression)));
            return result;
        }
        return jsonNode.at(expression);
    }

    public boolean validatePath(JsonNode jsonNode,String expression){
        JsonNode node = getFromJsonNode(jsonNode, expression);
        if(node == null || node.isNull() || node.isMissingNode() || (node.isValueNode() && node.asText().trim().isEmpty())) return false;
        if(node.isContainerNode() && node.isEmpty()) return false;
        return !node.isContainerNode() || IntStream.range(0, node.size()).map(i -> node.get(i).isNull() ? 0 : 1).sum() != 0;
    }

    public ArrayNode toArrayNode(Collection<?> objects){
        ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
        objects.forEach(o->arrayNode.add(valueToTree(o)));
        return arrayNode;
    }

    public JsonNode stringToTree(String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(json);
    }

    public List<Function<Point.Builder,Point.Builder>> reduceSetters(List<Function<Point.Builder,Point.Builder>> l1, List<Function<Point.Builder,Point.Builder>> l2,JsonNode source){
        List<Function<Point.Builder,Point.Builder>> start = l1.size()==0?l2:l1;
        if(l1.size()==0 && l2.size()==0) return new ArrayList<>();
        if(l1.size()==0 || l2.size()==0) return start;
        if(l1.size()==l2.size()){
            AtomicInteger index= new AtomicInteger();
            l1.replaceAll(b->b.andThen(l2.get(index.getAndIncrement())));
            return l1;
        }
        if(l1.size()==1 || l2.size()==1){
            return l1.stream().flatMap(s->l2.stream().map(s::andThen)).collect(Collectors.toList());
        } else {
            log.warn("reduceSetters but l1 (size: {}) and l2 (size: {}) not meet reduce size criteria: on of the list size is less than another and not equal 1. data: {}",l1.size(),l2.size(),source);
            AtomicInteger index= new AtomicInteger();
            List<Function<Point.Builder,Point.Builder>> s = l1.size()>l2.size()?l2:l1;
            List<Function<Point.Builder,Point.Builder>> l = l1==s?l2:l1;
            s.replaceAll(b->b.andThen(l.get(index.getAndIncrement())));
            return s;
        }
    }

    private String getReduceSkipPoints(List<Function<Point.Builder,Point.Builder>> l1, List<Function<Point.Builder,Point.Builder>> l2){
        List<Function<Point.Builder,Point.Builder>> s = l1.size()>l2.size()?l2:l1;
        List<Function<Point.Builder,Point.Builder>> l = l1==s?l2:l1;
        return IntStream.range(0,l.size()).filter(i->i>=s.size()).mapToObj(i-> {
            Point.Builder builder = l.get(i).apply(Point.measurement("error"));
            builder.addField("error","errorPoint");
            return builder.build();
        }).map(Point::lineProtocol).collect(Collectors.joining(", ","[","]"));
    }

    public  <R extends DataResponse<?> & ResponseWithMessage<R>> Mono<R> setMessageAndData(Mono<R> response, String message, Object...objects){
        return setData(response.map(r->r.addMessage(message)),objects);
    }

    public  <R extends DataResponse<?>> Mono<R> setData(Mono<R> response, Object...objects){
        return response.map(r->{
            r.dataArray(objects);
            return r;
        });
    }

    public <R extends DataResponse<?>> Mono<R> modifyData(Mono<R> response,Consumer<JsonNode> transformer){
        return response.map(r->{
            r.modifyData(transformer);
            return r;
        });
    }

    public <R extends DataResponse<?>> Mono<R> modifyAndSetData(Mono<R> response,UnaryOperator<JsonNode> transformer){
        return response.map(r->{
            r.modifyAndSetData(transformer);
            return r;
        });
    }

    public void collectData(Collection<JsonNode> list,Predicate<JsonNode> filter ,Object...objects){
        if(objects.length==1){
            if(objects[0] instanceof Collection){
                ((Collection<?>)objects[0]).forEach(o->collectData(list,filter,o));
            } else if(objects[0] instanceof ArrayNode){
                ((ArrayNode)objects[0]).forEach(j->collectData(list,filter,j));
            } else {
                JsonNode n = objects[0] instanceof JsonNode ? (JsonNode)objects[0] :  Utils.valueToTree(objects[0]);
                if(filter.test(n)){
                    list.add(n);
                }
            }
        } else Stream.of(objects).forEach(o->collectData(list,filter,o));
    }

    public void collectData(Collection<JsonNode> list,Object...objects){
        collectData(list,j->true,objects);
    }

    public List<JsonNode> collectDataToList(Predicate<JsonNode> filter, Object...objects){
        List<JsonNode> list = new ArrayList<>();
        collectData(list,filter,objects);
        return list;
    }

}
