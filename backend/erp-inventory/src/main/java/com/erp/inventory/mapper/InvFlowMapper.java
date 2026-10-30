package com.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.inventory.dto.FlowView;
import com.erp.inventory.entity.InvFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InvFlowMapper extends BaseMapper<InvFlow> {

    @Select("""
            <script>
            SELECT f.id, f.flow_no, f.biz_type, f.biz_no, f.product_id, f.batch_no,
                   f.qty_change, f.qty_before, f.qty_after, f.unit_cost, f.amount, f.remark, f.created_at,
                   p.code AS product_code, p.name AS product_name, w.name AS warehouse_name
              FROM inv_flow f
              LEFT JOIN md_product p ON p.id = f.product_id
              LEFT JOIN md_warehouse w ON w.id = f.warehouse_id
             WHERE 1 = 1
             <if test="productId != null">AND f.product_id = #{productId}</if>
             <if test="warehouseId != null">AND f.warehouse_id = #{warehouseId}</if>
             <if test="bizType != null and bizType != ''">AND f.biz_type = #{bizType}</if>
             <if test="bizNo != null and bizNo != ''">AND f.biz_no LIKE CONCAT('%', #{bizNo}, '%')</if>
             ORDER BY f.id DESC
            </script>
            """)
    IPage<FlowView> selectFlowPage(Page<FlowView> page,
                                   @Param("productId") Long productId,
                                   @Param("warehouseId") Long warehouseId,
                                   @Param("bizType") String bizType,
                                   @Param("bizNo") String bizNo);
}
