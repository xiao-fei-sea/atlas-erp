<template>
  <div class="page dashboard-page">
    <section class="dashboard-intro">
      <div class="intro-copy">
        <p class="intro-date">{{ todayLabel }}</p>
        <h1>经营仪表盘</h1>
        <p class="intro-text">{{ greeting }}，{{ displayName }}。订单、库存与资金的最新状态已汇总在这里。</p>
      </div>
      <div class="intro-actions">
        <span class="sync-status"><span class="sync-dot"></span>数据已同步</span>
        <el-button class="refresh-btn" :icon="Refresh" @click="load">刷新数据</el-button>
      </div>
    </section>

    <section class="stat-grid">
      <article v-for="card in cards" :key="card.label" class="stat-card" :class="`tone-${card.tone}`">
        <div class="stat-top">
          <span class="stat-label">{{ card.label }}</span>
          <span class="stat-icon">
            <el-icon><component :is="card.icon" /></el-icon>
          </span>
        </div>
        <div class="value">{{ card.value }}</div>
      </article>
    </section>

    <section class="chart-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-head">
            <div>
              <div class="panel-title">销售 / 采购金额趋势</div>
              <div class="panel-sub">近 12 个月 · 单位：元</div>
            </div>
          </div>
        </template>
        <div ref="trendRef" class="chart"></div>
      </el-card>
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-head">
            <div>
              <div class="panel-title">销售订单状态分布</div>
              <div class="panel-sub">按订单数量统计</div>
            </div>
          </div>
        </template>
        <div ref="pieRef" class="chart"></div>
      </el-card>
    </section>

    <el-card shadow="never" class="panel-card top-card">
      <template #header>
        <div class="panel-head">
          <div>
            <div class="panel-title">热销商品 TOP 5</div>
            <div class="panel-sub">按销售金额排序</div>
          </div>
        </div>
      </template>
      <el-table :data="topProducts" stripe>
        <el-table-column prop="productCode" label="商品编码" width="140" />
        <el-table-column prop="productName" label="商品名称" min-width="220" />
        <el-table-column prop="qty" label="销售数量" width="120" align="right" />
        <el-table-column prop="amount" label="销售金额" width="150" align="right" :formatter="moneyCell" />
        <el-table-column prop="orderCount" label="订单数" width="110" align="right" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import {
  Box,
  Coin,
  CreditCard,
  DocumentChecked,
  Refresh,
  ShoppingCart,
  TrendCharts,
  Wallet,
  Warning
} from '@element-plus/icons-vue'
import { reportApi } from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const data = ref({ summary: {}, salesTrend: [], purchaseTrend: [], topProducts: [], salesStatus: [] })
const trendRef = ref()
const pieRef = ref()
let trendChart = null
let pieChart = null

const todayLabel = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())

const displayName = computed(() => userStore.user.realName || userStore.user.username || '管理员')
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const cards = computed(() => {
  const s = data.value.summary || {}
  return [
    { label: '今日销售额', value: money(s.todaySalesAmount), icon: TrendCharts, tone: 'jade' },
    { label: '本月销售额', value: money(s.monthSalesAmount), icon: Coin, tone: 'jade' },
    { label: '本月采购额', value: money(s.monthPurchaseAmount), icon: ShoppingCart, tone: 'amber' },
    { label: '待审批任务', value: s.pendingApprovals || 0, icon: DocumentChecked, tone: 'ink' },
    { label: '应收余额', value: money(s.receivableOutstanding), icon: Wallet, tone: 'sky' },
    { label: '应付余额', value: money(s.payableOutstanding), icon: CreditCard, tone: 'sky' },
    { label: '库存金额', value: money(s.stockValue), icon: Box, tone: 'stone' },
    { label: '低库存商品', value: s.lowStockProducts || 0, icon: Warning, tone: 'rose' }
  ]
})

const topProducts = computed(() => data.value.topProducts || [])

function money(value) {
  return '¥ ' + Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function moneyCell(row, column, value) {
  return money(value)
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
    color: ['#0f766e', '#b0782f'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#0b1a21',
      borderWidth: 0,
      padding: [10, 14],
      textStyle: { color: '#f3faf7', fontSize: 12 }
    },
    legend: {
      top: 0,
      right: 4,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#65766f', fontSize: 12 }
    },
    grid: { left: 12, right: 16, top: 46, bottom: 10, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: months,
      axisLine: { lineStyle: { color: '#dfe9e5' } },
      axisTick: { show: false },
      axisLabel: { color: '#84938d', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#84938d', fontSize: 11 },
      splitLine: { lineStyle: { color: '#edf2f0' } }
    },
    series: [
      {
        name: '销售',
        type: 'line',
        smooth: 0.35,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2.5 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(15, 118, 110, 0.16)' },
            { offset: 1, color: 'rgba(15, 118, 110, 0)' }
          ])
        },
        data: months.map((m) => salesMap[m] || 0)
      },
      {
        name: '采购',
        type: 'line',
        smooth: 0.35,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2 },
        data: months.map((m) => purchaseMap[m] || 0)
      }
    ]
  })
}

