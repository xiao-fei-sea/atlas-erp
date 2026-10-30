package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_product")
public class MdProduct extends BaseEntity {

    private String code;
    private String name;
    private Long categoryId;
    private String spec;
    private Long unitId;
    private String barcode;
    private BigDecimal taxRate;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private BigDecimal safetyStock;
    private Integer status;
    private String remark;
}
