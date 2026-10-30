<template>
  <div class="page">
    <div class="page-header">
      <h2>库存调拨</h2>
      <el-button
        v-permission="'inventory:transfer:create'"
        type="primary"
        :icon="Plus"
        @click="openCreate"
      >
        新建调拨单
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
        <el-table-column prop="transferNo" label="调拨单号" width="160" />
        <el-table-column label="调出仓库" width="140">
          <template #default="{ row }">{{ warehouseName(row.fromWarehouseId) }}</template>
        </el-table-column>
        <el-table-column label="调入仓库" width="140">
          <template #default="{ row }">{{ warehouseName(row.toWarehouseId) }}</template>
        </el-table-column>
        <el-table-column prop="transferDate" label="调拨日期" width="120" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              v-permission="'inventory:transfer:confirm'"
              link
              type="success"
              @click="confirmRow(row)"
            >
              确认
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              v-permission="'inventory:transfer:create'"
              link
              type="danger"
              @click="cancelRow(row)"
            >
              取消
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

    <el-dialog v-model="createVisible" title="新建调拨单" width="820px">
      <el-form :model="createForm" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="调出仓库" required>
              <el-select
                v-model="createForm.fromWarehouseId"
                placeholder="请选择"
                style="width: 100%"
              >
                <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="调入仓库" required>
              <el-select v-model="createForm.toWarehouseId" placeholder="请选择" style="width: 100%">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="调拨日期" required>
              <el-date-picker
                v-model="createForm.transferDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <el-table :data="createItems" border stripe>
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <el-select
              v-model="row.productId"
              filterable
              placeholder="请选择商品"
              style="width: 100%"
            >
              <el-option
                v-for="p in products"
                :key="p.id"
                :label="`${p.code} ${p.name}`"
                :value="p.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="160">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="0" :precision="4" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeItem($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button style="margin-top: 8px" @click="addItem">添加明细</el-button>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitCreate">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="760px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="调拨单号">
          {{ detailData.transfer?.transferNo }}
        </el-descriptions-item>
        <el-descriptions-item label="调出仓库">
          {{ warehouseName(detailData.transfer?.fromWarehouseId) }}
        </el-descriptions-item>
        <el-descriptions-item label="调入仓库">
          {{ warehouseName(detailData.transfer?.toWarehouseId) }}
        </el-descriptions-item>
        <el-descriptions-item label="调拨日期">
          {{ detailData.transfer?.transferDate }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ statusLabel(detailData.transfer?.status) }}
        </el-descriptions-item>
        <el-descriptions-item label="备注">
          {{ detailData.transfer?.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-table :data="detailData.items" border stripe style="margin-top: 12px">
        <el-table-column prop="productCode" label="商品编码" width="140" />
        <el-table-column prop="productName" label="商品名称" min-width="180" />
        <el-table-column prop="batchNo" label="批次" width="130" />
        <el-table-column label="数量" width="120" align="right">
          <template #default="{ row }">{{ Number(row.qty || 0).toFixed(2) }}</template>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { productApi, transferApi, warehouseApi } from '@/api'

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已取消', value: 'CANCELLED' }
]

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const warehouses = ref([])
const products = ref([])

const query = reactive({ page: 1, size: 10, status: undefined, warehouseId: undefined })

const createVisible = ref(false)
const createForm = reactive({
  fromWarehouseId: undefined,
  toWarehouseId: undefined,
  transferDate: '',
  remark: ''
})
const createItems = ref([])

const detailVisible = ref(false)
const detailTitle = ref('调拨单详情')
const detailData = ref({ transfer: {}, items: [] })

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
  if (status === 'CONFIRMED') return 'success'
  if (status === 'CANCELLED') return 'info'
  return 'warning'
}

function warehouseName(id) {
  const item = warehouses.value.find((w) => w.id === id)
  return item ? item.name : '-'
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
    const data = await transferApi.page(buildParams())
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
  createForm.fromWarehouseId = undefined
  createForm.toWarehouseId = undefined
  createForm.transferDate = today()
  createForm.remark = ''
  createItems.value = [{ productId: undefined, qty: 1 }]
  createVisible.value = true
}

function addItem() {
  createItems.value.push({ productId: undefined, qty: 1 })
}

function removeItem(index) {
  createItems.value.splice(index, 1)
}

async function submitCreate() {
  if (!createForm.fromWarehouseId || !createForm.toWarehouseId) {
    ElMessage.warning('请选择调出与调入仓库')
    return
  }
  if (createForm.fromWarehouseId === createForm.toWarehouseId) {
    ElMessage.warning('调出仓库与调入仓库不能相同')
    return
  }
  if (!createItems.value.length) {
    ElMessage.warning('请添加调拨明细')
    return
  }
  const invalid = createItems.value.some((item) => !item.productId || !(Number(item.qty) > 0))
  if (invalid) {
    ElMessage.warning('请完善明细中的商品与数量')
    return
  }
  saving.value = true
  try {
    await transferApi.create({
      fromWarehouseId: createForm.fromWarehouseId,
      toWarehouseId: createForm.toWarehouseId,
      transferDate: createForm.transferDate,
      remark: createForm.remark,
      items: createItems.value.map((item) => ({ productId: item.productId, qty: item.qty }))
    })
    ElMessage.success('创建成功')
    createVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function confirmRow(row) {
  await ElMessageBox.confirm(`确认调拨单「${row.transferNo}」？`, '提示', { type: 'warning' })
  await transferApi.confirm(row.id)
  ElMessage.success('确认成功')
  load()
}

async function cancelRow(row) {
  await ElMessageBox.confirm(`确认取消调拨单「${row.transferNo}」？`, '提示', { type: 'warning' })
  await transferApi.cancel(row.id)
  ElMessage.success('已取消')
  load()
}

async function openDetail(row) {
  const data = await transferApi.detail(row.id)
  detailTitle.value = `调拨单详情 - ${row.transferNo}`
  detailData.value = { transfer: data.transfer || {}, items: data.items || [] }
  detailVisible.value = true
}

onMounted(() => {
  loadOptions()
  load()
})
</script>
