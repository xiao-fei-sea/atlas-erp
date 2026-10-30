<template>
  <div class="page">
    <CrudPage
      title="资金账户"
      :api="accountApi"
      perm="finance:account"
      :columns="columns"
      :fields="fields"
      :filters="filters"
      :default-form="defaultForm"
    />
  </div>
</template>

<script setup>
import CrudPage from '@/components/CrudPage.vue'
import { accountApi } from '@/api'

const typeOptions = [
  { label: '现金', value: 'CASH' },
  { label: '银行', value: 'BANK' }
]

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

function typeText(value) {
  const option = typeOptions.find((item) => item.value === value)
  return option ? option.label : value || '-'
}

function statusText(value) {
  return Number(value) === 1 ? '启用' : '停用'
}

const columns = [
  { prop: 'code', label: '账户编码', width: 150 },
  { prop: 'name', label: '账户名称', minWidth: 160 },
  { prop: 'type', label: '账户类型', width: 120, formatter: (row) => typeText(row.type) },
  {
    prop: 'balance',
    label: '账户余额',
    width: 150,
    formatter: (row) => Number(row.balance || 0).toFixed(2)
  },
  { prop: 'status', label: '状态', width: 100, formatter: (row) => statusText(row.status) }
]

const fields = [
  { prop: 'code', label: '账户编码', required: true },
  { prop: 'name', label: '账户名称', required: true },
  { prop: 'type', label: '账户类型', type: 'select', required: true, options: typeOptions },
  { prop: 'balance', label: '账户余额', type: 'number', precision: 2 },
  { prop: 'status', label: '状态', type: 'select', options: statusOptions }
]

const filters = [{ prop: 'keyword', label: '关键字' }]

function defaultForm() {
  return { code: '', name: '', type: 'BANK', balance: 0, status: 1 }
}
</script>

<style scoped>
.page :deep(.page) {
  padding: 0;
}
</style>
