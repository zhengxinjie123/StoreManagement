package com.joao.storemanagement.entity.primary;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("talent_datasource_config")
public class TalentDataSourceConfig {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("host")
    private String host;

    @TableField("port")
    private Integer port;

    @TableField("database_name")
    private String databaseName;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
