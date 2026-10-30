package com.erp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String module;
    private String action;
    private String method;
    private String params;
    private String result;
    private Integer success;
    private Long costMs;
    private Long operatorId;
    private String operatorName;
    private String ip;
    private LocalDateTime createdAt;
}
