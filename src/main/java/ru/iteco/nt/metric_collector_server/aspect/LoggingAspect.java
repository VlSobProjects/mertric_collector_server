package ru.iteco.nt.metric_collector_server.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import ru.iteco.nt.metric_collector_server.aspect.annotations.LogMethodReturn;

import java.lang.reflect.Method;
import java.util.Arrays;


@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("@annotation(ru.iteco.nt.metric_collector_server.aspect.annotations.LogMethodReturn)")
    public void logReturn(){}

    @AfterReturning(pointcut="logReturn()", returning="retVal")
    public void logMethodReturn(JoinPoint joinPoint,Object retVal){
        MethodSignature ms = (MethodSignature)joinPoint.getSignature();
        Method m = ms.getMethod();
        LogMethodReturn a = m.getAnnotation(LogMethodReturn.class);
        if((a.isInfo() && log.isInfoEnabled()) || log.isDebugEnabled()){
            StringBuilder line = new StringBuilder(String.format("[-%s.%s-] ",joinPoint.getTarget().getClass().getSimpleName(),m.getName()));
            if(a.includeArgs()) line.append(String.format("args: %s, ", Arrays.asList(joinPoint.getArgs())));
            line.append("return: ").append(retVal);
            if(a.isInfo()) log.info(line.toString());
            else log.debug(line.toString());
        }
    }

}
