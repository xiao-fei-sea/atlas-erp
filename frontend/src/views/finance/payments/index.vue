<template>
  <div class="page">
    <div class="page-header">
      <h2>付款管理</h2>
      <el-button v-permission="'finance:payment:create'" type="primary" :icon="Plus" @click="openCreate">
        新增付款单
      </el-button>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="供应商">
          <el-select v-model="query.supplierId" clearable filterable placeholder="全部" style="width: 180px">
            <el-option v-for="s in supplierOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="paymentNo" label="付款单号" width="170" show-overflow-tooltip />
        <el-table-column label="供应商" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ supplierName(row.supplierId) }}</template>
        </el-table-column>
        <el-table-column label="资金账户" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ accountName(row.accountId) }}</template>
        </el-table-column>
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="已核销" width="120" align="right">
          <template #default="{ row }">{{ money(row.usedAmount) }}</template>
        </el-table-column>
        <el-table-column label="未核销" width="120" align="right">
          <template #default="{ row }">{{ money(Number(row.amount || 0) - Number(row.usedAmount || 0)) }}</template>
        </el-table-column>
        <el-table-column prop="paymentDate" label="付款日期" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="Number(row.amount || 0) - Number(row.usedAmount || 0) > 0"
              v-permission="'finance:payment:writeoff'"
              link
              type="primary"
              @click="openWriteoff(row)"
            >
              核销
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
      :title="dialogMode === 'writeoff' ? '付款核销' : '新增付款单'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-alert
        v-if="dialogMode === 'writeoff'"
        :title="'供应商：' + supplierName(form.supplierId) + '，可核销余额：' + money(writeoffRemaining)"
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <template v-if="dialogMode === 'create'">
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="供应商" prop="supplierId">
                <el-select
                  v-model="form.supplierId"
                  filterable
                  placeholder="请选择供应商"
                  style="width: 100%"
                  @change="loadOpenPayables"
                >
                  <el-option v-for="s in supplierOptions" :key="s.id" :label="s.name" :value="s.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="资金账户">
                <el-select v-model="form.accountId" filterable placeholder="请选择资金账户" style="width: 100%">
                  <el-option v-for="a in accountOptions" :key="a.id" :label="a.name" :value="a.id" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="付款金额">
                <el-input-number
                  v-model="form.amount"
                  :precision="2"
                  :min="0"
                  :controls="false"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="付款日期" prop="paymentDate">
                <el-date-picker
                  v-model="form.paymentDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入" />
          </el-form-item>
        </template>
        <el-form-item label="核销明细">
          <div style="width: 100%">
            <el-table :data="form.allocations" border size="small">
              <el-table-column prop="no" label="应付单号" width="170" show-overflow-tooltip />
              <el-table-column prop="dueDate" label="到期日" width="110" />
              <el-table-column label="应付金额" width="110" align="right">
                <template #default="{ row }">{{ money(row.amount) }}</template>
              </el-table-column>
              <el-table-column label="已核销" width="100" align="right">
                <template #default="{ row }">{{ money(row.settledAmount) }}</template>
              </el-table-column>
              <el-table-column label="未核销" width="110" align="right">
                <template #default="{ row }">{{ money(row.outstanding) }}</template>
              </el-table-column>
              <el-table-column label="本次核销" width="140">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.allocAmount"
                    :precision="2"
                    :min="0"
                    :max="Number(row.outstanding || 0)"
                    :controls="false"
                    style="width: 100%"
                  />
                </template>
              </el-table-column>
            </el-table>
            <div class="muted" style="margin-top: 8px" v-if="!form.allocations.length">
              选择供应商后可加载未核销应付列表
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { accountApi, payableApi, paymentApi, supplierApi } from '@/api'

const STATUS_MAP = {
  CONFIRMED: '已确认'
}

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const supplierOptions = ref([])
const accountOptions = ref([])
const dialogVisible = ref(false)
const dialogMode = ref('create')
const writeoffRemaining = ref(0)
const formRef = ref()
const form = ref({
  id: null,
  supplierId: null,
  accountId: null,
  amount: 0,
  paymentDate: '',
  remark: '',
  allocations: []
})

const query = reactive({ page: 1, size: 10, supplierId: undefined })

const rules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  paymentDate: [{ required: true, message: '请选择付款日期', trigger: 'change' }]
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function statusText(status) {
  return STATUS_MAP[status] || status || '-'
}

function statusTag(status) {
  return status === 'CONFIRMED' ? 'success' : 'info'
}

function supplierName(id) {
  const supplier = supplierOptions.value.find((s) => s.id === id)
  return supplier ? supplier.name : id ? '#' + id : '-'
}

function accountName(id) {
  const account = accountOptions.value.find((a) => a.id === id)
  return account ? account.name : id ? '#' + id : '-'
}

function today() {
  const date = new Date()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return date.getFullYear() + '-' + month + '-' + day
}

function buildParams() {
  const params = { page: query.page, size: query.size }
  if (query.supplierId) params.supplierId = query.supplierId
  return params
}

async function load() {
  loading.value = true
  try {
    const data = await paymentApi.page(buildParams())
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
  query.supplierId = undefined
  search()
}

async function loadOpenPayables(supplierId) {
  form.value.allocations = []
  if (!supplierId) return
  const data = await payableApi.open(supplierId)
  form.value.allocations = (data || []).map((item) => ({ ...item, allocAmount: 0 }))
}

function openCreate() {
  dialogMode.value = 'create'
  form.value = {
    id: null,
    supplierId: null,
    accountId: null,
    amount: 0,
    paymentDate: today(),
    remark: '',
    allocations: []
  }
  dialogVisible.value = true
}

async function openWriteoff(row) {
  dialogMode.value = 'writeoff'
  writeoffRemaining.value = Number(row.amount || 0) - Number(row.usedAmount || 0)
  form.value = { id: row.id, supplierId: row.supplierId, allocations: [] }
  await loadOpenPayables(row.supplierId)
  dialogVisible.value = true
}

function collectAllocations() {
  return form.value.allocations
    .filter((item) => Number(item.allocAmount) > 0)
    .map((item) => ({ payableId: item.id, amount: item.allocAmount }))
}

async function save() {
  const allocations = collectAllocations()
  const allocTotal = allocations.reduce((sum, item) => sum + Number(item.amount || 0), 0)
  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      await formRef.value.validate()
      if (!(Number(form.value.amount) > 0)) {
        ElMessage.warning('请输入付款金额')
        return
      }
      if (allocTotal > Number(form.value.amount) + 0.0001) {
        ElMessage.warning('核销金额合计不能超过付款金额')
        return
      }
      await paymentApi.create({
        supplierId: form.value.supplierId,
        accountId: form.value.accountId,
        amount: form.value.amount,
        paymentDate: form.value.paymentDate,
        remark: form.value.remark,
        allocations
      })
      ElMessage.success('保存成功')
    } else {
      if (!allocations.length) {
        ElMessage.warning('请填写核销金额')
        return
      }
      if (allocTotal > writeoffRemaining.value + 0.0001) {
        ElMessage.warning('核销金额合计不能超过可核销余额')
        return
      }
      await paymentApi.writeoff(form.value.id, allocations)
      ElMessage.success('核销成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const [suppliers, accounts] = await Promise.all([supplierApi.options(), accountApi.options()])
  supplierOptions.value = suppliers || []
  accountOptions.value = accounts || []
  load()
})
</script>
