package com.erp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_id_sequence")
public class SysIdSequence implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String seqCode;
    private String prefix;
    private String datePattern;
    private Long currentValue;
    private String dateKey;
    private LocalDateTime updatedAt;
}
