<template>
  <crud-page
    title="供应商管理"
    :api="supplierApi"
    perm="masterdata:supplier"
    :columns="columns"
    :fields="fields"
    :filters="filters"
    :default-form="defaultForm"
    :actions-width="140"
  />
</template>

<script setup>
import CrudPage from '@/components/CrudPage.vue'
import { supplierApi } from '@/api'

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const columns = [
  { prop: 'code', label: '供应商编码', width: 140 },
  { prop: 'name', label: '供应商名称', minWidth: 180 },
  { prop: 'contactName', label: '联系人', width: 110 },
  { prop: 'phone', label: '电话', width: 140 },
  { prop: 'email', label: '邮箱', minWidth: 170 },
  { prop: 'taxNo', label: '税号', width: 160 },
  {
    prop: 'status',
    label: '状态',
    width: 90,
    formatter: (row) => (row.status === 1 ? '启用' : '停用')
  }
]

const fields = [
  { prop: 'code', label: '供应商编码', required: true },
  { prop: 'name', label: '供应商名称', required: true },
  { prop: 'contactName', label: '联系人' },
  { prop: 'phone', label: '电话' },
  { prop: 'email', label: '邮箱' },
  { prop: 'taxNo', label: '税号' },
  { prop: 'address', label: '地址' },
  { prop: 'status', label: '状态', type: 'select', options: statusOptions },
  { prop: 'remark', label: '备注', type: 'textarea' }
]

const filters = [
  { prop: 'keyword', label: '关键词' },
  { prop: 'status', label: '状态', type: 'select', options: statusOptions }
]

const defaultForm = () => ({
  code: '',
  name: '',
  contactName: '',
  phone: '',
  email: '',
  taxNo: '',
  address: '',
  status: 1,
  remark: ''
})
</script>
