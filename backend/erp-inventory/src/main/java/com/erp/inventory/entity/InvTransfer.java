package com.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_transfer")
public class InvTransfer extends BaseEntity {

    private String transferNo;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private String status;
    private LocalDate transferDate;
    private String remark;
}
