package com.erp.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.purchase.entity.PurOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PurOrderMapper extends BaseMapper<PurOrder> {

    @Update("UPDATE pur_order SET status = #{toStatus}, updated_at = NOW() WHERE id = #{id} AND status = #{fromStatus}")
    int updateStatus(@Param("id") Long id, @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);

    @Update("UPDATE pur_order SET approval_instance_id = #{instanceId}, updated_at = NOW() WHERE id = #{id}")
    int bindApproval(@Param("id") Long id, @Param("instanceId") Long instanceId);
}
