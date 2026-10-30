<template>
  <div class="page">
    <div class="page-header">
      <h2>参数配置</h2>
      <div class="header-actions">
        <el-input
          v-model="groupCode"
          clearable
          placeholder="分组编码"
          style="width: 180px"
          @keyup.enter="load"
        />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button
          v-permission="'system:config:update'"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          新增参数
        </el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="configKey" label="配置键" min-width="200" show-overflow-tooltip />
        <el-table-column prop="configValue" label="配置值" min-width="220" show-overflow-tooltip />
        <el-table-column prop="groupCode" label="分组" width="140" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'system:config:update'"
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-permission="'system:config:update'"
              link
              type="danger"
              @click="removeRow(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑参数' : '新增参数'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="form.configKey" placeholder="如 system.name" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="form.configValue" placeholder="请输入配置值" />
        </el-form-item>
        <el-form-item label="分组" prop="groupCode">
          <el-input v-model="form.groupCode" placeholder="如 SYSTEM" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
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
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { configApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const groupCode = ref('')
const rows = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const rules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }]
}

function emptyForm() {
  return { configKey: '', configValue: '', groupCode: '', remark: '' }
}

const form = ref(emptyForm())

async function load() {
  loading.value = true
  try {
    rows.value = (await configApi.list(groupCode.value || undefined)) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = emptyForm()
  dialogVisible.value = true
}

function openEdit(row) {
  form.value = {
    id: row.id,
    configKey: row.configKey,
    configValue: row.configValue,
    groupCode: row.groupCode,
    remark: row.remark
  }
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await configApi.save({ ...form.value })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function removeRow(row) {
  await ElMessageBox.confirm(`确认删除参数「${row.configKey}」？`, '提示', { type: 'warning' })
  await configApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
