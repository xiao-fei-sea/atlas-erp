<template>
  <div class="page">
    <div class="page-header">
      <h2>商品管理</h2>
      <div>
        <el-button
          v-permission="'masterdata:product:export'"
          :icon="Download"
          :loading="exporting"
          @click="handleExport"
        >
          导出
        </el-button>
        <el-button
          v-permission="'masterdata:product:create'"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          新增
        </el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="编码/名称/条码"
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-tree-select
            v-model="query.categoryId"
            :data="categoryTree"
            :props="treeProps"
            check-strictly
            clearable
            placeholder="全部"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="code" label="编码" width="130" />
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="分类" width="120">
          <template #default="{ row }">{{ categoryNames[row.categoryId] || '-' }}</template>
        </el-table-column>
        <el-table-column prop="spec" label="规格" width="120" show-overflow-tooltip />
        <el-table-column label="单位" width="90">
          <template #default="{ row }">{{ unitNames[row.unitId] || '-' }}</template>
        </el-table-column>
        <el-table-column label="税率" width="90" align="right">
          <template #default="{ row }">{{ Number(row.taxRate || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="采购价" width="110" align="right">
          <template #default="{ row }">{{ Number(row.purchasePrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="销售价" width="110" align="right">
          <template #default="{ row }">{{ Number(row.salePrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="安全库存" width="100" align="right">
          <template #default="{ row }">{{ Number(row.safetyStock || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'masterdata:product:update'"
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-permission="'masterdata:product:delete'"
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="商品编码" prop="code">
              <el-input v-model="form.code" placeholder="请输入商品编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入商品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品分类" prop="categoryId">
              <el-tree-select
                v-model="form.categoryId"
                :data="categoryTree"
                :props="treeProps"
                check-strictly
                clearable
                placeholder="请选择分类"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格" prop="spec">
              <el-input v-model="form.spec" placeholder="请输入规格" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计量单位" prop="unitId">
              <el-select v-model="form.unitId" clearable placeholder="请选择单位" style="width: 100%">
                <el-option v-for="o in unitOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="条码" prop="barcode">
              <el-input v-model="form.barcode" placeholder="请输入条码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税率" prop="taxRate">
              <el-input-number v-model="form.taxRate" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="采购价" prop="purchasePrice">
              <el-input-number v-model="form.purchasePrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="销售价" prop="salePrice">
              <el-input-number v-model="form.salePrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安全库存" prop="safetyStock">
              <el-input-number v-model="form.safetyStock" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { Download, Plus } from '@element-plus/icons-vue'
import { categoryApi, productApi, unitApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const exporting = ref(false)
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref()
const form = ref({})

const categoryTree = ref([])
const categoryNames = ref({})
const unitOptions = ref([])
const unitNames = ref({})

const treeProps = { label: 'name', value: 'id', children: 'children' }
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const query = reactive({ page: 1, size: 10, keyword: '', categoryId: undefined, status: undefined })

const rules = {
  code: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }]
}

const FORM_FIELDS = [
  'code',
  'name',
  'categoryId',
  'spec',
  'unitId',
  'barcode',
  'taxRate',
  'purchasePrice',
  'salePrice',
  'safetyStock',
  'status',
  'remark'
]

function defaultForm() {
  return {
    code: '',
    name: '',
    categoryId: undefined,
    spec: '',
    unitId: undefined,
    barcode: '',
    taxRate: 0,
    purchasePrice: 0,
    salePrice: 0,
    safetyStock: 0,
    status: 1,
    remark: ''
  }
}

function buildParams() {
  const params = { page: query.page, size: query.size }
  if (query.keyword) params.keyword = query.keyword
  if (query.categoryId) params.categoryId = query.categoryId
  if (query.status !== undefined && query.status !== null && query.status !== '') {
    params.status = query.status
  }
  return params
}

function buildPayload() {
  const payload = {}
  FORM_FIELDS.forEach((key) => {
    payload[key] = form.value[key]
  })
  return payload
}

function collectCategoryNames(nodes, map) {
  nodes.forEach((node) => {
    map[node.id] = node.name
    if (node.children && node.children.length) collectCategoryNames(node.children, map)
  })
}

async function loadMeta() {
  const [tree, units] = await Promise.all([categoryApi.tree(), unitApi.options()])
  categoryTree.value = tree || []
  categoryNames.value = {}
  collectCategoryNames(categoryTree.value, categoryNames.value)
  unitOptions.value = (units || []).map((u) => ({ label: u.name, value: u.id }))
  unitNames.value = {}
  ;(units || []).forEach((u) => {
    unitNames.value[u.id] = u.name
  })
}

async function load() {
  loading.value = true
  try {
    const data = await productApi.page(buildParams())
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
  query.categoryId = undefined
  query.status = undefined
  search()
}

function openCreate() {
  form.value = defaultForm()
  dialogVisible.value = true
}

function openEdit(row) {
  form.value = { ...defaultForm(), ...row }
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.value.id) {
      await productApi.update(form.value.id, buildPayload())
    } else {
      await productApi.create(buildPayload())
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function removeRow(row) {
  await ElMessageBox.confirm(`确认删除「${row.name || row.code}」？`, '提示', { type: 'warning' })
  await productApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

async function handleExport() {
  exporting.value = true
  try {
    await productApi.exportCsv(buildParams())
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadMeta()
  load()
})
</script>
