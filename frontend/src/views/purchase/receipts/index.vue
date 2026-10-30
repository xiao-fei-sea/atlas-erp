<template>
  <div class="page">
    <div class="page-header">
      <h2>收货入库</h2>
      <el-button v-permission="'purchase:receipt:create'" type="primary" :icon="Plus" @click="openCreate">
        新增收货单
      </el-button>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 150px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="采购订单">
          <el-select v-model="query.orderId" clearable filterable placeholder="全部" style="width: 260px">
            <el-option v-for="o in allOrders" :key="o.id" :label="o.orderNo" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="receiptNo" label="收货单号" width="170" show-overflow-tooltip />
        <el-table-column label="采购订单" width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ orderNoOf(row.orderId) }}</template>
        </el-table-column>
        <el-table-column label="供应商" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ supplierName(row.supplierId) }}</template>
        </el-table-column>
        <el-table-column label="仓库" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
        </el-table-column>
        <el-table-column prop="receiptDate" label="收货日期" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }">{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              v-permission="'purchase:receipt:confirm'"
              link
              type="success"
              @click="confirmReceipt(row)"
            >
              确认入库
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增收货单" width="900px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="采购订单" prop="orderId">
              <el-select
                v-model="form.orderId"
                filterable
                placeholder="请选择已审核采购订单"
                style="width: 100%"
                @change="onOrderChange"
              >
                <el-option
                  v-for="o in orders"
                  :key="o.id"
                  :label="o.orderNo + '（' + supplierName(o.supplierId) + '）'"
                  :value="o.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收货仓库" prop="warehouseId">
              <el-select v-model="form.warehouseId" filterable placeholder="请选择仓库" style="width: 100%">
                <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="收货日期" prop="receiptDate">
          <el-date-picker v-model="form.receiptDate" type="date" value-format="YYYY-MM-DD" style="width: 220px" />
        </el-form-item>
        <el-form-item label="收货明细">
          <div style="width: 100%">
            <el-table :data="form.items" border size="small">
              <el-table-column width="50" align="center">
                <template #default="{ row }">
                  <el-checkbox v-model="row.checked" :disabled="Number(row.remainingQty) <= 0" />
                </template>
              </el-table-column>
              <el-table-column label="商品编码" width="130">
                <template #default="{ row }">{{ row.productCode }}</template>
              </el-table-column>
              <el-table-column label="商品名称" min-width="150">
                <template #default="{ row }">{{ row.productName }}</template>
              </el-table-column>
              <el-table-column label="订单数量" width="100" align="right">
                <template #default="{ row }">{{ Number(row.orderQty || 0).toFixed(2) }}</template>
              </el-table-column>
              <el-table-column label="已收货" width="90" align="right">
                <template #default="{ row }">{{ Number(row.receivedQty || 0).toFixed(2) }}</template>
              </el-table-column>
              <el-table-column label="剩余" width="90" align="right">
                <template #default="{ row }">{{ Number(row.remainingQty || 0).toFixed(2) }}</template>
              </el-table-column>
              <el-table-column label="收货数量" width="120">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.qty"
                    :precision="2"
                    :min="0.01"
                    :max="Number(row.remainingQty || 0)"
                    :disabled="!row.checked || Number(row.remainingQty) <= 0"
                    :controls="false"
                    style="width: 100%"
                  />
                </template>
              </el-table-column>
              <el-table-column label="批次号" width="140">
                <template #default="{ row }">
                  <el-input v-model="row.batchNo" :disabled="!row.checked" placeholder="选填" />
                </template>
              </el-table-column>
            </el-table>
            <div class="muted" v-if="!form.items.length" style="margin-top: 8px">请先选择采购订单</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveReceipt">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="收货单详情" width="820px">
      <template v-if="detail.receipt">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="收货单号">{{ detail.receipt.receiptNo }}</el-descriptions-item>
          <el-descriptions-item label="采购订单">{{ orderNoOf(detail.receipt.orderId) }}</el-descriptions-item>
          <el-descriptions-item label="仓库">{{ warehouseName(detail.receipt.warehouseId) }}</el-descriptions-item>
          <el-descriptions-item label="收货日期">{{ detail.receipt.receiptDate }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(detail.receipt.status)">{{ statusText(detail.receipt.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="金额">{{ money(detail.receipt.totalAmount) }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.items" border stripe size="small" style="margin-top: 12px">
          <el-table-column prop="productCode" label="商品编码" width="130" />
          <el-table-column prop="productName" label="商品名称" min-width="150" />
          <el-table-column prop="batchNo" label="批次号" width="130" />
          <el-table-column prop="qty" label="收货数量" width="100" align="right" />
          <el-table-column label="单价" width="100" align="right">
            <template #default="{ row }">{{ money(row.price) }}</template>
          </el-table-column>
          <el-table-column label="金额" width="110" align="right">
            <template #default="{ row }">{{ money(row.amount) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { purchaseOrderApi, purchaseReceiptApi, supplierApi, warehouseApi } from '@/api'

const STATUS_MAP = {
  DRAFT: '草稿',
  CONFIRMED: '已确认'
}

const statusOptions = Object.entries(STATUS_MAP).map(([value, label]) => ({ value, label }))

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const orders = ref([])
const allOrders = ref([])
const supplierOptions = ref([])
const warehouseOptions = ref([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref()
const form = ref({ orderId: null, warehouseId: null, receiptDate: '', items: [] })
const detail = ref({ receipt: null, items: [] })

const query = reactive({ page: 1, size: 10, status: undefined, orderId: undefined })

const rules = {
  orderId: [{ required: true, message: '请选择采购订单', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择收货仓库', trigger: 'change' }],
  receiptDate: [{ required: true, message: '请选择收货日期', trigger: 'change' }]
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function statusText(status) {
  return STATUS_MAP[status] || status || '-'
}

function statusTag(status) {
  return status === 'CONFIRMED' ? 'success' : 'info'
}

function supplierName(id) {
  const supplier = supplierOptions.value.find((s) => s.id === id)
  return supplier ? supplier.name : id ? '#' + id : '-'
}

function warehouseName(id) {
  const warehouse = warehouseOptions.value.find((w) => w.id === id)
  return warehouse ? warehouse.name : id ? '#' + id : '-'
}

function orderNoOf(id) {
  const order = allOrders.value.find((o) => o.id === id)
  return order ? order.orderNo : id ? '#' + id : '-'
}

function today() {
  const date = new Date()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return date.getFullYear() + '-' + month + '-' + day
}

function buildParams() {
  const params = { page: query.page, size: query.size }
  if (query.status) params.status = query.status
  if (query.orderId) params.orderId = query.orderId
  return params
}

async function load() {
  loading.value = true
  try {
    const data = await purchaseReceiptApi.page(buildParams())
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.status = undefined
  query.orderId = undefined
  search()
}

async function loadOrders() {
  const [approved, partial, all] = await Promise.all([
    purchaseOrderApi.page({ page: 1, size: 100, status: 'APPROVED' }),
    purchaseOrderApi.page({ page: 1, size: 100, status: 'PARTIAL_RECEIVED' }),
    purchaseOrderApi.page({ page: 1, size: 100 })
  ])
  orders.value = [...(approved.records || []), ...(partial.records || [])]
  allOrders.value = all.records || []
}

async function loadOptions() {
  const [suppliers, warehouses] = await Promise.all([supplierApi.options(), warehouseApi.options()])
  supplierOptions.value = suppliers || []
  warehouseOptions.value = warehouses || []
}

async function openCreate() {
  form.value = { orderId: null, warehouseId: null, receiptDate: today(), items: [] }
  dialogVisible.value = true
}

async function onOrderChange(orderId) {
  form.value.items = []
  if (!orderId) return
  const data = await purchaseOrderApi.detail(orderId)
  form.value.items = (data.items || []).map((item) => ({
    orderItemId: item.id,
    productCode: item.productCode,
    productName: item.productName,
    orderQty: Number(item.qty || 0),
    receivedQty: Number(item.receivedQty || 0),
    remainingQty: Number(item.remainingQty || 0),
    qty: Number(item.remainingQty || 0),
    batchNo: '',
    checked: false
  }))
}

async function saveReceipt() {
  await formRef.value.validate()
  const items = form.value.items.filter((item) => item.checked && Number(item.qty) > 0)
  if (!items.length) {
    ElMessage.warning('请勾选并填写至少一条收货明细')
    return
  }
  if (items.some((item) => Number(item.qty) > Number(item.remainingQty))) {
    ElMessage.warning('收货数量不能超过订单剩余数量')
    return
  }
  saving.value = true
  try {
    await purchaseReceiptApi.create({
      orderId: form.value.orderId,
      warehouseId: form.value.warehouseId,
      receiptDate: form.value.receiptDate,
      items: items.map((item) => ({
        orderItemId: item.orderItemId,
        qty: item.qty,
        batchNo: item.batchNo
      }))
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openDetail(row) {
  detail.value = await purchaseReceiptApi.detail(row.id)
  detailVisible.value = true
}

async function confirmReceipt(row) {
  await ElMessageBox.confirm(`确认收货单「${row.receiptNo}」入库？`, '提示', { type: 'warning' })
  await purchaseReceiptApi.confirm(row.id)
  ElMessage.success('入库成功')
  load()
  loadOrders()
}

onMounted(() => {
  loadOptions()
  loadOrders()
  load()
})
</script>
