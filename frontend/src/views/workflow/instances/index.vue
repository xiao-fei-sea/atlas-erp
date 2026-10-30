<template>
  <div class="page">
    <div class="page-header">
      <h2>审批实例</h2>
      <div>
        <el-button v-permission="'workflow:instance:view'" :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 150px">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
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
        <el-table-column prop="id" label="实例ID" width="90" />
        <el-table-column prop="bizType" label="业务类型" width="120" :formatter="bizTypeText" />
        <el-table-column prop="bizNo" label="业务单号" min-width="160" />
        <el-table-column prop="amount" label="金额" width="130" align="right" :formatter="amountText" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentSeq" label="当前节点" width="100" />
        <el-table-column prop="submitterId" label="提交人" width="100" />
        <el-table-column prop="createdAt" label="提交时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'workflow:instance:view'"
              link
              type="primary"
              @click="openDetail(row)"
            >
              详情
            </el-button>
            <el-button
              v-if="row.status === 'RUNNING'"
              v-permission="'workflow:instance:view'"
              link
              type="warning"
              @click="cancelInstance(row)"
            >
              撤回
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

    <el-dialog v-model="detailVisible" title="审批实例详情" width="820px">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="实例ID">{{ detail.instance.id }}</el-descriptions-item>
            <el-descriptions-item label="流程名称">{{ detail.processName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="业务类型">{{ bizTypeText(detail.instance) }}</el-descriptions-item>
            <el-descriptions-item label="业务单号">{{ detail.instance.bizNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="金额">{{ money(detail.instance.amount) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagType(detail.instance.status)">
                {{ statusText(detail.instance.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="当前节点">{{ detail.instance.currentSeq }}</el-descriptions-item>
            <el-descriptions-item label="提交人ID">{{ detail.instance.submitterId }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ detail.instance.createdAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ detail.instance.finishedAt || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="审批任务" name="tasks">
          <el-table :data="detail.tasks" border stripe>
            <el-table-column prop="nodeSeq" label="节点序号" width="90" />
            <el-table-column prop="nodeName" label="审批节点" min-width="140" />
            <el-table-column label="会签方式" width="100">
              <template #default="{ row }">
                {{ row.signType === 'AND' ? '会签' : '或签' }}
              </template>
            </el-table-column>
            <el-table-column prop="approverType" label="审批人类型" width="110" />
            <el-table-column prop="approverRole" label="审批角色" width="110" />
            <el-table-column prop="approverId" label="审批人ID" width="100" />
            <el-table-column label="原审批人" width="100">
              <template #default="{ row }">{{ row.originApproverId || '-' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column label="超时" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.overdue === 1" type="danger" size="small">已超时</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="dueAt" label="截止时间" width="170" />
            <el-table-column prop="opinion" label="审批意见" min-width="160" />
            <el-table-column prop="actedAt" label="处理时间" width="170" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="审批历史" name="history">
          <el-table :data="detail.history" border stripe>
            <el-table-column prop="nodeSeq" label="节点序号" width="90" />
            <el-table-column prop="action" label="动作" width="120" :formatter="actionText" />
            <el-table-column prop="operatorName" label="操作人" width="120" />
            <el-table-column prop="opinion" label="意见" min-width="200" />
            <el-table-column prop="createdAt" label="操作时间" width="170" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { workflowApi } from '@/api'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, status: undefined, bizType: undefined })

const detailVisible = ref(false)
const activeTab = ref('basic')
const detail = ref({ instance: {}, tasks: [], history: [], processName: '' })

const statusOptions = [
  { value: 'RUNNING', label: '审批中' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'CANCELLED', label: '已撤回' }
]

const bizTypeOptions = [
  { value: 'PUR_ORDER', label: '采购订单' },
  { value: 'SAL_ORDER', label: '销售订单' },
  { value: 'EXPENSE', label: '费用报销' }
]

const actionMap = {
  SUBMIT: '提交审批',
  APPROVE: '审批通过',
  REJECT: '审批驳回',
  CANCEL: '撤回',
  TRANSFER: '转交',
  AUTO_APPROVE: '自动通过'
}

function bizTypeText(row) {
  const item = bizTypeOptions.find((i) => i.value === row.bizType)
  return item ? item.label : row.bizType || '-'
}

function statusText(status) {
  const item = statusOptions.find((i) => i.value === status)
  return item ? item.label : status || '-'
}

function statusTagType(status) {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'CANCELLED') return 'info'
  return 'warning'
}

function actionText(row) {
  return actionMap[row.action] || row.action || '-'
}

function money(value) {
  return '¥ ' + Number(value || 0).toFixed(2)
}

function amountText(row) {
  return money(row.amount)
}

function buildParams() {
  const params = { page: query.page, size: query.size }
  if (query.status) params.status = query.status
  if (query.bizType) params.bizType = query.bizType
  return params
}

async function load() {
  loading.value = true
  try {
    const data = await workflowApi.instances(buildParams())
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
  query.status = undefined
  query.bizType = undefined
  search()
}

async function openDetail(row) {
  const data = await workflowApi.instanceDetail(row.id)
  detail.value = {
    instance: data.instance || {},
    tasks: data.tasks || [],
    history: data.history || [],
    processName: data.processName || ''
  }
  activeTab.value = 'basic'
  detailVisible.value = true
}

async function cancelInstance(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      `确认撤回业务单号「${row.bizNo}」的审批？`,
      '撤回审批',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '请输入撤回原因（可选）',
        inputValidator: () => true,
        type: 'warning'
      }
    )
    await workflowApi.cancel(row.id, value)
    ElMessage.success('撤回成功')
    load()
  } catch {
    // 用户取消操作
  }
}

onMounted(load)
</script>
