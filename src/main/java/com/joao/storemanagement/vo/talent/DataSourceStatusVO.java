package com.joao.storemanagement.vo.talent;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DataSourceStatusVO {

    private final boolean configured;
    private final boolean connected;
    private final String host;
    private final Integer port;
    private final String databaseName;
    private final String username;
    private final LocalDateTime lastAppliedAt;
    private final String message;
}
