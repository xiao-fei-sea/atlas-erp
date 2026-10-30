<template>
  <div class="page">
    <div class="page-header">
      <h2>销售订单</h2>
      <el-button v-permission="'sales:order:create'" type="primary" :icon="Plus" @click="openCreate">
        新增销售订单
      </el-button>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 150px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户">
          <el-select v-model="query.customerId" clearable filterable placeholder="全部" style="width: 180px">
            <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="query.keyword" clearable placeholder="请输入" style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="orderNo" label="订单号" width="170" show-overflow-tooltip />
        <el-table-column label="客户" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ customerName(row.customerId) }}</template>
        </el-table-column>
        <el-table-column prop="orderDate" label="订单日期" width="120" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }">{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="税额" width="120" align="right">
          <template #default="{ row }">{{ money(row.taxAmount) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              v-permission="'sales:order:update'"
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              v-permission="'sales:order:submit'"
              link
              type="primary"
              @click="submitOrder(row)"
            >
              提交
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              v-permission="'sales:order:submit'"
              link
              type="warning"
              @click="cancelOrder(row)"
            >
              取消
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              v-permission="'sales:order:update'"
              link
              type="danger"
              @click="removeOrder(row)"
            >
              删除
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

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑销售订单' : '新增销售订单'"
      width="900px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="客户" prop="customerId">
              <el-select v-model="form.customerId" filterable placeholder="请选择客户" style="width: 100%">
                <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单日期" prop="orderDate">
              <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="明细">
          <div style="width: 100%">
            <el-table :data="form.items" border size="small">
              <el-table-column label="商品" min-width="220">
                <template #default="{ row }">
                  <el-select v-model="row.productId" filterable placeholder="请选择商品" style="width: 100%">
                    <el-option
                      v-for="p in productOptions"
                      :key="p.id"
                      :label="p.name + '（' + p.code + '）'"
                      :value="p.id"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="数量" width="110">
                <template #default="{ row }">
                  <el-input-number v-model="row.qty" :precision="2" :min="0.01" :controls="false" style="width: 100%" />
                </template>
              </el-table-column>
              <el-table-column label="单价" width="110">
                <template #default="{ row }">
                  <el-input-number v-model="row.price" :precision="2" :min="0" :controls="false" style="width: 100%" />
                </template>
              </el-table-column>
              <el-table-column label="税率" width="100">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.taxRate"
                    :precision="2"
                    :min="0"
                    :max="1"
                    :step="0.01"
                    :controls="false"
                    style="width: 100%"
                  />
                </template>
              </el-table-column>
              <el-table-column label="行金额" width="110" align="right">
                <template #default="{ row }">{{ money(Number(row.qty || 0) * Number(row.price || 0)) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="70" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" :icon="Delete" @click="removeItem($index)" />
                </template>
              </el-table-column>
            </el-table>
            <el-button link type="primary" :icon="Plus" style="margin-top: 8px" @click="addItem">添加明细</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveOrder">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="销售订单详情" width="820px">
      <template v-if="detail.order">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ customerName(detail.order.customerId) }}</el-descriptions-item>
          <el-descriptions-item label="订单日期">{{ detail.order.orderDate }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(detail.order.status)">{{ statusText(detail.order.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="金额">{{ money(detail.order.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="税额">{{ money(detail.order.taxAmount) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.order.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.items" border stripe size="small" style="margin-top: 12px">
          <el-table-column prop="productCode" label="商品编码" width="130" />
          <el-table-column prop="productName" label="商品名称" min-width="150" />
          <el-table-column prop="spec" label="规格" width="110" />
          <el-table-column prop="qty" label="数量" width="90" align="right" />
          <el-table-column prop="shippedQty" label="已发货" width="90" align="right" />
          <el-table-column label="剩余" width="90" align="right">
            <template #default="{ row }">{{ Number(row.remainingQty || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="单价" width="100" align="right">
            <template #default="{ row }">{{ money(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="taxRate" label="税率" width="80" align="right" />
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
import { Delete, Plus } from '@element-plus/icons-vue'
import { customerApi, productApi, salesOrderApi } from '@/api'

const STATUS_MAP = {
  DRAFT: '草稿',
  PENDING: '审批中',
  APPROVED: '已审核',
  PARTIAL_SHIPPED: '部分发货',
  SHIPPED: '已发货',
  CLOSED: '已关闭',
  CANCELLED: '已取消',
  REJECTED: '已驳回'
}

const statusOptions = Object.entries(STATUS_MAP).map(([value, label]) => ({ value, label }))

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const customerOptions = ref([])
const productOptions = ref([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref()
const form = ref({ id: null, customerId: null, orderDate: '', remark: '', items: [] })
const detail = ref({ order: null, items: [] })

const query = reactive({ page: 1, size: 10, status: undefined, customerId: undefined, keyword: '' })

const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择订单日期', trigger: 'change' }]
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function statusText(status) {
  return STATUS_MAP[status] || status || '-'
}

function statusTag(status) {
  if (status === 'APPROVED' || status === 'SHIPPED') return 'success'
  if (status === 'PENDING' || status === 'PARTIAL_SHIPPED') return 'warning'
  if (status === 'REJECTED') return 'danger'
  return 'info'
}

function customerName(id) {
  const customer = customerOptions.value.find((c) => c.id === id)
  return customer ? customer.name : id ? '#' + id : '-'
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
  if (query.customerId) params.customerId = query.customerId
  if (query.keyword) params.keyword = query.keyword.trim()
  return params
}

async function load() {
  loading.value = true
  try {
    const data = await salesOrderApi.page(buildParams())
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
  query.customerId = undefined
  query.keyword = ''
  search()
}

async function loadOptions() {
  const [customers, products] = await Promise.all([customerApi.options(), productApi.options()])
  customerOptions.value = customers || []
  productOptions.value = products || []
}

function addItem() {
  form.value.items.push({ productId: null, qty: 1, price: 0, taxRate: 0 })
}

function removeItem(index) {
  form.value.items.splice(index, 1)
}

function openCreate() {
  form.value = { id: null, customerId: null, orderDate: today(), remark: '', items: [] }
  addItem()
  dialogVisible.value = true
}

async function openEdit(row) {
  const data = await salesOrderApi.detail(row.id)
  form.value = {
    id: row.id,
    customerId: data.order.customerId,
    orderDate: data.order.orderDate,
    remark: data.order.remark || '',
    items: (data.items || []).map((item) => ({
      productId: item.productId,
      qty: Number(item.qty || 0),
      price: Number(item.price || 0),
      taxRate: Number(item.taxRate || 0)
    }))
  }
  dialogVisible.value = true
}

async function saveOrder() {
  await formRef.value.validate()
  const items = form.value.items.filter((item) => item.productId && Number(item.qty) > 0)
  if (!items.length) {
    ElMessage.warning('请至少添加一条有效明细')
    return
  }
  saving.value = true
  try {
    const body = {
      customerId: form.value.customerId,
      orderDate: form.value.orderDate,
      remark: form.value.remark,
      items: items.map((item) => ({
        productId: item.productId,
        qty: item.qty,
        price: item.price,
        taxRate: item.taxRate
      }))
    }
    if (form.value.id) {
      await salesOrderApi.update(form.value.id, body)
    } else {
      await salesOrderApi.create(body)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openDetail(row) {
  detail.value = await salesOrderApi.detail(row.id)
  detailVisible.value = true
}

async function submitOrder(row) {
  await ElMessageBox.confirm(`确认提交订单「${row.orderNo}」？`, '提示', { type: 'warning' })
  await salesOrderApi.submit(row.id)
  ElMessage.success('提交成功')
  load()
}

async function cancelOrder(row) {
  await ElMessageBox.confirm(`确认取消订单「${row.orderNo}」？`, '提示', { type: 'warning' })
  await salesOrderApi.cancel(row.id)
  ElMessage.success('取消成功')
  load()
}

async function removeOrder(row) {
  await ElMessageBox.confirm(`确认删除订单「${row.orderNo}」？`, '提示', { type: 'warning' })
  await salesOrderApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  loadOptions()
  load()
})
</script>
