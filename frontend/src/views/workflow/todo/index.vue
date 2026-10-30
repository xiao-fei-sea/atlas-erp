<template>
  <div class="page">
    <div class="page-header">
      <h2>我的待办</h2>
      <div>
        <el-button v-permission="'workflow:task:view'" :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="id" label="任务ID" width="90" />
        <el-table-column prop="nodeName" label="审批节点" min-width="140" />
        <el-table-column label="会签方式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.signType === 'AND' ? 'warning' : 'info'" size="small">
              {{ row.signType === 'AND' ? '会签' : '或签' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bizType" label="业务类型" width="120" :formatter="bizTypeText" />
        <el-table-column prop="bizNo" label="业务单号" min-width="160" />
        <el-table-column prop="submitterName" label="提交人" width="120" />
        <el-table-column label="截止时间" width="180">
          <template #default="{ row }">
            <template v-if="row.dueAt">
              <span>{{ row.dueAt }}</span>
              <el-tag v-if="row.overdue === 1" type="danger" size="small" style="margin-left: 6px">已超时</el-tag>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.overdue === 1 ? 'danger' : 'warning'">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'workflow:task:view'"
              link
              type="primary"
              @click="handle(row, 'approve')"
            >
              通过
            </el-button>
            <el-button
              v-permission="'workflow:task:view'"
              link
              type="danger"
              @click="handle(row, 'reject')"
            >
              驳回
            </el-button>
            <el-button
              v-permission="'workflow:task:transfer'"
              link
              type="warning"
              @click="openTransfer(row)"
            >
              转交
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="muted">共 {{ rows.length }} 条待办任务</div>
    </el-card>

    <el-dialog v-model="transferVisible" title="转交审批任务" width="460px">
      <el-form label-width="90px">
        <el-form-item label="当前任务">
          <span>{{ transferRow.nodeName }}（{{ transferRow.bizNo }}）</span>
        </el-form-item>
        <el-form-item label="转交给" required>
          <el-select v-model="transferForm.targetUserId" filterable placeholder="请选择用户" style="width: 100%">
            <el-option
              v-for="u in transferOptions"
              :key="u.id"
              :label="u.name"
              :value="u.id"
              :disabled="u.id === currentUserId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="转交说明">
          <el-input v-model="transferForm.reason" type="textarea" :rows="3" placeholder="请输入转交说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="transferVisible = false">取消</el-button>
          <el-button type="primary" :loading="transferSaving" @click="submitTransfer">确定转交</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { workflowApi } from '@/api'
import { useUserStore } from '@/store/user'

const loading = ref(false)
const rows = ref([])
const userStore = useUserStore()
const currentUserId = computed(() => userStore.user.id)

const transferVisible = ref(false)
const transferSaving = ref(false)
const transferRow = ref({})
const transferOptions = ref([])
const transferForm = ref({ targetUserId: null, reason: '' })

const bizTypeMap = {
  PUR_ORDER: '采购订单',
  SAL_ORDER: '销售订单',
  EXPENSE: '费用报销'
}

const statusMap = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已取消'
}

function bizTypeText(row) {
  return bizTypeMap[row.bizType] || row.bizType || '-'
}

function statusText(status) {
  return statusMap[status] || status || '-'
}

async function load() {
  loading.value = true
  try {
    rows.value = (await workflowApi.pendingTasks()) || []
  } finally {
    loading.value = false
  }
}

async function handle(row, action) {
  const approve = action === 'approve'
  try {
    const { value } = await ElMessageBox.prompt(
      `确认${approve ? '通过' : '驳回'}任务「${row.nodeName || row.id}」？`,
      approve ? '审批通过' : '审批驳回',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '请输入审批意见（可选）',
        inputValidator: () => true,
        type: 'warning'
      }
    )
    if (approve) {
      await workflowApi.approve(row.id, value)
    } else {
      await workflowApi.reject(row.id, value)
    }
    ElMessage.success(approve ? '审批通过成功' : '审批驳回成功')
    load()
  } catch {
    // 用户取消操作
  }
}

async function openTransfer(row) {
  transferRow.value = row
  transferForm.value = { targetUserId: null, reason: '' }
  transferOptions.value = (await workflowApi.transferOptions()) || []
  transferVisible.value = true
}

async function submitTransfer() {
  if (!transferForm.value.targetUserId) {
    ElMessage.warning('请选择转交目标用户')
    return
  }
  transferSaving.value = true
  try {
    await workflowApi.transfer(transferRow.value.id, transferForm.value.targetUserId, transferForm.value.reason)
    ElMessage.success('转交成功')
    transferVisible.value = false
    load()
  } finally {
    transferSaving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.muted {
  margin-top: 8px;
}
</style>
