package com.erp.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 报表查询：直接使用原生 SQL 聚合，避免跨模块 Service 依赖。
 */
@Mapper
public interface ReportMapper {

    @Select("""
            SELECT
              (SELECT IFNULL(SUM(total_amount),0) FROM sal_order WHERE deleted=0 AND status NOT IN ('DRAFT','CANCELLED','REJECTED') AND order_date = CURDATE()) AS todaySalesAmount,
              (SELECT COUNT(*) FROM sal_order WHERE deleted=0 AND order_date = CURDATE()) AS todaySalesOrders,
              (SELECT IFNULL(SUM(total_amount),0) FROM sal_order WHERE deleted=0 AND status NOT IN ('DRAFT','CANCELLED','REJECTED') AND DATE_FORMAT(order_date,'%Y-%m') = DATE_FORMAT(CURDATE(),'%Y-%m')) AS monthSalesAmount,
              (SELECT IFNULL(SUM(total_amount),0) FROM pur_order WHERE deleted=0 AND status NOT IN ('DRAFT','CANCELLED','REJECTED') AND DATE_FORMAT(order_date,'%Y-%m') = DATE_FORMAT(CURDATE(),'%Y-%m')) AS monthPurchaseAmount,
              (SELECT COUNT(*) FROM sal_order WHERE deleted=0 AND status = 'PENDING') AS salesPendingOrders,
              (SELECT COUNT(*) FROM pur_order WHERE deleted=0 AND status = 'PENDING') AS purchasePendingOrders,
              (SELECT COUNT(*) FROM wf_task WHERE status = 'PENDING') AS pendingApprovals,
              (SELECT COUNT(DISTINCT s.product_id) FROM inv_stock s JOIN md_product p ON p.id = s.product_id
                WHERE s.deleted=0 AND p.deleted=0 AND p.safety_stock > 0 AND s.qty_on_hand < p.safety_stock) AS lowStockProducts,
              (SELECT IFNULL(SUM(amount - settled_amount),0) FROM fin_receivable WHERE deleted=0 AND status <> 'SETTLED') AS receivableOutstanding,
              (SELECT IFNULL(SUM(amount - settled_amount),0) FROM fin_payable WHERE deleted=0 AND status <> 'SETTLED') AS payableOutstanding,
              (SELECT IFNULL(SUM(qty_on_hand * avg_cost),0) FROM inv_stock WHERE deleted=0) AS stockValue,
              (SELECT COUNT(*) FROM md_customer WHERE deleted=0 AND status=1) AS customerCount,
              (SELECT COUNT(*) FROM md_product WHERE deleted=0 AND status=1) AS productCount
            """)
    Map<String, Object> dashboardSummary();

    @Select("""
            SELECT DATE_FORMAT(order_date,'%Y-%m') AS month,
                   COUNT(*) AS orderCount,
                   IFNULL(SUM(total_amount),0) AS amount
              FROM sal_order
             WHERE deleted=0 AND status NOT IN ('DRAFT','CANCELLED','REJECTED')
               AND order_date >= DATE_SUB(CURDATE(), INTERVAL 11 MONTH)
             GROUP BY DATE_FORMAT(order_date,'%Y-%m')
             ORDER BY month
            """)
    List<Map<String, Object>> salesTrend();

    @Select("""
            SELECT DATE_FORMAT(order_date,'%Y-%m') AS month,
                   COUNT(*) AS orderCount,
                   IFNULL(SUM(total_amount),0) AS amount
              FROM pur_order
             WHERE deleted=0 AND status NOT IN ('DRAFT','CANCELLED','REJECTED')
               AND order_date >= DATE_SUB(CURDATE(), INTERVAL 11 MONTH)
             GROUP BY DATE_FORMAT(order_date,'%Y-%m')
             ORDER BY month
            """)
    List<Map<String, Object>> purchaseTrend();

