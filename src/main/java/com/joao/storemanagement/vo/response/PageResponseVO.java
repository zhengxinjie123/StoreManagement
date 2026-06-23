package com.joao.storemanagement.vo.response;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PageResponseVO<T> {

    private final long current;
    private final long pageSize;
    private final long total;
    private final List<T> records;

    public static <T> PageResponseVO<T> of(IPage<?> page, List<T> records) {
        return PageResponseVO.<T>builder()
                .current(page.getCurrent())
                .pageSize(page.getSize())
                .total(page.getTotal())
                .records(records)
                .build();
    }

    public static <T> PageResponseVO<T> of(long current, long pageSize, long total, List<T> records) {
        return PageResponseVO.<T>builder()
                .current(current)
                .pageSize(pageSize)
                .total(total)
                .records(records)
                .build();
    }
}
