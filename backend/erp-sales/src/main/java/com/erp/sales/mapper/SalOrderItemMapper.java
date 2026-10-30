package com.erp.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.sales.entity.SalOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface SalOrderItemMapper extends BaseMapper<SalOrderItem> {

    @Update("UPDATE sal_order_item SET shipped_qty = shipped_qty + #{qty}, updated_at = NOW() "
            + "WHERE id = #{id} AND deleted = 0 AND shipped_qty + #{qty} <= qty")
    int addShippedQty(@Param("id") Long id, @Param("qty") BigDecimal qty);
}
