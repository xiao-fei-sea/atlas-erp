package com.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_batch")
public class InvBatch extends BaseEntity {

    private Long productId;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
}
