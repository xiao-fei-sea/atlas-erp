package com.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_transfer_item")
public class InvTransferItem extends BaseEntity {

    private Long transferId;
    private Long productId;
    private String batchNo;
    private BigDecimal qty;
}
