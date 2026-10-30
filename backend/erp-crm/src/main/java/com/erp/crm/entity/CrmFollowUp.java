package com.erp.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crm_follow_up")
public class CrmFollowUp {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String targetType;
    private Long targetId;
    private Long customerId;
    private String content;
    private String followType;
    private LocalDateTime followAt;
    private Long ownerId;
    private LocalDateTime createdAt;
}
