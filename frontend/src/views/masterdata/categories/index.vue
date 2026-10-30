<template>
  <div class="page">
    <div class="page-header">
      <h2>商品分类</h2>
      <div>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button
          v-permission="'masterdata:category:create'"
          type="primary"
          :icon="Plus"
          @click="openCreate()"
        >
          新增
        </el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        border
        stripe
      >
        <el-table-column prop="name" label="名称" min-width="220" />
        <el-table-column prop="code" label="编码" width="160" />
        <el-table-column prop="sort" label="排序" width="90" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'masterdata:category:create'"
              link
              type="primary"
              @click="openCreate(row)"
            >
              新增子分类
            </el-button>
            <el-button
              v-permission="'masterdata:category:update'"
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-permission="'masterdata:category:delete'"
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级分类" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentTree"
            :props="treeProps"
            check-strictly
            placeholder="请选择上级分类"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入分类编码" />
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
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
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { categoryApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const dialogVisible = ref(false)
const formRef = ref()
const form = ref({})

const treeProps = { label: 'name', value: 'id', children: 'children' }
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]
const rules = {
  code: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const parentTree = computed(() => [{ id: 0, name: '根分类', children: rows.value }])

function defaultForm() {
  return { parentId: 0, code: '', name: '', sort: 0, status: 1 }
}

async function load() {
  loading.value = true
  try {
    rows.value = (await categoryApi.tree()) || []
  } finally {
    loading.value = false
  }
}

function openCreate(parentRow) {
  form.value = defaultForm()
  if (parentRow) form.value.parentId = parentRow.id
  dialogVisible.value = true
}

function openEdit(row) {
  form.value = {
    id: row.id,
    parentId: row.parentId === null ? 0 : row.parentId,
    code: row.code,
    name: row.name,
    sort: row.sort === null ? 0 : row.sort,
    status: row.status
  }
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      parentId: form.value.parentId === undefined || form.value.parentId === null ? 0 : form.value.parentId,
      code: form.value.code,
      name: form.value.name,
      sort: form.value.sort,
      status: form.value.status
    }
    if (form.value.id) {
      await categoryApi.update(form.value.id, payload)
    } else {
      await categoryApi.create(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function removeRow(row) {
  await ElMessageBox.confirm(`确认删除分类「${row.name}」？`, '提示', { type: 'warning' })
  await categoryApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
