package com.joao.storemanagement.vo.primary;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@Builder
public class DownloadFileVO {

    private final Path path;
    private final String filename;
}
