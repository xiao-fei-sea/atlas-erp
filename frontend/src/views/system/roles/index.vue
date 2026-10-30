<template>
  <div class="page">
    <div class="page-header">
      <h2>角色管理</h2>
      <el-button v-permission="'system:role:create'" type="primary" :icon="Plus" @click="openCreate">
        新增角色
      </el-button>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="角色编码 / 名称"
            style="width: 200px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="code" label="角色编码" width="160" show-overflow-tooltip />
        <el-table-column prop="name" label="名称" width="180" show-overflow-tooltip />
        <el-table-column label="数据范围" width="150">
          <template #default="{ row }">{{ scopeLabel(row.dataScope) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:role:update'" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-permission="'system:role:delete'" link type="danger" @click="removeRow(row)">
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
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" placeholder="如 WAREHOUSE" />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option
              v-for="item in scopeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单权限" prop="menuIds">
          <div class="menu-tree">
            <el-tree
              ref="menuTreeRef"
              :data="menuTree"
              show-checkbox
              node-key="id"
              :props="{ label: 'name', children: 'children' }"
              default-expand-all
            />
          </div>
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
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { menuApi, roleApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const menuTree = ref([])
const dialogVisible = ref(false)
const formRef = ref()
const menuTreeRef = ref()

const query = reactive({ page: 1, size: 10, keyword: '', status: undefined })

const scopeOptions = [
  { value: 'ALL', label: '全部数据' },
  { value: 'DEPT_AND_CHILD', label: '本部门及下级' },
  { value: 'DEPT', label: '本部门' },
  { value: 'SELF', label: '仅本人' }
]

const rules = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

function emptyForm() {
  return {
    code: '',
    name: '',
    dataScope: 'ALL',
    status: 1,
    remark: ''
  }
}

function collectLeafIds(nodes, set) {
  nodes.forEach((node) => {
    if (!node.children || node.children.length === 0) {
      set.add(node.id)
    } else {
      collectLeafIds(node.children, set)
    }
  })
}

const form = ref(emptyForm())

function scopeLabel(value) {
  const item = scopeOptions.find((s) => s.value === value)
  return item ? item.label : value || '-'
}

async function load() {
  loading.value = true
  try {
    const data = await roleApi.page({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: typeof query.status === 'number' ? query.status : undefined
    })
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
  form.value = emptyForm()
  dialogVisible.value = true
  nextTick(() => {
    if (menuTreeRef.value) menuTreeRef.value.setCheckedKeys([])
  })
}

async function openEdit(row) {
  form.value = { ...row }
  dialogVisible.value = true
  await nextTick()
  const menuIds = await roleApi.menus(row.id)
  const leafIds = new Set()
  collectLeafIds(menuTree.value, leafIds)
  const checked = (menuIds || []).filter((id) => leafIds.has(id))
  if (menuTreeRef.value) menuTreeRef.value.setCheckedKeys(checked)
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const menuIds = [
      ...menuTreeRef.value.getCheckedKeys(),
      ...menuTreeRef.value.getHalfCheckedKeys()
    ]
    const payload = {
      code: form.value.code,
      name: form.value.name,
      dataScope: form.value.dataScope,
      status: form.value.status,
      remark: form.value.remark,
      menuIds
    }
    if (form.value.id) {
      await roleApi.update(form.value.id, payload)
    } else {
      await roleApi.create(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function removeRow(row) {
  await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, '提示', { type: 'warning' })
  await roleApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  menuTree.value = (await menuApi.treeAll()) || []
  load()
})
</script>

<style scoped>
.menu-tree {
  width: 100%;
  max-height: 300px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 6px;
}
</style>
