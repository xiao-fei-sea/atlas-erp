<template>
  <div class="page">
    <div class="page-header">
      <h2>应收账款</h2>
    </div>

    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="账龄分析" name="aging">
          <el-table v-loading="agingLoading" :data="agingRows" border stripe>
            <el-table-column prop="partnerName" label="客户" min-width="160" show-overflow-tooltip />
            <el-table-column label="未到期" width="120" align="right">
              <template #default="{ row }">{{ money(row.notDue) }}</template>
            </el-table-column>
            <el-table-column label="1-30 天" width="120" align="right">
              <template #default="{ row }">{{ money(row.overdue1to30) }}</template>
            </el-table-column>
            <el-table-column label="31-60 天" width="120" align="right">
              <template #default="{ row }">{{ money(row.overdue31to60) }}</template>
            </el-table-column>
            <el-table-column label="61-90 天" width="120" align="right">
              <template #default="{ row }">{{ money(row.overdue61to90) }}</template>
            </el-table-column>
            <el-table-column label="90 天以上" width="120" align="right">
              <template #default="{ row }">{{ money(row.overdueOver90) }}</template>
            </el-table-column>
            <el-table-column label="合计" width="130" align="right">
              <template #default="{ row }">{{ money(row.total) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="应收明细" name="detail">
          <el-form :inline="true" class="filter-form" @submit.prevent>
            <el-form-item label="客户">
              <el-select v-model="query.customerId" clearable filterable placeholder="全部" style="width: 180px">
                <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.status" clearable placeholder="全部" style="width: 150px">
                <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="search">查询</el-button>
              <el-button @click="reset">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="loading" :data="rows" border stripe>
            <el-table-column prop="receivableNo" label="应收单号" width="170" show-overflow-tooltip />
            <el-table-column label="客户" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">{{ customerName(row.customerId) }}</template>
            </el-table-column>
            <el-table-column prop="sourceNo" label="来源单号" width="170" show-overflow-tooltip />
            <el-table-column label="金额" width="130" align="right">
              <template #default="{ row }">{{ money(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="已核销" width="120" align="right">
              <template #default="{ row }">{{ money(row.settledAmount) }}</template>
            </el-table-column>
            <el-table-column label="未核销" width="120" align="right">
              <template #default="{ row }">{{ money(Number(row.amount || 0) - Number(row.settledAmount || 0)) }}</template>
            </el-table-column>
            <el-table-column prop="dueDate" label="到期日" width="120" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
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
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { customerApi, receivableApi } from '@/api'

const STATUS_MAP = {
  UNSETTLED: '未核销',
  PARTIAL: '部分核销',
  SETTLED: '已核销'
}

const statusOptions = Object.entries(STATUS_MAP).map(([value, label]) => ({ value, label }))

const activeTab = ref('aging')
const loading = ref(false)
const agingLoading = ref(false)
const rows = ref([])
const total = ref(0)
const agingRows = ref([])
const customerOptions = ref([])

const query = reactive({ page: 1, size: 10, customerId: undefined, status: undefined })

function money(value) {
  return Number(value || 0).toFixed(2)
}

function statusText(status) {
  return STATUS_MAP[status] || status || '-'
}

function statusTag(status) {
  if (status === 'SETTLED') return 'success'
  if (status === 'PARTIAL') return 'warning'
  return 'info'
}

function customerName(id) {
  const customer = customerOptions.value.find((c) => c.id === id)
  return customer ? customer.name : id ? '#' + id : '-'
}

function buildParams() {
  const params = { page: query.page, size: query.size }
  if (query.customerId) params.customerId = query.customerId
  if (query.status) params.status = query.status
  return params
}

async function load() {
  loading.value = true
  try {
    const data = await receivableApi.page(buildParams())
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadAging() {
  agingLoading.value = true
  try {
    agingRows.value = (await receivableApi.aging()) || []
  } finally {
    agingLoading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.customerId = undefined
  query.status = undefined
  search()
}

onMounted(async () => {
  customerOptions.value = (await customerApi.options()) || []
  load()
  loadAging()
})
</script>
