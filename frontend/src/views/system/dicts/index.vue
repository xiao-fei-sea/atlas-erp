<template>
  <div class="page">
    <div class="page-header">
      <h2>字典管理</h2>
      <el-button v-permission="'system:dict:create'" type="primary" :icon="Plus" @click="openTypeCreate">
        新增字典类型
      </el-button>
    </div>

    <el-row :gutter="12">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>字典类型</template>
          <el-table
            v-loading="typeLoading"
            :data="types"
            row-key="id"
            :current-row-key="currentType ? currentType.id : undefined"
            border
            stripe
            highlight-current-row
            @row-click="selectType"
          >
            <el-table-column prop="code" label="编码" min-width="150" show-overflow-tooltip />
            <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-permission="'system:dict:update'"
                  link
                  type="primary"
                  @click.stop="openTypeEdit(row)"
                >
                  编辑
                </el-button>
                <el-button
                  v-permission="'system:dict:delete'"
                  link
                  type="danger"
                  @click.stop="removeType(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="items-header">
              <span>{{ currentType ? '字典项：' + currentType.name : '字典项' }}</span>
              <el-button
                v-if="currentType"
                v-permission="'system:dict:create'"
                type="primary"
                :icon="Plus"
                @click="openItemCreate"
              >
                新增字典项
              </el-button>
            </div>
          </template>
          <el-empty v-if="!currentType" description="请选择左侧字典类型" />
          <template v-else>
            <el-table v-loading="itemLoading" :data="items" border stripe>
              <el-table-column prop="itemLabel" label="标签" min-width="140" show-overflow-tooltip />
              <el-table-column prop="itemValue" label="值" min-width="140" show-overflow-tooltip />
              <el-table-column prop="sort" label="排序" width="80" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'">
                    {{ row.status === 1 ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-permission="'system:dict:update'"
                    link
                    type="primary"
                    @click="openItemEdit(row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-permission="'system:dict:delete'"
                    link
                    type="danger"
                    @click="removeItem(row)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="typeDialogVisible"
      :title="typeForm.id ? '编辑字典类型' : '新增字典类型'"
      width="520px"
    >
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="typeForm.code" placeholder="如 order_status" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="typeForm.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="typeForm.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="typeDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitType">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="itemDialogVisible"
      :title="itemForm.id ? '编辑字典项' : '新增字典项'"
      width="520px"
    >
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="90px">
        <el-form-item label="标签" prop="itemLabel">
          <el-input v-model="itemForm.itemLabel" placeholder="请输入标签" />
        </el-form-item>
        <el-form-item label="值" prop="itemValue">
          <el-input v-model="itemForm.itemValue" placeholder="请输入值" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="itemForm.sort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="itemForm.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="itemDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitItem">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { dictApi } from '@/api'

const typeLoading = ref(false)
const itemLoading = ref(false)
const saving = ref(false)
const types = ref([])
const items = ref([])
const currentType = ref(null)

const typeDialogVisible = ref(false)
const itemDialogVisible = ref(false)
const typeFormRef = ref()
const itemFormRef = ref()

const typeForm = ref({ code: '', name: '', status: 1 })
const itemForm = ref({ itemLabel: '', itemValue: '', sort: 0, status: 1 })

const typeRules = {
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const itemRules = {
  itemLabel: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  itemValue: [{ required: true, message: '请输入值', trigger: 'blur' }]
}

async function loadTypes() {
  typeLoading.value = true
  try {
    types.value = (await dictApi.types()) || []
  } finally {
    typeLoading.value = false
  }
}

async function loadItems(typeId) {
  itemLoading.value = true
  try {
    items.value = (await dictApi.items(typeId)) || []
  } finally {
    itemLoading.value = false
  }
}

function selectType(row) {
  currentType.value = row
  loadItems(row.id)
}

function openTypeCreate() {
  typeForm.value = { code: '', name: '', status: 1 }
  typeDialogVisible.value = true
}

function openTypeEdit(row) {
  typeForm.value = { id: row.id, code: row.code, name: row.name, status: row.status }
  typeDialogVisible.value = true
}

async function submitType() {
  await typeFormRef.value.validate()
  saving.value = true
  try {
    const payload = {
      code: typeForm.value.code,
      name: typeForm.value.name,
      status: typeForm.value.status
    }
    if (typeForm.value.id) {
      await dictApi.updateType(typeForm.value.id, payload)
    } else {
      await dictApi.createType(payload)
    }
    ElMessage.success('保存成功')
    typeDialogVisible.value = false
    await loadTypes()
    if (currentType.value) {
      const found = types.value.find((t) => t.id === currentType.value.id)
      if (found) currentType.value = found
    }
  } finally {
    saving.value = false
  }
}

async function removeType(row) {
  await ElMessageBox.confirm(`确认删除字典类型「${row.name}」及其字典项？`, '提示', {
    type: 'warning'
  })
  await dictApi.removeType(row.id)
  ElMessage.success('删除成功')
  if (currentType.value && currentType.value.id === row.id) {
    currentType.value = null
    items.value = []
  }
  loadTypes()
}

function openItemCreate() {
  itemForm.value = { itemLabel: '', itemValue: '', sort: 0, status: 1 }
  itemDialogVisible.value = true
}

function openItemEdit(row) {
  itemForm.value = {
    id: row.id,
    itemLabel: row.itemLabel,
    itemValue: row.itemValue,
    sort: row.sort || 0,
    status: row.status
  }
  itemDialogVisible.value = true
}

async function submitItem() {
  await itemFormRef.value.validate()
  saving.value = true
  try {
    const payload = {
      dictTypeId: currentType.value.id,
      itemLabel: itemForm.value.itemLabel,
      itemValue: itemForm.value.itemValue,
      sort: itemForm.value.sort,
      status: itemForm.value.status
    }
    if (itemForm.value.id) {
      await dictApi.updateItem(itemForm.value.id, payload)
    } else {
      await dictApi.createItem(payload)
    }
    ElMessage.success('保存成功')
    itemDialogVisible.value = false
    loadItems(currentType.value.id)
  } finally {
    saving.value = false
  }
}

async function removeItem(row) {
  await ElMessageBox.confirm(`确认删除字典项「${row.itemLabel}」？`, '提示', { type: 'warning' })
  await dictApi.removeItem(row.id)
  ElMessage.success('删除成功')
  loadItems(currentType.value.id)
}

onMounted(async () => {
  await loadTypes()
  if (types.value.length) {
    selectType(types.value[0])
  }
})
</script>

<style scoped>
.items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
