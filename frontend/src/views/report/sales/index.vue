<template>
  <div class="page">
    <div class="page-header">
      <h2>销售报表</h2>
      <div class="header-actions">
        <el-date-picker
          v-model="range"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
        />
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        <el-button
          v-permission="'report:sales:view'"
          :icon="Download"
          :loading="exporting"
          @click="exportData"
        >
          导出
        </el-button>
      </div>
    </div>

    <div class="stat-cards">
      <div v-for="card in cards" :key="card.label" class="stat-card">
        <div class="label">{{ card.label }}</div>
        <div class="value">{{ card.value }}</div>
      </div>
    </div>

    <el-card shadow="never">
      <template #header>每日销售金额趋势</template>
      <div ref="chartRef" class="chart"></div>
    </el-card>

    <el-row :gutter="12" style="margin-top: 12px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>商品销售明细</template>
          <el-table :data="data.byProduct" border stripe>
            <el-table-column prop="productCode" label="商品编码" width="130" />
            <el-table-column prop="productName" label="商品名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="spec" label="规格" width="100" />
            <el-table-column prop="qty" label="数量" width="100" align="right" />
            <el-table-column prop="amount" label="金额" width="130" align="right">
              <template #default="{ row }">{{ money(row.amount) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>客户销售排行</template>
          <el-table :data="data.byCustomer" border stripe>
            <el-table-column prop="customerCode" label="客户编码" width="130" />
            <el-table-column prop="customerName" label="客户名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="orderCount" label="订单数" width="100" align="right" />
            <el-table-column prop="amount" label="金额" width="130" align="right">
              <template #default="{ row }">{{ money(row.amount) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 12px">
      <template #header>订单状态分布</template>
      <el-table :data="data.byStatus" border stripe>
        <el-table-column prop="status" label="状态" width="150">
          <template #default="{ row }">{{ statusText(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="120" align="right" />
        <el-table-column prop="amount" label="金额" width="160" align="right">
          <template #default="{ row }">{{ money(row.amount) }}</template>
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

const data = ref({ summary: {}, byDay: [], byProduct: [], byCustomer: [], byStatus: [] })
const chartRef = ref()
const exporting = ref(false)
let chart = null

const range = ref(defaultRange())

const statusMap = {
  DRAFT: '草稿',
  PENDING: '待审批',
  APPROVED: '已审批',
  REJECTED: '已驳回',
  CANCELLED: '已取消',
  SHIPPED: '已发货',
  PARTIAL_SHIPPED: '部分发货',
  RECEIVED: '已入库',
  PARTIAL_RECEIVED: '部分入库'
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function formatDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function defaultRange() {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 29)
  return [formatDate(start), formatDate(end)]
}

function money(value) {
  return '¥ ' + Number(value || 0).toFixed(2)
}

function statusText(status) {
  return statusMap[status] || status || '-'
}

const cards = computed(() => {
  const summary = data.value.summary || {}
  return [
    { label: '订单数', value: summary.orderCount || 0 },
    { label: '销售金额', value: money(summary.totalAmount) },
    { label: '税额', value: money(summary.taxAmount) },
    { label: '平均单额', value: money(summary.avgAmount) }
  ]
})

function buildParams() {
  const params = {}
  if (range.value && range.value.length === 2) {
    params.startDate = range.value[0]
    params.endDate = range.value[1]
  }
  return params
}

function renderChart() {
  if (!chart) return
  const rows = data.value.byDay || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 70, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: rows.map((i) => i.dateKey) },
    yAxis: { type: 'value' },
    series: [
      {
        name: '销售金额',
        type: 'line',
        smooth: true,
        areaStyle: {},
        data: rows.map((i) => Number(i.amount || 0))
      }
    ]
  })
}

function resize() {
  if (chart) chart.resize()
}

async function load() {
  data.value = await reportApi.sales(buildParams())
  renderChart()
}

async function exportData() {
  exporting.value = true
  try {
    await reportApi.exportSales(buildParams())
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
