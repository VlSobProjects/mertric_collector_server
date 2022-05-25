package ru.iteco.nt.metric_collector_server.influx;

import org.influxdb.dto.Point;

import ru.iteco.nt.metric_collector_server.MetricCollector;
import ru.iteco.nt.metric_collector_server.MetricCollectorGroup;
import ru.iteco.nt.metric_collector_server.MetricWriter;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorGroupResponse;

import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorGroupConfig;

import java.util.stream.Collectors;

public class InfluxMetricCollectorGroup extends MetricCollectorGroup<
        Point
        ,InfluxMetricCollectorGroupConfig
        ,InfluxMetricCollectorGroupResponse
        ,MetricWriter<Point,?,?,?>
        ,InfluxMetricCollector
        > {

    public InfluxMetricCollectorGroup(InfluxMetricCollectorGroupConfig config,MetricWriter<Point,?,?,?> dbConnector) {
        super(config,dbConnector);
    }

    private String getValidationMessage(){
        String message;
        if(getCollectors().size()==0){
            message = "No collectors.";
        } else {
            long notValidated = getCollectors().stream().filter(m->!m.isDataValidated()).count();
            int collectorSize = getCollectors().size();
            long validateButNotPass = getCollectors().stream().filter(MetricCollector::isDataValidated).filter(m-> !m.isValidateDataPass()).count();
            if(notValidated==collectorSize)
                message = "Metric Collectors is not validated "+notValidated+".";
            else {
                if(notValidated==0){
                    message = "All Collectors validated.";
                } else message = notValidated+" collectors not validated.";
                if(validateButNotPass==0)
                    message+=" All validated collector Pass Validation";
                else message+=" "+validateButNotPass+" fail validation";
            }
        }
        return message;
    }

    @Override
    public InfluxMetricCollectorGroupResponse response() {
        return InfluxMetricCollectorGroupResponse.builder()
                .time(System.currentTimeMillis())
                .collecting(isRunning())
                .writer(getDbConnector().response())
                .collectors(getCollectors().stream().map(InfluxMetricCollector::response).collect(Collectors.toList()))
                .id(getId())
                .validate(isDataValidated())
                .message(getValidationMessage())
                .settings(getConfig())
                .build()
                ;
    }


}
