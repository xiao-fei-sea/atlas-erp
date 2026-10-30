<template>
  <crud-page
    title="仓库管理"
    :api="warehouseApi"
    perm="masterdata:warehouse"
    :columns="columns"
    :fields="fields"
    :filters="filters"
    :default-form="defaultForm"
    :actions-width="200"
  >
    <template #actions="{ row }">
      <el-button
        v-permission="'masterdata:warehouse:view'"
        link
        type="primary"
        @click="openLocations(row)"
      >
        库位
      </el-button>
    </template>
  </crud-page>

  <el-dialog v-model="locationVisible" :title="locationTitle" width="720px">
    <el-table v-loading="locationLoading" :data="locations" border stripe>
      <el-table-column prop="code" label="库位编码" width="160" />
      <el-table-column prop="name" label="库位名称" min-width="180" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'masterdata:warehouse:update'"
            link
            type="danger"
            @click="removeLocation(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-form :inline="true" :model="locationForm" style="margin-top: 12px">
      <el-form-item label="库位编码">
        <el-input v-model="locationForm.code" placeholder="必填" style="width: 160px" />
      </el-form-item>
      <el-form-item label="库位名称">
        <el-input v-model="locationForm.name" style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button
          v-permission="'masterdata:warehouse:update'"
          type="primary"
          :loading="addingLocation"
          @click="addLocation"
        >
          添加
        </el-button>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="locationVisible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CrudPage from '@/components/CrudPage.vue'
import { warehouseApi } from '@/api'

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const columns = [
  { prop: 'code', label: '仓库编码', width: 140 },
  { prop: 'name', label: '仓库名称', minWidth: 180 },
  { prop: 'address', label: '地址', minWidth: 200 },
  { prop: 'manager', label: '负责人', width: 110 },
  {
    prop: 'status',
    label: '状态',
    width: 90,
    formatter: (row) => (row.status === 1 ? '启用' : '停用')
  }
]

const fields = [
  { prop: 'code', label: '仓库编码', required: true },
  { prop: 'name', label: '仓库名称', required: true },
  { prop: 'address', label: '地址' },
  { prop: 'manager', label: '负责人' },
  { prop: 'status', label: '状态', type: 'select', options: statusOptions }
]

const filters = [
  { prop: 'keyword', label: '关键词' },
  { prop: 'status', label: '状态', type: 'select', options: statusOptions }
]

const defaultForm = () => ({ code: '', name: '', address: '', manager: '', status: 1 })

const locationVisible = ref(false)
const locationLoading = ref(false)
const addingLocation = ref(false)
const locations = ref([])
const currentWarehouse = ref({})
const locationForm = reactive({ code: '', name: '' })

const locationTitle = computed(() => `库位 - ${currentWarehouse.value.name || ''}`)

async function loadLocations() {
  if (!currentWarehouse.value.id) return
  locationLoading.value = true
  try {
    locations.value = (await warehouseApi.locations(currentWarehouse.value.id)) || []
  } finally {
    locationLoading.value = false
  }
}

function openLocations(row) {
  currentWarehouse.value = row
  locationForm.code = ''
  locationForm.name = ''
  locationVisible.value = true
  loadLocations()
}

async function addLocation() {
  if (!locationForm.code) {
    ElMessage.warning('请输入库位编码')
    return
  }
  addingLocation.value = true
  try {
    await warehouseApi.addLocation(currentWarehouse.value.id, {
      code: locationForm.code,
      name: locationForm.name,
      status: 1
    })
    ElMessage.success('添加成功')
    locationForm.code = ''
    locationForm.name = ''
    loadLocations()
  } finally {
    addingLocation.value = false
  }
}

async function removeLocation(row) {
  await ElMessageBox.confirm(`确认删除库位「${row.code}」？`, '提示', { type: 'warning' })
  await warehouseApi.removeLocation(currentWarehouse.value.id, row.id)
  ElMessage.success('删除成功')
  loadLocations()
}
</script>
