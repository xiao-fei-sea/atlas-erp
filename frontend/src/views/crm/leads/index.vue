<template>
  <div class="page">
    <div class="page-header">
      <h2>客户线索</h2>
      <div>
        <el-button v-permission="'crm:lead:create'" type="primary" :icon="Plus" @click="openCreate">
          新增线索
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
            placeholder="名称/公司/电话"
            style="width: 200px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 150px">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="name" label="名称" min-width="130" />
        <el-table-column prop="company" label="公司" min-width="160" />
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column prop="source" label="来源" width="120" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ownerId" label="负责人" width="100" />
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'crm:lead:update'" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="row.status !== 'CONVERTED'"
              v-permission="'crm:lead:update'"
              link
              type="success"
              @click="convert(row)"
            >
              转化
            </el-button>
            <el-button v-permission="'crm:lead:update'" link type="danger" @click="removeRow(row)">
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑线索' : '新增线索'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入线索名称" />
        </el-form-item>
        <el-form-item label="公司">
          <el-input v-model="form.company" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.source" placeholder="如 展会 / 转介绍 / 官网" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
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
import { crmApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', status: undefined })

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  company: '',
  phone: '',
  source: '',
  status: 'NEW',
  ownerId: null,
  remark: ''
})

const statusOptions = [
  { value: 'NEW', label: '新线索' },
  { value: 'FOLLOWING', label: '跟进中' },
  { value: 'CONVERTED', label: '已转化' },
  { value: 'INVALID', label: '无效' }
]

const rules = {
  name: [{ required: true, message: '请输入线索名称', trigger: 'blur' }]
}

function statusText(status) {
  const item = statusOptions.find((i) => i.value === status)
  return item ? item.label : status || '-'
}

function statusTagType(status) {
  if (status === 'CONVERTED') return 'success'
  if (status === 'INVALID') return 'danger'
  if (status === 'FOLLOWING') return 'warning'
  return 'info'
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.status) params.status = query.status
    const data = await crmApi.leads(params)
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
  query.status = undefined
  search()
}

function openCreate() {
  Object.assign(form, {
    id: null,
    name: '',
    company: '',
    phone: '',
    source: '',
    status: 'NEW',
    ownerId: null,
    remark: ''
  })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    company: row.company,
    phone: row.phone,
    source: row.source,
    status: row.status,
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
      company: form.company,
      phone: form.phone,
      source: form.source,
      status: form.status,
      ownerId: form.ownerId,
      remark: form.remark
    }
    if (form.id) {
      await crmApi.updateLead(form.id, payload)
    } else {
      await crmApi.createLead(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function convert(row) {
  await ElMessageBox.confirm(`确认将线索「${row.name}」转化为客户？`, '提示', { type: 'warning' })
  await crmApi.convertLead(row.id)
  ElMessage.success('转化成功')
  load()
}

async function removeRow(row) {
  await ElMessageBox.confirm(`确认删除线索「${row.name}」？`, '提示', { type: 'warning' })
  await crmApi.deleteLead(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
