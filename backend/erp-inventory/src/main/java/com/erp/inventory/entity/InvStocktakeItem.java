package com.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_stocktake_item")
public class InvStocktakeItem extends BaseEntity {

    private Long stocktakeId;
    private Long productId;
    private String batchNo;
    private BigDecimal systemQty;
    private BigDecimal actualQty;
    private BigDecimal diffQty;
}
