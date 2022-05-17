package ru.iteco.nt.metric_collector_server.influx;


import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import ru.iteco.nt.metric_collector_server.MetricWriter;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxDbConnectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxDBConnectorConfig;
import java.util.Collection;



public class InfluxDbConnector extends MetricWriter<Point,InfluxDBConnectorConfig,InfluxDbConnectorResponse,InfluxDbConnectorResponse.InfluxDbConnectorResponseBuilder<InfluxDbConnectorResponse,?>> {

    public InfluxDbConnector(InfluxDBConnectorConfig config){
        super(config);
    }

    @SuppressWarnings("unchecked")
    @Override
    public InfluxDbConnectorResponse.InfluxDbConnectorResponseBuilder<InfluxDbConnectorResponse, ?> getBuilder() {
        return (InfluxDbConnectorResponse.InfluxDbConnectorResponseBuilder<InfluxDbConnectorResponse, ?>) InfluxDbConnectorResponse.builder().success(check());
    }

    @Override
    public boolean check(){
        Pong p = getConnection(false).ping();
        return p!=null && p.isGood();
    }

    private synchronized InfluxDB getConnection(boolean batch){
        InfluxDB influxDB = InfluxDBFactory.connect(getConfig().getUrl(), getConfig().getUser(), getConfig().getPass());
        if(!batch)
            return influxDB.setRetentionPolicy(getConfig().getRetentionPolicy()).setDatabase(getConfig().getDataBase());
        return influxDB;

    }

    @Override
    protected void writeData(Collection<Point> data) {
        if(data.size()>=getConfig().getMinBatchSize()){
            InfluxDB conn = getConnection(true);
            conn.enableBatch();
            getConnection(true).write(convert(data));
        } else {
            InfluxDB conn = getConnection(false);
            conn.disableBatch();
            data.forEach(conn::write);
        }
    }

    private  BatchPoints convert(Collection<Point> points){
        return BatchPoints.database(getConfig().getDataBase()).retentionPolicy(getConfig().getRetentionPolicy()).points(points).build();
    }

}
