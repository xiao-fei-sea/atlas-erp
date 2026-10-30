<template>
  <div class="page">
    <div class="page-header">
      <h2>库存流水</h2>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="商品">
          <el-select
            v-model="query.productId"
            filterable
            clearable
            placeholder="全部"
            style="width: 200px"
          >
            <el-option
              v-for="p in products"
              :key="p.id"
              :label="`${p.code} ${p.name}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="query.warehouseId" clearable placeholder="全部" style="width: 150px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="query.bizType" clearable placeholder="全部" style="width: 150px">
            <el-option v-for="o in bizTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务单号">
          <el-input
            v-model="query.bizNo"
            clearable
            placeholder="请输入单号"
            style="width: 170px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="flowNo" label="流水号" width="150" />
        <el-table-column label="业务类型" width="110">
          <template #default="{ row }">{{ bizTypeLabel(row.bizType) }}</template>
        </el-table-column>
        <el-table-column prop="bizNo" label="业务单号" width="160" show-overflow-tooltip />
        <el-table-column prop="productCode" label="商品编码" width="130" />
        <el-table-column prop="productName" label="商品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="110" />
        <el-table-column prop="batchNo" label="批次" width="110" />
        <el-table-column label="变动数量" width="100" align="right">
          <template #default="{ row }">{{ Number(row.qtyChange || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="变动前" width="100" align="right">
          <template #default="{ row }">{{ Number(row.qtyBefore || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="变动后" width="100" align="right">
          <template #default="{ row }">{{ Number(row.qtyAfter || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="单位成本" width="110" align="right">
          <template #default="{ row }">{{ Number(row.unitCost || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">{{ Number(row.amount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { productApi, stockApi, warehouseApi } from '@/api'

const bizTypeOptions = [
  { label: '其他入库', value: 'INBOUND' },
  { label: '其他出库', value: 'OUTBOUND' },
  { label: '调拨入库', value: 'TRANSFER_IN' },
  { label: '调拨出库', value: 'TRANSFER_OUT' },
  { label: '盘盈入库', value: 'STOCKTAKE_IN' },
  { label: '盘亏出库', value: 'STOCKTAKE_OUT' },
  { label: '采购入库', value: 'PUR_RECEIPT' },
  { label: '销售出库', value: 'SAL_SHIPMENT' }
]

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const products = ref([])
const warehouses = ref([])

const query = reactive({
  page: 1,
  size: 10,
  productId: undefined,
  warehouseId: undefined,
  bizType: undefined,
  bizNo: ''
})

function bizTypeLabel(type) {
  const item = bizTypeOptions.find((o) => o.value === type)
  return item ? item.label : type || '-'
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}

function buildParams() {
  const params = { page: query.page, size: query.size }
  if (query.productId) params.productId = query.productId
  if (query.warehouseId) params.warehouseId = query.warehouseId
  if (query.bizType) params.bizType = query.bizType
  if (query.bizNo) params.bizNo = query.bizNo
  return params
}

async function load() {
  loading.value = true
  try {
    const data = await stockApi.flows(buildParams())
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
  query.productId = undefined
  query.warehouseId = undefined
  query.bizType = undefined
  query.bizNo = ''
  search()
}

async function loadOptions() {
  const [productList, warehouseList] = await Promise.all([
    productApi.options(),
    warehouseApi.options()
  ])
  products.value = productList || []
  warehouses.value = warehouseList || []
}

onMounted(() => {
  loadOptions()
  load()
})
</script>