function renderPie() {
  if (!pieRef.value) return
  if (!pieChart) pieChart = echarts.init(pieRef.value)
  const rows = data.value.salesStatus || []
  pieChart.setOption({
    color: ['#0f766e', '#3e8c83', '#b0782f', '#6f8e86', '#c7a36c'],
    tooltip: {
      trigger: 'item',
      backgroundColor: '#0b1a21',
      borderWidth: 0,
      padding: [10, 14],
      textStyle: { color: '#f3faf7', fontSize: 12 }
    },
    legend: {
      bottom: 2,
      left: 'center',
      itemWidth: 9,
      itemHeight: 9,
      itemGap: 14,
      textStyle: { color: '#65766f', fontSize: 11 }
    },
    series: [
      {
        type: 'pie',
        radius: ['46%', '70%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderColor: '#ffffff',
          borderWidth: 3,
          borderRadius: 6
        },
        label: { show: false },
        emphasis: {
          label: {
            show: true,
            color: '#1c2b26',
            fontSize: 12,
            fontWeight: 600
          }
        },
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
.dashboard-intro {
  position: relative;
  display: flex;
  overflow: hidden;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 18px;
  padding: 28px 30px;
  border-radius: 18px;
  background: linear-gradient(115deg, #0a1b22 0%, #12352f 58%, #1a5a50 100%);
  box-shadow: 0 18px 42px rgba(9, 42, 35, 0.16);
  color: #ffffff;
}

.dashboard-intro::before {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 42px 42px;
  content: '';
  mask-image: linear-gradient(120deg, rgba(0, 0, 0, 0.9) 0%, transparent 60%);
}

.dashboard-intro::after {
  position: absolute;
  right: 12%;
  bottom: -120px;
  width: 240px;
  height: 240px;
  border: 1px solid rgba(109, 205, 183, 0.18);
  border-radius: 50%;
  box-shadow:
    0 0 0 36px rgba(109, 205, 183, 0.04),
    0 0 0 74px rgba(109, 205, 183, 0.02);
  content: '';
}

.intro-copy,
.intro-actions {
  position: relative;
  z-index: 1;
}

.intro-date {
  margin: 0 0 10px;
  color: #7bb9aa;
  font-size: 12px;
  letter-spacing: 0.5px;
}

.dashboard-intro h1 {
  margin: 0;
  color: #f6fcfa;
  font-size: 30px;
  font-weight: 650;
  letter-spacing: -0.7px;
  line-height: 1.2;
}

.intro-text {
  max-width: 560px;
  margin: 10px 0 0;
  color: #a8c5bd;
  font-size: 13px;
  line-height: 1.65;
}

.intro-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
}

.sync-status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 11px;
  border: 1px solid rgba(255, 255, 255, 0.13);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  color: #c3dbd4;
  font-size: 12px;
}

.sync-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #65d0b6;
  box-shadow: 0 0 0 3px rgba(101, 208, 182, 0.12);
}

.refresh-btn {
  border-color: rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: none;
  color: #143a34;
}

.refresh-btn:hover {
  border-color: #ffffff;
  background: #ffffff;
  color: #0b5d57;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  padding: 17px 18px 16px;
  border: 1px solid #e3ece8;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 10px 28px rgba(24, 55, 46, 0.045);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.stat-card::before {
  position: absolute;
  top: 18px;
  bottom: 18px;
  left: 0;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--tone);
  content: '';
}

.stat-card:hover {
  box-shadow: 0 16px 34px rgba(24, 55, 46, 0.08);
  transform: translateY(-2px);
}

.stat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.stat-label {
  color: #687a73;
  font-size: 13px;
  font-weight: 500;
}

.stat-icon {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 10px;
  background: var(--tone-soft);
  color: var(--tone);
  font-size: 16px;
}

.stat-card .value {
  margin-top: 12px;
  color: #172821;
  font-size: 25px;
  font-variant-numeric: tabular-nums;
  font-weight: 650;
  letter-spacing: -0.6px;
  line-height: 1.15;
}

.tone-jade {
  --tone: #0f766e;
  --tone-soft: #e2f3ef;
}

.tone-amber {
  --tone: #a86f27;
  --tone-soft: #fbf0df;
}

.tone-ink {
  --tone: #314a5c;
  --tone-soft: #edf2f6;
}

.tone-sky {
  --tone: #3c709e;
  --tone-soft: #eaf1f8;
}

.tone-stone {
  --tone: #64746d;
  --tone-soft: #eff3f1;
}

.tone-rose {
  --tone: #a34a43;
  --tone-soft: #fbecea;
}

.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.8fr);
  gap: 16px;
  margin-bottom: 16px;
}

.panel-card {
  border-radius: 16px;
}

.panel-card :deep(.el-card__body) {
  padding: 8px 12px 10px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  color: #263a33;
  font-size: 14px;
  font-weight: 600;
}

.panel-sub {
  margin-top: 4px;
  color: #91a19b;
  font-size: 11px;
  font-weight: 400;
}

.chart {
  width: 100%;
  height: 330px;
}

.top-card {
  margin-top: 0;
}

.top-card :deep(.el-card__body) {
  padding: 8px 12px 12px;
}

@media (max-width: 1180px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .dashboard-intro {
    align-items: flex-start;
    flex-direction: column;
    padding: 24px;
  }

  .dashboard-intro h1 {
    font-size: 26px;
  }

  .intro-actions {
    width: 100%;
    justify-content: space-between;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }

  .chart {
    height: 280px;
  }
}
</style>
