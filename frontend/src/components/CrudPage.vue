<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ title }}</h2>
      <div>
        <slot name="toolbar" />
        <el-button
          v-if="api.create"
          v-permission="perm + ':create'"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          {{ createText || '新增' }}
        </el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-form v-if="filters.length" :inline="true" class="filter-form" @submit.prevent>
        <el-form-item v-for="f in filters" :key="f.prop" :label="f.label">
          <el-select
            v-if="f.type === 'select'"
            v-model="query[f.prop]"
            clearable
            placeholder="全部"
            style="width: 160px"
          >
            <el-option v-for="o in f.options || []" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-input
            v-else
            v-model="query[f.prop]"
            clearable
            placeholder="请输入"
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column
          v-for="c in columns"
          :key="c.prop"
          :prop="c.prop"
          :label="c.label"
          :width="c.width"
          :min-width="c.minWidth"
          :formatter="c.formatter"
          :show-overflow-tooltip="c.tooltip !== false"
        />
        <el-table-column label="操作" :width="actionsWidth" fixed="right">
          <template #default="{ row }">
            <slot name="actions" :row="row" />
            <el-button
              v-if="api.update"
              v-permission="perm + ':update'"
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="api.remove"
              v-permission="perm + ':delete'"
              link
              type="danger"
              @click="removeRow(row)"
            >
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑' + title : '新增' + title" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item v-for="f in fields" :key="f.prop" :label="f.label" :prop="f.prop">
          <el-input
            v-if="!f.type || f.type === 'input'"
            v-model="form[f.prop]"
            :placeholder="f.placeholder || '请输入'"
          />
          <el-input
            v-else-if="f.type === 'textarea'"
            v-model="form[f.prop]"
            type="textarea"
            :rows="3"
            placeholder="请输入"
          />
          <el-input-number
            v-else-if="f.type === 'number'"
            v-model="form[f.prop]"
            :precision="f.precision"
            :min="f.min === undefined ? 0 : f.min"
            :max="f.max"
            style="width: 100%"
          />
          <el-select
            v-else-if="f.type === 'select'"
            v-model="form[f.prop]"
            clearable
            placeholder="请选择"
            style="width: 100%"
          >
            <el-option v-for="o in f.options || []" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-date-picker
            v-else-if="f.type === 'date'"
            v-model="form[f.prop]"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, required: true },
  api: { type: Object, required: true },
  perm: { type: String, default: '' },
  columns: { type: Array, default: () => [] },
  fields: { type: Array, default: () => [] },
  filters: { type: Array, default: () => [] },
  defaultForm: { type: Function, default: () => ({}) },
  actionsWidth: { type: Number, default: 180 },
  createText: { type: String, default: '新增' },
  beforeSubmit: { type: Function, default: null }
})

const emit = defineEmits(['saved'])

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref()
const form = ref({})

const query = reactive({ page: 1, size: 10 })

const rules = computed(() => {
  const result = {}
  props.fields.forEach((f) => {
    if (f.required) {
      result[f.prop] = [{ required: true, message: '请输入' + f.label, trigger: f.type === 'select' ? 'change' : 'blur' }]
    }
  })
  return result
})

function buildParams() {
  const params = { page: query.page, size: query.size }
  props.filters.forEach((f) => {
    const value = query[f.prop]
    if (value !== undefined && value !== null && value !== '') {
      params[f.prop] = value
    }
  })
  return params
}

async function load() {
  loading.value = true
  try {
    const data = await props.api.page(buildParams())
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
  props.filters.forEach((f) => {
    query[f.prop] = f.type === 'select' ? undefined : ''
  })
  search()
}

function openCreate() {
  form.value = { ...props.defaultForm() }
  dialogVisible.value = true
}

async function openEdit(row) {
  form.value = { ...row }
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = props.beforeSubmit ? props.beforeSubmit({ ...form.value }) : { ...form.value }
    if (form.value.id) {
      await props.api.update(form.value.id, payload)
    } else {
      await props.api.create(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    emit('saved')
    load()
  } finally {
    saving.value = false
  }
}

async function removeRow(row) {
  await ElMessageBox.confirm(`确认删除「${row.name || row.code || row.id}」？`, '提示', { type: 'warning' })
  await props.api.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  props.filters.forEach((f) => {
    if (query[f.prop] === undefined) query[f.prop] = f.type === 'select' ? undefined : ''
  })
  load()
})

defineExpose({ load, search })
</script>
