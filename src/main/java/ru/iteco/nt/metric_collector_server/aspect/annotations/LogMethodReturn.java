package ru.iteco.nt.metric_collector_server.aspect.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogMethodReturn {
    boolean isInfo() default false;
    boolean includeArgs() default false;
}
