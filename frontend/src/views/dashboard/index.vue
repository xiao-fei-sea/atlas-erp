<template>
  <div class="page">
    <div class="page-header">
      <h2>经营仪表盘</h2>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="stat-cards">
      <div v-for="card in cards" :key="card.label" class="stat-card">
        <div class="label">{{ card.label }}</div>
        <div class="value">{{ card.value }}</div>
      </div>
    </div>

    <el-row :gutter="12">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>近 12 个月销售 / 采购金额趋势</template>
          <div ref="trendRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>销售订单状态分布</template>
          <div ref="pieRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 12px">
      <template #header>热销商品 TOP5</template>
      <el-table :data="topProducts" border stripe>
        <el-table-column prop="productCode" label="商品编码" width="140" />
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column prop="qty" label="销售数量" width="120" />
        <el-table-column prop="amount" label="销售金额" width="140" />
        <el-table-column prop="orderCount" label="订单数" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { reportApi } from '@/api'

const data = ref({ summary: {}, salesTrend: [], purchaseTrend: [], topProducts: [], salesStatus: [] })
const trendRef = ref()
const pieRef = ref()
let trendChart = null
let pieChart = null

const cards = computed(() => {
  const s = data.value.summary || {}
  return [
    { label: '今日销售额', value: money(s.todaySalesAmount) },
    { label: '本月销售额', value: money(s.monthSalesAmount) },
    { label: '本月采购额', value: money(s.monthPurchaseAmount) },
    { label: '待审批任务', value: s.pendingApprovals || 0 },
    { label: '应收余额', value: money(s.receivableOutstanding) },
    { label: '应付余额', value: money(s.payableOutstanding) },
    { label: '库存金额', value: money(s.stockValue) },
    { label: '低库存商品', value: s.lowStockProducts || 0 }
  ]
})

const topProducts = computed(() => data.value.topProducts || [])

function money(value) {
  return '¥ ' + Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function renderTrend() {
  if (!trendRef.value) return
  if (!trendChart) trendChart = echarts.init(trendRef.value)
  const sales = data.value.salesTrend || []
  const purchase = data.value.purchaseTrend || []
  const months = Array.from(new Set([...sales, ...purchase].map((i) => i.month))).sort()
  const salesMap = Object.fromEntries(sales.map((i) => [i.month, Number(i.amount || 0)]))
  const purchaseMap = Object.fromEntries(purchase.map((i) => [i.month, Number(i.amount || 0)]))
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售', '采购'] },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value' },
    series: [
      { name: '销售', type: 'line', smooth: true, data: months.map((m) => salesMap[m] || 0) },
      { name: '采购', type: 'line', smooth: true, data: months.map((m) => purchaseMap[m] || 0) }
    ]
  })
}

function renderPie() {
  if (!pieRef.value) return
  if (!pieChart) pieChart = echarts.init(pieRef.value)
  const rows = data.value.salesStatus || []
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        data: rows.map((i) => ({ name: i.status, value: i.orderCount }))
      }
    ]
  })
}

function resize() {
  if (trendChart) trendChart.resize()
  if (pieChart) pieChart.resize()
}

async function load() {
  data.value = await reportApi.dashboard()
  renderTrend()
  renderPie()
}

onMounted(() => {
  load()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (trendChart) trendChart.dispose()
  if (pieChart) pieChart.dispose()
})
</script>

<style scoped>
.chart {
  height: 320px;
}
</style>
