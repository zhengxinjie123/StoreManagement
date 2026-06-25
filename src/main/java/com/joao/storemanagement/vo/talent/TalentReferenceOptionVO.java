package com.joao.storemanagement.vo.talent;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TalentReferenceOptionVO {

    /** 写入 app_config 的值（通常为 GUID，部分配置为名称）。 */
    private final String value;

    /** 下拉展示文本。 */
    private final String label;

    /** 编号或代码，便于区分。 */
    private final String code;
}
