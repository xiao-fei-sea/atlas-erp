<template>
  <div class="page">
    <div class="page-header">
      <h2>流程定义</h2>
      <div>
        <el-button v-permission="'workflow:process:create'" type="primary" :icon="Plus" @click="openCreate">
          新增流程
        </el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="业务类型">
          <el-select v-model="query.bizType" clearable placeholder="全部" style="width: 150px">
            <el-option v-for="b in bizTypeOptions" :key="b.value" :label="b.label" :value="b.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="code" label="流程编码" min-width="150" />
        <el-table-column prop="name" label="流程名称" min-width="180" />
        <el-table-column prop="bizType" label="业务类型" width="130" :formatter="bizTypeText" />
        <el-table-column prop="enabled" label="启用" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">
              {{ row.enabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'workflow:process:update'"
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-permission="'workflow:process:update'"
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

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑审批流程' : '新增审批流程'"
      width="1200px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="流程编码" prop="code">
              <el-input v-model="form.code" placeholder="如 PUR_ORDER_STD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="流程名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入流程名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务类型" prop="bizType">
              <el-select
                v-model="form.bizType"
                filterable
                allow-create
                default-first-option
                placeholder="请选择或输入"
                style="width: 100%"
              >
                <el-option v-for="b in bizTypeOptions" :key="b.value" :label="b.label" :value="b.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用">
              <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="node-header">
          <span>审批节点</span>
          <el-button size="small" :icon="Plus" @click="addNode">添加节点</el-button>
        </div>
        <el-table :data="form.nodes" border size="small" empty-text="暂无审批节点">
          <el-table-column label="序号" width="80">
            <template #default="{ row }">
              <el-input-number v-model="row.seq" :min="1" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="节点名称" min-width="130">
            <template #default="{ row }">
              <el-input v-model="row.name" placeholder="节点名称" />
            </template>
          </el-table-column>
          <el-table-column label="审批人类型" width="100">
            <template #default="{ row }">
              <el-select v-model="row.approverType" style="width: 100%" @change="row.approverIds = []">
                <el-option label="角色" value="ROLE" />
                <el-option label="用户" value="USER" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="审批人" min-width="150">
            <template #default="{ row }">
              <el-select
                v-model="row.approverIds"
                multiple
                filterable
                collapse-tags
                collapse-tags-tooltip
                placeholder="选择审批人"
                style="width: 100%"
              >
                <el-option
                  v-for="o in approverOptions(row)"
                  :key="o.id"
                  :label="o.name"
                  :value="o.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="会签方式" width="100">
            <template #default="{ row }">
              <el-select v-model="row.signType" style="width: 100%">
                <el-option label="或签" value="OR" />
                <el-option label="会签" value="AND" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="限定部门" width="130">
            <template #default="{ row }">
              <el-select v-model="row.deptId" clearable placeholder="不限" style="width: 100%">
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="超时(小时)" width="95">
            <template #default="{ row }">
              <el-input-number v-model="row.timeoutHours" :min="0" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="最小金额" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.minAmount" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="最大金额" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.maxAmount" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeNode($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="muted" style="margin-top: 6px">
          最大金额填 0 表示不限制；限定部门为空表示不限；超时小时填 0 表示不限；会签节点需全部审批人通过，或签节点一人通过即可。
        </div>
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
import { deptApi, roleApi, workflowApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, bizType: undefined })
const roleOptions = ref([])
const userOptions = ref([])
const deptOptions = ref([])

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  code: '',
  name: '',
  bizType: '',
  enabled: 1,
  remark: '',
  nodes: []
})

const bizTypeOptions = [
  { value: 'PUR_ORDER', label: '采购订单' },
  { value: 'SAL_ORDER', label: '销售订单' },
  { value: 'EXPENSE', label: '费用报销' }
]

const rules = {
  code: [{ required: true, message: '请输入流程编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  bizType: [{ required: true, message: '请选择业务类型', trigger: 'change' }]
}

function bizTypeText(row) {
  const item = bizTypeOptions.find((i) => i.value === row.bizType)
  return item ? item.label : row.bizType || '-'
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.bizType) params.bizType = query.bizType
    const data = await workflowApi.processes(params)
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
  query.bizType = undefined
  search()
}

function approverOptions(row) {
  return row.approverType === 'USER' ? userOptions.value : roleOptions.value
}

function addNode() {
  form.nodes.push({
    seq: form.nodes.length + 1,
    name: '',
    approverType: 'ROLE',
    approverIds: [],
    signType: 'OR',
    deptId: null,
    timeoutHours: 0,
    minAmount: 0,
    maxAmount: 0
  })
}

async function loadOptions() {
  const [roles, users, depts] = await Promise.all([
    roleApi.list(),
    workflowApi.transferOptions(),
    deptApi.list()
  ])
  roleOptions.value = roles || []
  userOptions.value = users || []
  deptOptions.value = depts || []
}

function removeNode(index) {
  form.nodes.splice(index, 1)
}

function openCreate() {
  Object.assign(form, {
    id: null,
    code: '',
    name: '',
    bizType: '',
    enabled: 1,
    remark: '',
    nodes: []
  })
  addNode()
  dialogVisible.value = true
}

async function openEdit(row) {
  const data = await workflowApi.processDetail(row.id)
  const process = data.process || {}
  Object.assign(form, {
    id: row.id,
    code: process.code,
    name: process.name,
    bizType: process.bizType,
    enabled: process.enabled === undefined ? 1 : process.enabled,
    remark: process.remark,
    nodes: (data.nodes || []).map((n) => ({
      seq: n.seq,
      name: n.name,
      approverType: n.approverType || 'ROLE',
      approverIds: n.approverIds
        ? String(n.approverIds).split(',').filter(Boolean).map(Number)
        : n.approverId
          ? [n.approverId]
          : [],
      signType: n.signType || 'OR',
      deptId: n.deptId || null,
      timeoutHours: Number(n.timeoutHours || 0),
      minAmount: Number(n.minAmount || 0),
      maxAmount: Number(n.maxAmount || 0)
    }))
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      code: form.code,
      name: form.name,
      bizType: form.bizType,
      enabled: form.enabled,
      remark: form.remark,
      nodes: form.nodes.map((n, index) => ({
        seq: n.seq || index + 1,
        name: n.name,
        approverType: n.approverType || 'ROLE',
        approverId: n.approverIds && n.approverIds.length ? n.approverIds[0] : null,
        approverIds: (n.approverIds || []).join(','),
        signType: n.signType || 'OR',
        deptId: n.deptId || null,
        timeoutHours: Number(n.timeoutHours || 0),
        minAmount: Number(n.minAmount || 0),
        maxAmount: Number(n.maxAmount || 0)
      }))
    }
    if (form.id) {
      await workflowApi.updateProcess(form.id, payload)
    } else {
      await workflowApi.createProcess(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function removeRow(row) {
  await ElMessageBox.confirm(`确认删除流程「${row.name}」？`, '提示', { type: 'warning' })
  await workflowApi.deleteProcess(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  load()
  loadOptions()
})
</script>

<style scoped>
.node-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 8px;
  font-weight: 600;
}
</style>
