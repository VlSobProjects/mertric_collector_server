package ru.iteco.nt.metric_collector_server.influx;

import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.Point;
import ru.iteco.nt.metric_collector_server.MetricWriter;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxToFileConnectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxToFileConnectorConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class InfluxToFileConnector extends MetricWriter<Point, InfluxToFileConnectorConfig, InfluxToFileConnectorResponse,InfluxToFileConnectorResponse.InfluxToFileConnectorResponseBuilder<InfluxToFileConnectorResponse,?>> {

    private final Path filePath;

    public InfluxToFileConnector(InfluxToFileConnectorConfig config) {
        super(config);
        Path file= null;
            try {
                Path p = Paths.get(config.getFilePath());
                file = Files.isDirectory(p) && Files.isDirectory(p) && Files.isWritable(p) ?
                        p.resolve("influx_points.txt") : null;
                if(file!=null){
                    Files.deleteIfExists(file);
                    Files.createFile(file);
                }
            } catch (Exception e) {
                log.error("Fail to create file to write influx Points - config: {} file path: {}",config,file,e);
            }

        filePath = file;
    }

    @SuppressWarnings("unchecked")
    @Override
    public InfluxToFileConnectorResponse.InfluxToFileConnectorResponseBuilder<InfluxToFileConnectorResponse, ?> getBuilder() {
        return (InfluxToFileConnectorResponse.InfluxToFileConnectorResponseBuilder<InfluxToFileConnectorResponse, ?>) InfluxToFileConnectorResponse.builder();
    }

    @Override
    protected void writeData(Collection<Point> data) {
        if(check()){
            LocalDateTime time = LocalDateTime.now();
            List<String> list =  data.stream().map(Point::lineProtocol).map(s->String.format("[%s] %s",time,s)).collect(Collectors.toList());
            try {
                Files.write(filePath,list,StandardOpenOption.APPEND);
            } catch (IOException e) {
                log.error("Fail to write to file influx Points (size: {}) - config: {} file path:{}",list.size(),getConfig(),filePath.toAbsolutePath());
            }
        }

    }

    @Override
    public boolean check(){
        return filePath!=null && Files.isRegularFile(filePath) && Files.isWritable(filePath);
    }


}
