<template>
  <div class="page">
    <div class="page-header">
      <h2>库存查询</h2>
      <div>
        <el-button v-permission="'inventory:inbound'" type="primary" @click="openChange('inbound')">
          其他入库
        </el-button>
        <el-button v-permission="'inventory:outbound'" type="success" @click="openChange('outbound')">
          其他出库
        </el-button>
        <el-button v-permission="'inventory:stock:freeze'" @click="openFreeze('freeze')">
          冻结
        </el-button>
        <el-button v-permission="'inventory:stock:freeze'" @click="openFreeze('release')">
          解冻
        </el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="仓库">
          <el-select v-model="query.warehouseId" clearable placeholder="全部" style="width: 160px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="编码/名称/批次"
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="仅看有库存">
          <el-switch v-model="query.onlyAvailable" @change="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="productCode" label="商品编码" width="130" />
        <el-table-column prop="productName" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="productSpec" label="规格" width="120" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="batchNo" label="批次" width="120" />
        <el-table-column label="在手" width="100" align="right">
          <template #default="{ row }">{{ Number(row.qtyOnHand || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="可用" width="100" align="right">
          <template #default="{ row }">{{ Number(row.availableQty || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="冻结" width="100" align="right">
          <template #default="{ row }">{{ Number(row.qtyFrozen || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="平均成本" width="110" align="right">
          <template #default="{ row }">{{ Number(row.avgCost || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="库存金额" width="120" align="right">
          <template #default="{ row }">{{ stockValue(row) }}</template>
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
      v-model="changeVisible"
      :title="changeMode === 'inbound' ? '其他入库' : '其他出库'"
      width="560px"
    >
      <el-form :model="changeForm" label-width="90px">
        <el-form-item label="仓库" required>
          <el-select
            v-model="changeForm.warehouseId"
            placeholder="请选择仓库"
            style="width: 100%"
          >
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品" required>
          <el-select
            v-model="changeForm.productId"
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
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number v-model="changeForm.qty" :min="0" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="changeMode === 'inbound'" label="单位成本">
          <el-input-number
            v-model="changeForm.unitCost"
            :min="0"
            :precision="4"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="changeForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="changeVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitChange">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="freezeVisible"
      :title="freezeMode === 'freeze' ? '冻结库存' : '解冻库存'"
      width="520px"
    >
      <el-form :model="freezeForm" label-width="90px">
        <el-form-item label="仓库" required>
          <el-select v-model="freezeForm.warehouseId" placeholder="请选择仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品" required>
          <el-select
            v-model="freezeForm.productId"
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
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number v-model="freezeForm.qty" :min="0" :precision="4" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="freezeVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitFreeze">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { productApi, stockApi, warehouseApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const rows = ref([])
const total = ref(0)
const warehouses = ref([])
const products = ref([])

const query = reactive({ page: 1, size: 10, keyword: '', warehouseId: undefined, onlyAvailable: false })

const changeVisible = ref(false)
const changeMode = ref('inbound')
const changeForm = reactive({ warehouseId: undefined, productId: undefined, qty: 1, unitCost: 0, remark: '' })

const freezeVisible = ref(false)
const freezeMode = ref('freeze')
const freezeForm = reactive({ warehouseId: undefined, productId: undefined, qty: 1 })

function stockValue(row) {
  return (Number(row.qtyOnHand || 0) * Number(row.avgCost || 0)).toFixed(2)
}

async function load() {
  loading.value = true
  try {
    const data = await stockApi.page({ ...query })
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
  query.keyword = ''
  query.warehouseId = undefined
  query.onlyAvailable = false
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

function openChange(mode) {
  changeMode.value = mode
  changeForm.warehouseId = undefined
  changeForm.productId = undefined
  changeForm.qty = 1
  changeForm.unitCost = 0
  changeForm.remark = ''
  changeVisible.value = true
}

function openFreeze(mode) {
  freezeMode.value = mode
  freezeForm.warehouseId = undefined
  freezeForm.productId = undefined
  freezeForm.qty = 1
  freezeVisible.value = true
}

async function submitChange() {
  if (!changeForm.warehouseId) {
    ElMessage.warning('请选择仓库')
    return
  }
  if (!changeForm.productId) {
    ElMessage.warning('请选择商品')
    return
  }
  if (!(Number(changeForm.qty) > 0)) {
    ElMessage.warning('数量必须大于 0')
    return
  }
  submitting.value = true
  try {
    const body = {
      warehouseId: changeForm.warehouseId,
      remark: changeForm.remark,
      items: [{ productId: changeForm.productId, qty: changeForm.qty }]
    }
    if (changeMode.value === 'inbound') {
      body.items[0].unitCost = changeForm.unitCost
      await stockApi.inbound(body)
    } else {
      await stockApi.outbound(body)
    }
    ElMessage.success(changeMode.value === 'inbound' ? '入库成功' : '出库成功')
    changeVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function submitFreeze() {
  if (!freezeForm.warehouseId) {
    ElMessage.warning('请选择仓库')
    return
  }
  if (!freezeForm.productId) {
    ElMessage.warning('请选择商品')
    return
  }
  if (!(Number(freezeForm.qty) > 0)) {
    ElMessage.warning('数量必须大于 0')
    return
  }
  submitting.value = true
  try {
    const body = {
      warehouseId: freezeForm.warehouseId,
      productId: freezeForm.productId,
      qty: freezeForm.qty
    }
    if (freezeMode.value === 'freeze') {
      await stockApi.freeze(body)
    } else {
      await stockApi.release(body)
    }
    ElMessage.success(freezeMode.value === 'freeze' ? '冻结成功' : '解冻成功')
    freezeVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadOptions()
  load()
})
</script>
