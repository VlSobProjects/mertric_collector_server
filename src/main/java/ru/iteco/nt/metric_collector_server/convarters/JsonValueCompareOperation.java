package ru.iteco.nt.metric_collector_server.convarters;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
public enum JsonValueCompareOperation {
    EQ((s,j)->j!=null && j.isTextual() && j.textValue().equals(s)),
    CONTAIN((s,j)->j!=null && j.isTextual() &&  j.textValue().contains(s))
    ;
    private final BiPredicate<String,JsonNode> compare;


}
