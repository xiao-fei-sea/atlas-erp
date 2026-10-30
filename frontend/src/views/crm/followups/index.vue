<template>
  <div class="page">
    <div class="page-header">
      <h2>跟进记录</h2>
      <div>
        <el-button v-permission="'crm:followup:create'" type="primary" :icon="Plus" @click="openCreate">
          新增跟进
        </el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="目标类型">
          <el-select v-model="query.targetType" clearable placeholder="全部" style="width: 150px">
            <el-option v-for="t in targetOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标ID">
          <el-input
            v-model="query.targetId"
            clearable
            placeholder="请输入目标ID"
            style="width: 150px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="targetType" label="目标类型" width="120" :formatter="targetText" />
        <el-table-column prop="targetId" label="目标ID" width="100" />
        <el-table-column prop="customerId" label="客户" width="100" />
        <el-table-column prop="followType" label="跟进方式" width="120" />
        <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="followAt" label="跟进时间" width="170" />
        <el-table-column prop="ownerId" label="负责人" width="100" />
      </el-table>
      <div class="muted">共 {{ rows.length }} 条跟进记录</div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增跟进" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="目标类型" prop="targetType">
          <el-select v-model="form.targetType" placeholder="请选择目标类型" style="width: 100%">
            <el-option v-for="t in targetOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标ID" prop="targetId">
          <el-input-number v-model="form.targetId" :min="1" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="客户ID">
          <el-input-number v-model="form.customerId" :min="1" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="跟进方式">
          <el-input v-model="form.followType" placeholder="如 电话 / 微信 / 拜访" />
        </el-form-item>
        <el-form-item label="跟进时间">
          <el-date-picker
            v-model="form.followAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择跟进时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="跟进内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入跟进内容" />
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
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { crmApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const query = reactive({ targetType: undefined, targetId: '' })

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  targetType: 'CUSTOMER',
  targetId: null,
  customerId: null,
  followType: '',
  content: '',
  followAt: null
})

const targetOptions = [
  { value: 'LEAD', label: '线索' },
  { value: 'OPPORTUNITY', label: '商机' },
  { value: 'CUSTOMER', label: '客户' }
]

const rules = {
  targetType: [{ required: true, message: '请选择目标类型', trigger: 'change' }],
  targetId: [{ required: true, message: '请输入目标ID', trigger: 'blur' }],
  content: [{ required: true, message: '请输入跟进内容', trigger: 'blur' }]
}

function targetText(row) {
  const item = targetOptions.find((i) => i.value === row.targetType)
  return item ? item.label : row.targetType || '-'
}

async function load() {
  loading.value = true
  try {
    const params = {}
    if (query.targetType) params.targetType = query.targetType
    const targetId = Number(query.targetId)
    if (query.targetId && Number.isInteger(targetId)) params.targetId = targetId
    rows.value = (await crmApi.followUps(params)) || []
  } finally {
    loading.value = false
  }
}

function search() {
  load()
}

function reset() {
  query.targetType = undefined
  query.targetId = ''
  load()
}

function openCreate() {
  Object.assign(form, {
    targetType: 'CUSTOMER',
    targetId: null,
    customerId: null,
    followType: '',
    content: '',
    followAt: null
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await crmApi.addFollowUp({
      targetType: form.targetType,
      targetId: form.targetId,
      customerId: form.customerId,
      followType: form.followType,
      content: form.content,
      followAt: form.followAt
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
