package ru.iteco.nt.metric_collector_server.influx;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.influxdb.impl.InfluxDBMapper;

import java.util.List;

@Slf4j
@Builder
public class InfluxClient {

    private final String url;
    private final String pass;
    private final String user;
    @Builder.Default
    private final int butchSize = 100;


    private synchronized InfluxDB getConnection(){
        return InfluxDBFactory.connect(url,user,pass);
    }

    public boolean checkConnection(){
        try(InfluxDB influxDB = getConnection()) {
            Pong response = influxDB.ping();
            if(response.getVersion().equalsIgnoreCase("unknowing")){
                log.error("Error ping response:{}",response);
                return false;
            } else {
                log.info("ping response is ok response:{} ",response);
                return true;
            }
        }
    }

//    public void write(){
//        Point point = Point.measurement("").
//        getConnection().setDatabase("").write();
//    }

    public String getVersion(){
        try(InfluxDB influxDB = getConnection()) {
            return influxDB.version();
        }catch (Exception e){
            log.error("Error in getVersion",e);
            return e.toString();
        }
    }

    private void writeBatch(List<?> list){
        try(InfluxDB influxDB = getConnection()) {
            BatchOptions options = BatchOptions.DEFAULTS.actions(100).flushDuration(100).jitterDuration(20);
            influxDB.enableBatch(options);
            InfluxDBMapper mapper = new InfluxDBMapper(influxDB);
            list.forEach(mapper::save);
            influxDB.flush();
        }catch (Exception e){
            log.error("writeBatch fail exception:{}",e,e);
        }
    }

    private void write(List<?> list){
        try(InfluxDB influxDB = getConnection()) {
            InfluxDBMapper mapper = new InfluxDBMapper(influxDB);
            list.forEach(mapper::save);
            //influxDB.flush();
        }catch (Exception e){
            log.error("write exception:{}",e,e);
        }
    }

    public synchronized void writeMetric(List<?> list){
        if(list.size()==0)return;
        if(list.size()>butchSize)writeBatch(list);
        else write(list);
    }


}
