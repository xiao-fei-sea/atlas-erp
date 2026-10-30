package com.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.inventory.dto.StockView;
import com.erp.inventory.entity.InvStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InvStockMapper extends BaseMapper<InvStock> {

    @Select("SELECT * FROM inv_stock WHERE product_id = #{productId} AND warehouse_id = #{warehouseId} "
            + "AND batch_no = #{batchNo} AND deleted = 0 FOR UPDATE")
    InvStock selectForUpdate(@Param("productId") Long productId,
                             @Param("warehouseId") Long warehouseId,
                             @Param("batchNo") String batchNo);

    @Select("""
            <script>
            SELECT s.id, s.product_id, s.warehouse_id, s.batch_no, s.expiry_date, s.qty_on_hand,
                   s.qty_reserved, s.qty_frozen, s.avg_cost, s.version,
                   p.code AS product_code, p.name AS product_name, p.spec AS product_spec, p.unit_id,
                   w.name AS warehouse_name,
                   (s.qty_on_hand - s.qty_reserved - s.qty_frozen) AS available_qty
              FROM inv_stock s
              LEFT JOIN md_product p ON p.id = s.product_id
              LEFT JOIN md_warehouse w ON w.id = s.warehouse_id
             WHERE s.deleted = 0
             <if test="keyword != null and keyword != ''">
               AND (p.code LIKE CONCAT('%', #{keyword}, '%') OR p.name LIKE CONCAT('%', #{keyword}, '%')
                    OR s.batch_no LIKE CONCAT('%', #{keyword}, '%'))
             </if>
             <if test="warehouseId != null">AND s.warehouse_id = #{warehouseId}</if>
             <if test="onlyAvailable">AND s.qty_on_hand &gt; 0</if>
             ORDER BY s.id DESC
            </script>
            """)
    IPage<StockView> selectStockPage(Page<StockView> page,
                                     @Param("keyword") String keyword,
                                     @Param("warehouseId") Long warehouseId,
                                     @Param("onlyAvailable") boolean onlyAvailable);

    @Select("""
            SELECT s.id, s.product_id, s.warehouse_id, s.batch_no, s.expiry_date, s.qty_on_hand,
                   s.qty_reserved, s.qty_frozen, s.avg_cost, s.version,
                   p.code AS product_code, p.name AS product_name, p.spec AS product_spec, p.unit_id,
                   w.name AS warehouse_name,
                   (s.qty_on_hand - s.qty_reserved - s.qty_frozen) AS available_qty
              FROM inv_stock s
              JOIN md_product p ON p.id = s.product_id
              LEFT JOIN md_warehouse w ON w.id = s.warehouse_id
             WHERE s.deleted = 0 AND p.safety_stock > 0 AND s.qty_on_hand < p.safety_stock
             ORDER BY (p.safety_stock - s.qty_on_hand) DESC
            """)
    List<StockView> selectWarnings();
}
