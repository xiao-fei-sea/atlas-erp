<template>
  <div class="page">
    <div class="page-header">
      <h2>商机管理</h2>
      <div>
        <el-button
          v-permission="'crm:opportunity:create'"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          新增商机
        </el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="商机名称"
            style="width: 200px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="阶段">
          <el-select v-model="query.stage" clearable placeholder="全部" style="width: 150px">
            <el-option v-for="s in stageOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="name" label="商机名称" min-width="160" />
        <el-table-column label="客户" min-width="150">
          <template #default="{ row }">{{ customerName(row.customerId) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="130" align="right">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="stage" label="阶段" width="120">
          <template #default="{ row }">
            <el-tag :type="stageTagType(row.stage)">{{ stageText(row.stage) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="probability" label="概率" width="90">
          <template #default="{ row }">{{ Number(row.probability || 0) }}%</template>
        </el-table-column>
        <el-table-column prop="expectedCloseDate" label="预计成交日" width="130" />
        <el-table-column prop="ownerId" label="负责人" width="100" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'crm:opportunity:update'"
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-select
              v-permission="'crm:opportunity:update'"
              :model-value="row.stage"
              size="small"
              style="width: 110px; margin: 0 8px"
              @change="(stage) => changeStage(row, stage)"
            >
              <el-option v-for="s in stageOptions" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
            <el-button v-permission="'crm:opportunity:update'" link type="danger" @click="removeRow(row)">
              删除
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商机' : '新增商机'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="商机名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商机名称" />
        </el-form-item>
        <el-form-item label="客户">
          <el-select v-model="form.customerId" clearable filterable placeholder="请选择客户" style="width: 100%">
            <el-option
              v-for="c in customerOptions"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="阶段">
          <el-select v-model="form.stage" placeholder="请选择阶段" style="width: 100%">
            <el-option v-for="s in stageOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="概率(%)">
          <el-input-number v-model="form.probability" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预计成交日">
          <el-date-picker
            v-model="form.expectedCloseDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="负责人ID">
          <el-input-number v-model="form.ownerId" :min="1" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { crmApi, customerApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', stage: undefined })
const customerOptions = ref([])

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  customerId: null,
  amount: 0,
  stage: 'INITIAL',
  probability: 10,
  expectedCloseDate: null,
  ownerId: null,
  remark: ''
})

const stageOptions = [
  { value: 'INITIAL', label: '初步接触' },
  { value: 'QUALIFY', label: '资格确认' },
  { value: 'PROPOSAL', label: '方案报价' },
  { value: 'NEGOTIATION', label: '商务谈判' },
  { value: 'WON', label: '赢单' },
  { value: 'LOST', label: '输单' }
]

const rules = {
  name: [{ required: true, message: '请输入商机名称', trigger: 'blur' }]
}

function stageText(stage) {
  const item = stageOptions.find((i) => i.value === stage)
  return item ? item.label : stage || '-'
}

function stageTagType(stage) {
  if (stage === 'WON') return 'success'
  if (stage === 'LOST') return 'danger'
  if (stage === 'NEGOTIATION') return 'warning'
  return 'info'
}

function customerName(customerId) {
  if (!customerId) return '-'
  const item = customerOptions.value.find((c) => c.id === customerId)
  return item ? item.name : customerId
}

function money(value) {
  return '¥ ' + Number(value || 0).toFixed(2)
}

async function loadCustomers() {
  customerOptions.value = (await customerApi.options()) || []
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.stage) params.stage = query.stage
    const data = await crmApi.opportunities(params)
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
  query.stage = undefined
  search()
}

function openCreate() {
  Object.assign(form, {
    id: null,
    name: '',
    customerId: null,
    amount: 0,
    stage: 'INITIAL',
    probability: 10,
    expectedCloseDate: null,
    ownerId: null,
    remark: ''
  })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    customerId: row.customerId,
    amount: Number(row.amount || 0),
    stage: row.stage,
    probability: row.probability,
    expectedCloseDate: row.expectedCloseDate,
    ownerId: row.ownerId,
    remark: row.remark
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      name: form.name,
      customerId: form.customerId,
      amount: form.amount,
      stage: form.stage,
      probability: form.probability,
      expectedCloseDate: form.expectedCloseDate,
      ownerId: form.ownerId,
      remark: form.remark
    }
    if (form.id) {
      await crmApi.updateOpportunity(form.id, payload)
    } else {
      await crmApi.createOpportunity(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function changeStage(row, stage) {
  try {
    await ElMessageBox.confirm(
      `确认将商机「${row.name}」阶段变更为「${stageText(stage)}」？`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await crmApi.updateStage(row.id, stage)
  ElMessage.success('阶段变更成功')
  load()
}

async function removeRow(row) {
  await ElMessageBox.confirm(`确认删除商机「${row.name}」？`, '提示', { type: 'warning' })
  await crmApi.deleteOpportunity(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  loadCustomers()
  load()
})
</script>
