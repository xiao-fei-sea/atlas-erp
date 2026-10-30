package com.erp.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.inventory.dto.TransferRequest;
import com.erp.inventory.entity.InvTransfer;
import com.erp.inventory.entity.InvTransferItem;
import com.erp.inventory.mapper.InvTransferItemMapper;
import com.erp.inventory.mapper.InvTransferMapper;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.mapper.MdProductMapper;
import com.erp.masterdata.service.WarehouseService;
import com.erp.system.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final InvTransferMapper transferMapper;
    private final InvTransferItemMapper itemMapper;
    private final StockService stockService;
    private final WarehouseService warehouseService;
    private final MdProductMapper productMapper;
    private final SequenceService sequenceService;

    public PageResult<InvTransfer> page(long page, long size, String status, Long warehouseId) {
        LambdaQueryWrapper<InvTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), InvTransfer::getStatus, status)
                .and(warehouseId != null, w -> w.eq(InvTransfer::getFromWarehouseId, warehouseId)
                        .or().eq(InvTransfer::getToWarehouseId, warehouseId))
                .orderByDesc(InvTransfer::getId);
        return PageResult.of(transferMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public Map<String, Object> detail(Long id) {
        InvTransfer transfer = getRequired(id);
        List<InvTransferItem> items = itemMapper.selectList(new LambdaQueryWrapper<InvTransferItem>()
                .eq(InvTransferItem::getTransferId, id));
        List<Map<String, Object>> itemViews = new ArrayList<>();
        for (InvTransferItem item : items) {
            MdProduct product = productMapper.selectById(item.getProductId());
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", item.getId());
            view.put("productId", item.getProductId());
            view.put("productCode", product == null ? "" : product.getCode());
            view.put("productName", product == null ? "" : product.getName());
            view.put("batchNo", item.getBatchNo());
            view.put("qty", item.getQty());
            itemViews.add(view);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transfer", transfer);
        data.put("items", itemViews);
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(TransferRequest request) {
        if (request.getFromWarehouseId() == null || request.getToWarehouseId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "调出与调入仓库不能为空");
        }
        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "调出仓库与调入仓库不能相同");
        }
        warehouseService.getRequired(request.getFromWarehouseId());
        warehouseService.getRequired(request.getToWarehouseId());
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "调拨明细不能为空");
        }
        InvTransfer transfer = new InvTransfer();
        transfer.setTransferNo(sequenceService.next("TRANSFER"));
        transfer.setFromWarehouseId(request.getFromWarehouseId());
        transfer.setToWarehouseId(request.getToWarehouseId());
        transfer.setTransferDate(request.getTransferDate() == null ? LocalDate.now() : request.getTransferDate());
        transfer.setStatus("DRAFT");
        transfer.setRemark(request.getRemark());
        transferMapper.insert(transfer);
        for (TransferRequest.Item source : request.getItems()) {
            if (source.getProductId() == null || source.getQty() == null
                    || source.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "调拨明细的商品与数量不正确");
            }
            InvTransferItem item = new InvTransferItem();
            item.setTransferId(transfer.getId());
            item.setProductId(source.getProductId());
            item.setBatchNo(StringUtils.hasText(source.getBatchNo()) ? source.getBatchNo() : "");
            item.setQty(source.getQty());
            itemMapper.insert(item);
        }
        return transfer.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        InvTransfer transfer = getRequired(id);
        int updated = transferMapper.updateStatus(id, "DRAFT", "CONFIRMED");
        if (updated == 0) {
            throw new BizException(ErrorCode.CONFLICT, "仅草稿状态的调拨单可确认");
        }
        List<InvTransferItem> items = itemMapper.selectList(new LambdaQueryWrapper<InvTransferItem>()
                .eq(InvTransferItem::getTransferId, id));
        for (InvTransferItem item : items) {
            BigDecimal cost = stockService.averageCost(item.getProductId(), transfer.getFromWarehouseId(), item.getBatchNo());
            stockService.decrease(item.getProductId(), transfer.getFromWarehouseId(), item.getBatchNo(), item.getQty(),
                    "TRANSFER_OUT", transfer.getTransferNo(), "TRANSFER", transfer.getId(), "调拨出库");
            stockService.increase(item.getProductId(), transfer.getToWarehouseId(), item.getBatchNo(), item.getQty(),
                    cost, "TRANSFER_IN", transfer.getTransferNo(), "TRANSFER", transfer.getId(), "调拨入库");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        getRequired(id);
        int updated = transferMapper.updateStatus(id, "DRAFT", "CANCELLED");
        if (updated == 0) {
            throw new BizException(ErrorCode.CONFLICT, "仅草稿状态的调拨单可取消");
        }
    }

    private InvTransfer getRequired(Long id) {
        InvTransfer transfer = transferMapper.selectById(id);
        if (transfer == null) {
            throw BizException.notFound("调拨单不存在");
        }
        return transfer;
    }
}
