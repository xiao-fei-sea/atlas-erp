package com.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存流水（只增不改，独立于审计基类）。
 */
@Data
@TableName("inv_flow")
public class InvFlow {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String flowNo;
    private String bizType;
    private String bizNo;
    private Long productId;
    private Long warehouseId;
    private String batchNo;
    private BigDecimal qtyChange;
    private BigDecimal qtyBefore;
    private BigDecimal qtyAfter;
    private BigDecimal unitCost;
    private BigDecimal amount;
    private String refType;
    private Long refId;
    private String remark;
    private Long operatorId;
    private LocalDateTime createdAt;
}
