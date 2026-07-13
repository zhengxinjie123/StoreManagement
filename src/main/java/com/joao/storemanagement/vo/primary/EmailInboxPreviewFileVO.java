package com.joao.storemanagement.vo.primary;

import java.nio.file.Path;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailInboxPreviewFileVO {

    private final Path path;
    private final String filename;
    private final String contentType;
    private final boolean inline;
}
