package com.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_stocktake")
public class InvStocktake extends BaseEntity {

    private String stocktakeNo;
    private Long warehouseId;
    private String status;
    private LocalDate stocktakeDate;
    private String remark;
}
