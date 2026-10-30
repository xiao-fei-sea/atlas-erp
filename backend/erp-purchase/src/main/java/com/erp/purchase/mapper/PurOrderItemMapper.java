package com.erp.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.purchase.entity.PurOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface PurOrderItemMapper extends BaseMapper<PurOrderItem> {

    @Update("UPDATE pur_order_item SET received_qty = received_qty + #{qty}, updated_at = NOW() "
            + "WHERE id = #{id} AND deleted = 0 AND received_qty + #{qty} <= qty")
    int addReceivedQty(@Param("id") Long id, @Param("qty") BigDecimal qty);
}
