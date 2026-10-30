<template>
  <div class="page">
    <div class="page-header">
      <h2>库存盘点</h2>
      <el-button
        v-permission="'inventory:stocktake:create'"
        type="primary"
        :icon="Plus"
        @click="openCreate"
      >
        新建盘点单
      </el-button>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 150px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="query.warehouseId" clearable placeholder="全部" style="width: 160px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="stocktakeNo" label="盘点单号" width="170" />
        <el-table-column label="仓库" width="140">
          <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
        </el-table-column>
        <el-table-column prop="stocktakeDate" label="盘点日期" width="120" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row, false)">详情</el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              v-permission="'inventory:stocktake:create'"
              link
              type="primary"
              @click="openDetail(row, true)"
            >
              录入实盘
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              v-permission="'inventory:stocktake:submit'"
              link
              type="success"
              @click="submitRow(row)"
            >
              提交
            </el-button>
            <el-button
              v-if="row.status === 'SUBMITTED'"
              v-permission="'inventory:stocktake:approve'"
              link
              type="warning"
              @click="approveRow(row)"
            >
              审核
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

    <el-dialog v-model="createVisible" title="新建盘点单" width="560px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="盘点仓库" required>
          <el-select v-model="createForm.warehouseId" placeholder="请选择仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点日期" required>
          <el-date-picker
            v-model="createForm.stocktakeDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitCreate">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="880px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="盘点单号">
          {{ detailData.stocktake?.stocktakeNo }}
        </el-descriptions-item>
        <el-descriptions-item label="仓库">
          {{ warehouseName(detailData.stocktake?.warehouseId) }}
        </el-descriptions-item>
        <el-descriptions-item label="盘点日期">
          {{ detailData.stocktake?.stocktakeDate }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ statusLabel(detailData.stocktake?.status) }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ detailData.stocktake?.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-table :data="detailData.items" border stripe style="margin-top: 12px">
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">{{ productLabel(row.productId) }}</template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次" width="130" />
        <el-table-column label="系统数量" width="110" align="right">
          <template #default="{ row }">{{ Number(row.systemQty || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="实盘数量" width="170" align="right">
          <template #default="{ row }">
            <el-input-number
              v-if="editable"
              v-model="row.actualQty"
              :min="0"
              :precision="4"
              size="small"
              style="width: 140px"
              @change="saveActual(row)"
            />
            <span v-else>{{ Number(row.actualQty || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="差异数量" width="110" align="right">
          <template #default="{ row }">{{ Number(row.diffQty || 0).toFixed(2) }}</template>
        </el-table-column>
      </el-table>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { productApi, stocktakeApi, warehouseApi } from '@/api'

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '已审核', value: 'APPROVED' }
]

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const warehouses = ref([])
const products = ref([])

const query = reactive({ page: 1, size: 10, status: undefined, warehouseId: undefined })

const createVisible = ref(false)
const createForm = reactive({ warehouseId: undefined, stocktakeDate: '', remark: '' })

const detailVisible = ref(false)
const detailTitle = ref('盘点单详情')
const detailData = ref({ stocktake: {}, items: [] })
const editMode = ref(false)

const editable = computed(
  () => editMode.value && detailData.value.stocktake && detailData.value.stocktake.status === 'DRAFT'
)

function today() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function statusLabel(status) {
  const item = statusOptions.find((o) => o.value === status)
  return item ? item.label : status || '-'
}

function statusTag(status) {
  if (status === 'APPROVED') return 'success'
  if (status === 'SUBMITTED') return 'warning'
  return 'info'
}

function warehouseName(id) {
  const item = warehouses.value.find((w) => w.id === id)
  return item ? item.name : '-'
}

function productLabel(id) {
  const item = products.value.find((p) => p.id === id)
  return item ? `${item.code} ${item.name}` : id
}

function buildParams() {
  const params = { page: query.page, size: query.size }
  if (query.status) params.status = query.status
  if (query.warehouseId) params.warehouseId = query.warehouseId
  return params
}

async function load() {
  loading.value = true
  try {
    const data = await stocktakeApi.page(buildParams())
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
  query.warehouseId = undefined
  search()
}

async function loadOptions() {
  const [warehouseList, productList] = await Promise.all([
    warehouseApi.options(),
    productApi.options()
  ])
  warehouses.value = warehouseList || []
  products.value = productList || []
}

function openCreate() {
  createForm.warehouseId = undefined
  createForm.stocktakeDate = today()
  createForm.remark = ''
  createVisible.value = true
}

async function submitCreate() {
  if (!createForm.warehouseId) {
    ElMessage.warning('请选择盘点仓库')
    return
  }
  saving.value = true
  try {
    await stocktakeApi.create({
      warehouseId: createForm.warehouseId,
      stocktakeDate: createForm.stocktakeDate,
      remark: createForm.remark,
      loadStocks: true
    })
    ElMessage.success('创建成功')
    createVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openDetail(row, edit) {
  const data = await stocktakeApi.detail(row.id)
  detailTitle.value = `${edit ? '录入实盘' : '盘点单详情'} - ${row.stocktakeNo}`
  detailData.value = { stocktake: data.stocktake || {}, items: data.items || [] }
  editMode.value = edit
  detailVisible.value = true
}

async function saveActual(row) {
  const stocktakeId = detailData.value.stocktake?.id
  if (!stocktakeId) return
  try {
    await stocktakeApi.updateActual(stocktakeId, row.id, row.actualQty)
    row.diffQty = Number(row.actualQty || 0) - Number(row.systemQty || 0)
  } catch {
    const data = await stocktakeApi.detail(stocktakeId)
    detailData.value = { stocktake: data.stocktake || {}, items: data.items || [] }
  }
}

async function submitRow(row) {
  await ElMessageBox.confirm(`确认提交盘点单「${row.stocktakeNo}」？`, '提示', { type: 'warning' })
  await stocktakeApi.submit(row.id)
  ElMessage.success('提交成功')
  load()
}

async function approveRow(row) {
  await ElMessageBox.confirm(`确认审核盘点单「${row.stocktakeNo}」？审核后将调整库存。`, '提示', {
    type: 'warning'
  })
  await stocktakeApi.approve(row.id)
  ElMessage.success('审核成功')
  load()
}

onMounted(() => {
  loadOptions()
  load()
})
</script>