    @Select("""
            SELECT p.code AS productCode, p.name AS productName,
                   IFNULL(SUM(i.qty),0) AS qty, IFNULL(SUM(i.amount),0) AS amount,
                   COUNT(DISTINCT o.id) AS orderCount
              FROM sal_order_item i
              JOIN sal_order o ON o.id = i.order_id
              JOIN md_product p ON p.id = i.product_id
             WHERE o.deleted=0 AND i.deleted=0 AND o.status NOT IN ('DRAFT','CANCELLED','REJECTED')
             GROUP BY p.id, p.code, p.name
             ORDER BY amount DESC
             LIMIT #{limit}
            """)
    List<Map<String, Object>> topProducts(@Param("limit") int limit);

    @Select("""
            SELECT status, COUNT(*) AS orderCount, IFNULL(SUM(total_amount),0) AS amount
              FROM sal_order
             WHERE deleted=0
             GROUP BY status
             ORDER BY orderCount DESC
            """)
    List<Map<String, Object>> salesStatusDistribution();

    // ---------------- 销售报表 ----------------

    @Select("""
            <script>
            SELECT COUNT(*) AS orderCount,
                   IFNULL(SUM(total_amount),0) AS totalAmount,
                   IFNULL(SUM(tax_amount),0) AS taxAmount,
                   IFNULL(SUM(discount_amount),0) AS discountAmount,
                   IFNULL(AVG(total_amount),0) AS avgAmount
              FROM sal_order
             WHERE deleted=0 AND status NOT IN ('DRAFT','CANCELLED','REJECTED')
             <if test="startDate != null and startDate != ''">AND order_date &gt;= #{startDate}</if>
             <if test="endDate != null and endDate != ''">AND order_date &lt;= #{endDate}</if>
            </script>
            """)
    Map<String, Object> salesSummary(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("""
            <script>
            SELECT order_date AS dateKey, COUNT(*) AS orderCount, IFNULL(SUM(total_amount),0) AS amount
              FROM sal_order
             WHERE deleted=0 AND status NOT IN ('DRAFT','CANCELLED','REJECTED')
             <if test="startDate != null and startDate != ''">AND order_date &gt;= #{startDate}</if>
             <if test="endDate != null and endDate != ''">AND order_date &lt;= #{endDate}</if>
             GROUP BY order_date
             ORDER BY order_date
            </script>
            """)
    List<Map<String, Object>> salesByDay(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("""
            <script>
            SELECT p.code AS productCode, p.name AS productName, p.spec AS spec,
                   IFNULL(SUM(i.qty),0) AS qty, IFNULL(SUM(i.amount),0) AS amount
              FROM sal_order_item i
              JOIN sal_order o ON o.id = i.order_id
              JOIN md_product p ON p.id = i.product_id
             WHERE o.deleted=0 AND i.deleted=0 AND o.status NOT IN ('DRAFT','CANCELLED','REJECTED')
             <if test="startDate != null and startDate != ''">AND o.order_date &gt;= #{startDate}</if>
             <if test="endDate != null and endDate != ''">AND o.order_date &lt;= #{endDate}</if>
             GROUP BY p.id, p.code, p.name, p.spec
             ORDER BY amount DESC
            </script>
            """)
    List<Map<String, Object>> salesByProduct(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("""
            <script>
            SELECT c.code AS customerCode, c.name AS customerName, COUNT(*) AS orderCount,
                   IFNULL(SUM(o.total_amount),0) AS amount
              FROM sal_order o
              LEFT JOIN md_customer c ON c.id = o.customer_id
             WHERE o.deleted=0 AND o.status NOT IN ('DRAFT','CANCELLED','REJECTED')
             <if test="startDate != null and startDate != ''">AND o.order_date &gt;= #{startDate}</if>
             <if test="endDate != null and endDate != ''">AND o.order_date &lt;= #{endDate}</if>
             GROUP BY c.id, c.code, c.name
             ORDER BY amount DESC
            </script>
            """)
    List<Map<String, Object>> salesByCustomer(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("""
            <script>
            SELECT status, COUNT(*) AS orderCount, IFNULL(SUM(total_amount),0) AS amount
              FROM sal_order
             WHERE deleted=0
             <if test="startDate != null and startDate != ''">AND order_date &gt;= #{startDate}</if>
             <if test="endDate != null and endDate != ''">AND order_date &lt;= #{endDate}</if>
             GROUP BY status
             ORDER BY orderCount DESC
            </script>
            """)
    List<Map<String, Object>> salesStatus(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("""
            <script>
            SELECT o.order_no AS orderNo, c.name AS customerName, o.order_date AS orderDate,
                   o.status AS status, o.total_amount AS totalAmount, o.tax_amount AS taxAmount
              FROM sal_order o
              LEFT JOIN md_customer c ON c.id = o.customer_id
             WHERE o.deleted=0
             <if test="startDate != null and startDate != ''">AND o.order_date &gt;= #{startDate}</if>
             <if test="endDate != null and endDate != ''">AND o.order_date &lt;= #{endDate}</if>
             ORDER BY o.order_date DESC, o.id DESC
            </script>
            """)
    List<Map<String, Object>> salesOrderRows(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // ---------------- 采购报表 ----------------

    @Select("""
            <script>
            SELECT COUNT(*) AS orderCount,
                   IFNULL(SUM(total_amount),0) AS totalAmount,
                   IFNULL(SUM(tax_amount),0) AS taxAmount,
                   IFNULL(SUM(discount_amount),0) AS discountAmount,
                   IFNULL(AVG(total_amount),0) AS avgAmount
              FROM pur_order
             WHERE deleted=0 AND status NOT IN ('DRAFT','CANCELLED','REJECTED')
             <if test="startDate != null and startDate != ''">AND order_date &gt;= #{startDate}</if>
             <if test="endDate != null and endDate != ''">AND order_date &lt;= #{endDate}</if>
            </script>
            """)
    Map<String, Object> purchaseSummary(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("""
            <script>
            SELECT order_date AS dateKey, COUNT(*) AS orderCount, IFNULL(SUM(total_amount),0) AS amount
              FROM pur_order
             WHERE deleted=0 AND status NOT IN ('DRAFT','CANCELLED','REJECTED')
             <if test="startDate != null and startDate != ''">AND order_date &gt;= #{startDate}</if>
             <if test="endDate != null and endDate != ''">AND order_date &lt;= #{endDate}</if>
             GROUP BY order_date
             ORDER BY order_date
            </script>
            """)
    List<Map<String, Object>> purchaseByDay(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("""
            <script>
            SELECT s.code AS supplierCode, s.name AS supplierName, COUNT(*) AS orderCount,
                   IFNULL(SUM(o.total_amount),0) AS amount
              FROM pur_order o
              LEFT JOIN md_supplier s ON s.id = o.supplier_id
             WHERE o.deleted=0 AND o.status NOT IN ('DRAFT','CANCELLED','REJECTED')
             <if test="startDate != null and startDate != ''">AND o.order_date &gt;= #{startDate}</if>
             <if test="endDate != null and endDate != ''">AND o.order_date &lt;= #{endDate}</if>
             GROUP BY s.id, s.code, s.name
             ORDER BY amount DESC
            </script>
            """)
    List<Map<String, Object>> purchaseBySupplier(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("""
            <script>
            SELECT p.code AS productCode, p.name AS productName, p.spec AS spec,
                   IFNULL(SUM(i.qty),0) AS qty, IFNULL(SUM(i.amount),0) AS amount
              FROM pur_order_item i
              JOIN pur_order o ON o.id = i.order_id
              JOIN md_product p ON p.id = i.product_id
             WHERE o.deleted=0 AND i.deleted=0 AND o.status NOT IN ('DRAFT','CANCELLED','REJECTED')
             <if test="startDate != null and startDate != ''">AND o.order_date &gt;= #{startDate}</if>
             <if test="endDate != null and endDate != ''">AND o.order_date &lt;= #{endDate}</if>
             GROUP BY p.id, p.code, p.name, p.spec
             ORDER BY amount DESC
            </script>
            """)
    List<Map<String, Object>> purchaseByProduct(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("""
            <script>
            SELECT status, COUNT(*) AS orderCount, IFNULL(SUM(total_amount),0) AS amount
              FROM pur_order
             WHERE deleted=0
             <if test="startDate != null and startDate != ''">AND order_date &gt;= #{startDate}</if>
             <if test="endDate != null and endDate != ''">AND order_date &lt;= #{endDate}</if>
             GROUP BY status
             ORDER BY orderCount DESC
            </script>
            """)
    List<Map<String, Object>> purchaseStatus(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("""
            <script>
            SELECT o.order_no AS orderNo, s.name AS supplierName, o.order_date AS orderDate,
                   o.status AS status, o.total_amount AS totalAmount, o.tax_amount AS taxAmount
              FROM pur_order o
              LEFT JOIN md_supplier s ON s.id = o.supplier_id
             WHERE o.deleted=0
             <if test="startDate != null and startDate != ''">AND o.order_date &gt;= #{startDate}</if>
             <if test="endDate != null and endDate != ''">AND o.order_date &lt;= #{endDate}</if>
             ORDER BY o.order_date DESC, o.id DESC
            </script>
            """)
    List<Map<String, Object>> purchaseOrderRows(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // ---------------- 库存报表 ----------------

    @Select("""
            SELECT IFNULL(SUM(qty_on_hand),0) AS totalQty,
                   IFNULL(SUM(qty_on_hand * avg_cost),0) AS totalValue,
                   COUNT(*) AS stockRows,
                   COUNT(DISTINCT product_id) AS productCount,
                   COUNT(DISTINCT warehouse_id) AS warehouseCount
              FROM inv_stock
             WHERE deleted=0 AND qty_on_hand <> 0
            """)
    Map<String, Object> stockValueSummary();

    @Select("""
            SELECT w.name AS warehouseName, COUNT(DISTINCT s.product_id) AS productCount,
                   IFNULL(SUM(s.qty_on_hand),0) AS qty,
                   IFNULL(SUM(s.qty_on_hand * s.avg_cost),0) AS value
              FROM inv_stock s
              LEFT JOIN md_warehouse w ON w.id = s.warehouse_id
             WHERE s.deleted=0
             GROUP BY w.id, w.name
             ORDER BY value DESC
            """)
    List<Map<String, Object>> stockByWarehouse();

    @Select("""
            SELECT IFNULL(c.name,'未分类') AS categoryName,
                   IFNULL(SUM(s.qty_on_hand),0) AS qty,
                   IFNULL(SUM(s.qty_on_hand * s.avg_cost),0) AS value
              FROM inv_stock s
              JOIN md_product p ON p.id = s.product_id
              LEFT JOIN md_product_category c ON c.id = p.category_id
             WHERE s.deleted=0 AND p.deleted=0
             GROUP BY c.id, c.name
             ORDER BY value DESC
            """)
    List<Map<String, Object>> stockByCategory();

    @Select("""
            SELECT p.code AS productCode, p.name AS productName, p.spec AS spec,
                   p.safety_stock AS safetyStock,
                   IFNULL(SUM(s.qty_on_hand),0) AS qtyOnHand,
                   IFNULL(SUM(s.qty_on_hand * s.avg_cost),0) AS stockValue
              FROM md_product p
              LEFT JOIN inv_stock s ON s.product_id = p.id AND s.deleted = 0
             WHERE p.deleted=0 AND p.safety_stock > 0
             GROUP BY p.id, p.code, p.name, p.spec, p.safety_stock
            HAVING qtyOnHand < safetyStock
             ORDER BY (safetyStock - qtyOnHand) DESC
             LIMIT 100
            """)
    List<Map<String, Object>> lowStockList();

    @Select("""
            SELECT biz_type AS bizType, COUNT(*) AS flowCount,
                   IFNULL(SUM(CASE WHEN qty_change > 0 THEN qty_change ELSE 0 END),0) AS inQty,
                   IFNULL(SUM(CASE WHEN qty_change < 0 THEN -qty_change ELSE 0 END),0) AS outQty
              FROM inv_flow
             WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
             GROUP BY biz_type
             ORDER BY flowCount DESC
            """)
    List<Map<String, Object>> flowSummary(@Param("days") int days);
}
