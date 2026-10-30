<template>
  <div class="page">
    <div class="page-header">
      <h2>库存报表</h2>
      <div class="header-actions">
        <span class="muted">统计天数</span>
        <el-input-number v-model="days" :min="1" :max="365" style="width: 120px" />
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        <el-button
          v-permission="'report:inventory:view'"
          :icon="Download"
          :loading="exporting"
          @click="exportLowStock"
        >
          导出低库存
        </el-button>
      </div>
    </div>

    <div class="stat-cards">
      <div v-for="card in cards" :key="card.label" class="stat-card">
        <div class="label">{{ card.label }}</div>
        <div class="value">{{ card.value }}</div>
      </div>
    </div>

    <el-row :gutter="12">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>仓库库存金额分布</template>
          <div ref="chartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>分类库存</template>
          <el-table :data="data.byCategory" border stripe>
            <el-table-column prop="categoryName" label="分类" min-width="140" />
            <el-table-column prop="qty" label="库存数量" width="120" align="right">
              <template #default="{ row }">{{ Number(row.qty || 0) }}</template>
            </el-table-column>
            <el-table-column prop="value" label="库存金额" width="150" align="right">
              <template #default="{ row }">{{ money(row.value) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 12px">
      <template #header>低库存清单</template>
      <el-table :data="data.lowStock" border stripe>
        <el-table-column prop="productCode" label="商品编码" width="140" />
        <el-table-column prop="productName" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="safetyStock" label="安全库存" width="120" align="right">
          <template #default="{ row }">{{ Number(row.safetyStock || 0) }}</template>
        </el-table-column>
        <el-table-column prop="qtyOnHand" label="当前库存" width="120" align="right">
          <template #default="{ row }">{{ Number(row.qtyOnHand || 0) }}</template>
        </el-table-column>
        <el-table-column prop="stockValue" label="库存金额" width="150" align="right">
          <template #default="{ row }">{{ money(row.stockValue) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <template #header>近 {{ days }} 天流水汇总</template>
      <el-table :data="data.flowSummary" border stripe empty-text="暂无流水数据">
        <el-table-column prop="bizType" label="业务类型" min-width="150">
          <template #default="{ row }">{{ bizTypeText(row.bizType) }}</template>
        </el-table-column>
        <el-table-column prop="flowCount" label="流水笔数" width="120" align="right" />
        <el-table-column prop="inQty" label="入库数量" width="130" align="right">
          <template #default="{ row }">{{ Number(row.inQty || 0) }}</template>
        </el-table-column>
        <el-table-column prop="outQty" label="出库数量" width="130" align="right">
          <template #default="{ row }">{{ Number(row.outQty || 0) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { Download, Search } from '@element-plus/icons-vue'
import { reportApi } from '@/api'

const data = ref({ summary: {}, byWarehouse: [], byCategory: [], lowStock: [], flowSummary: [] })
const chartRef = ref()
const days = ref(30)
const exporting = ref(false)
let chart = null

const bizTypeMap = {
  PUR_RECEIPT: '采购入库',
  SAL_SHIPMENT: '销售出库',
  TRANSFER: '库存调拨',
  STOCKTAKE: '库存盘点',
  MANUAL: '手工调整'
}

function money(value) {
  return '¥ ' + Number(value || 0).toFixed(2)
}

function bizTypeText(bizType) {
  return bizTypeMap[bizType] || bizType || '-'
}

const cards = computed(() => {
  const summary = data.value.summary || {}
  return [
    { label: '库存总量', value: summary.totalQty || 0 },
    { label: '库存金额', value: money(summary.totalValue) },
    { label: '商品数', value: summary.productCount || 0 },
    { label: '仓库数', value: summary.warehouseCount || 0 }
  ]
})

function renderChart() {
  if (!chart) return
  const rows = data.value.byWarehouse || []
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        name: '库存金额',
        type: 'pie',
        radius: ['40%', '65%'],
        data: rows.map((i) => ({ name: i.warehouseName || '未分配', value: Number(i.value || 0) }))
      }
    ]
  })
}

function resize() {
  if (chart) chart.resize()
}

async function load() {
  data.value = await reportApi.inventory({ days: days.value || 30 })
  renderChart()
}

async function exportLowStock() {
  exporting.value = true
  try {
    await reportApi.exportLowStock()
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  chart = echarts.init(chartRef.value)
  load()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) chart.dispose()
})
</script>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart {
  height: 320px;
}
</style>
