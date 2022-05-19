package ru.iteco.nt.metric_collector_server.influx.model.responses;

public interface ResponseWithMessage<T> {

     T setMessage(String message);
     String getMessage();
     default T addMessage(String message){
          String m = getMessage();
          if(m==null || m.trim().isEmpty())
               return setMessage(message);
          else return setMessage(String.format("%s. %s",m,message));
     }
}
