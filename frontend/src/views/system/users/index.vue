<template>
  <div class="page">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-button v-permission="'system:user:create'" type="primary" :icon="Plus" @click="openCreate">
        新增用户
      </el-button>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="用户名 / 姓名 / 手机"
            style="width: 190px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="query.deptId" clearable placeholder="全部" style="width: 160px">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
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
        <el-table-column prop="username" label="用户名" width="140" show-overflow-tooltip />
        <el-table-column prop="realName" label="姓名" width="120" show-overflow-tooltip />
        <el-table-column label="部门" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ deptName(row.deptId) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:user:update'" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-permission="'system:user:reset'" link type="warning" @click="resetPassword(row)">
              重置密码
            </el-button>
            <el-button
              v-permission="'system:user:update'"
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button v-permission="'system:user:delete'" link type="danger" @click="removeRow(row)">
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="留空则使用默认密码 Erp@123456"
          />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-select v-model="form.deptId" clearable placeholder="请选择部门" style="width: 100%">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple clearable placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { deptApi, roleApi, userApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const depts = ref([])
const roles = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, keyword: '', deptId: undefined, status: undefined })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

function emptyForm() {
  return {
    username: '',
    password: '',
    realName: '',
    deptId: undefined,
    phone: '',
    email: '',
    status: 1,
    roleIds: [],
    remark: ''
  }
}

const form = ref(emptyForm())

const deptMap = computed(() => {
  const map = {}
  depts.value.forEach((d) => {
    map[d.id] = d.name
  })
  return map
})

function deptName(id) {
  return deptMap.value[id] || '-'
}

function buildParams() {
  return {
    page: query.page,
    size: query.size,
    keyword: query.keyword || undefined,
    deptId: query.deptId || undefined,
    status: typeof query.status === 'number' ? query.status : undefined
  }
}

async function load() {
  loading.value = true
  try {
    const data = await userApi.page(buildParams())
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
  query.deptId = undefined
  query.status = undefined
  search()
}

function openCreate() {
  form.value = emptyForm()
  dialogVisible.value = true
}

async function openEdit(row) {
  const data = await userApi.detail(row.id)
  form.value = { ...data.user, password: '', roleIds: data.roleIds || [] }
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      username: form.value.username,
      realName: form.value.realName,
      deptId: form.value.deptId || null,
      phone: form.value.phone,
      email: form.value.email,
      status: form.value.status,
      remark: form.value.remark,
      roleIds: form.value.roleIds || []
    }
    if (form.value.id) {
      await userApi.update(form.value.id, payload)
    } else {
      payload.password = form.value.password
      await userApi.create(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(
    `确认${next === 1 ? '启用' : '停用'}用户「${row.username}」？`,
    '提示',
    { type: 'warning' }
  )
  await userApi.updateStatus(row.id, next)
  ElMessage.success('操作成功')
  load()
}

async function resetPassword(row) {
  const result = await ElMessageBox.prompt(`请输入「${row.username}」的新密码`, '重置密码', {
    inputPattern: /^.{6,}$/,
    inputErrorMessage: '密码至少 6 位',
    inputValue: ''
  })
  await userApi.resetPassword(row.id, result.value)
  ElMessage.success('密码重置成功')
}

async function removeRow(row) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await userApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  const [deptList, roleList] = await Promise.all([deptApi.list(), roleApi.list()])
  depts.value = deptList || []
  roles.value = roleList || []
  load()
})
</script>
