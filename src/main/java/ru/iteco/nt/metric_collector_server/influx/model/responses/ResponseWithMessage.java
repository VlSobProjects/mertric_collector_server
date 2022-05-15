package ru.iteco.nt.metric_collector_server.influx.model.responses;

public interface ResponseWithMessage<T> {

     T setMessage(String message);
}
