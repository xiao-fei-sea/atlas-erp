<template>
  <crud-page
    title="计量单位"
    :api="unitApi"
    perm="masterdata:unit"
    :columns="columns"
    :fields="fields"
    :filters="filters"
    :default-form="defaultForm"
    :actions-width="140"
  />
</template>

<script setup>
import CrudPage from '@/components/CrudPage.vue'
import { unitApi } from '@/api'

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const columns = [
  { prop: 'code', label: '单位编码', width: 150 },
  { prop: 'name', label: '单位名称', minWidth: 180 },
  { prop: 'precisionNum', label: '精度', width: 100 },
  {
    prop: 'status',
    label: '状态',
    width: 90,
    formatter: (row) => (row.status === 1 ? '启用' : '停用')
  }
]

const fields = [
  { prop: 'code', label: '单位编码', required: true },
  { prop: 'name', label: '单位名称', required: true },
  { prop: 'precisionNum', label: '精度', type: 'number', precision: 0 },
  { prop: 'status', label: '状态', type: 'select', options: statusOptions, required: true }
]

const filters = [
  { prop: 'keyword', label: '关键词' },
  { prop: 'status', label: '状态', type: 'select', options: statusOptions }
]

const defaultForm = () => ({ code: '', name: '', precisionNum: 0, status: 1 })
</script>
