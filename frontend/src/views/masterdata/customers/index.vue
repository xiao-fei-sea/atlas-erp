<template>
  <crud-page
    title="客户管理"
    :api="customerApi"
    perm="masterdata:customer"
    :columns="columns"
    :fields="fields"
    :filters="filters"
    :default-form="defaultForm"
    :actions-width="200"
  >
    <template #actions="{ row }">
      <el-button
        v-permission="'masterdata:customer:view'"
        link
        type="primary"
        @click="openContacts(row)"
      >
        联系人
      </el-button>
    </template>
  </crud-page>

  <el-dialog v-model="contactVisible" :title="contactTitle" width="760px">
    <el-table v-loading="contactLoading" :data="contacts" border stripe>
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="position" label="职位" width="120" />
      <el-table-column prop="phone" label="电话" width="150" />
      <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'masterdata:customer:update'"
            link
            type="danger"
            @click="removeContact(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-form :inline="true" :model="contactForm" style="margin-top: 12px">
      <el-form-item label="姓名">
        <el-input v-model="contactForm.name" placeholder="必填" style="width: 120px" />
      </el-form-item>
      <el-form-item label="职位">
        <el-input v-model="contactForm.position" style="width: 120px" />
      </el-form-item>
      <el-form-item label="电话">
        <el-input v-model="contactForm.phone" style="width: 140px" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="contactForm.email" style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button
          v-permission="'masterdata:customer:update'"
          type="primary"
          :loading="addingContact"
          @click="addContact"
        >
          添加
        </el-button>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="contactVisible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CrudPage from '@/components/CrudPage.vue'
import { customerApi } from '@/api'

const levelOptions = [
  { label: 'A 级', value: 'A' },
  { label: 'B 级', value: 'B' },
  { label: 'C 级', value: 'C' }
]
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const columns = [
  { prop: 'code', label: '客户编码', width: 130 },
  { prop: 'name', label: '客户名称', minWidth: 180 },
  { prop: 'level', label: '级别', width: 80 },
  { prop: 'contactName', label: '联系人', width: 110 },
  { prop: 'phone', label: '电话', width: 140 },
  { prop: 'email', label: '邮箱', minWidth: 170 },
  {
    prop: 'creditLimit',
    label: '信用额度',
    width: 120,
    formatter: (row) => Number(row.creditLimit || 0).toFixed(2)
  },
  {
    prop: 'status',
    label: '状态',
    width: 90,
    formatter: (row) => (row.status === 1 ? '启用' : '停用')
  }
]

const fields = [
  { prop: 'code', label: '客户编码', required: true },
  { prop: 'name', label: '客户名称', required: true },
  { prop: 'level', label: '客户级别', type: 'select', options: levelOptions },
  { prop: 'contactName', label: '联系人' },
  { prop: 'phone', label: '电话' },
  { prop: 'email', label: '邮箱' },
  { prop: 'taxNo', label: '税号' },
  { prop: 'address', label: '地址' },
  { prop: 'creditLimit', label: '信用额度', type: 'number', precision: 2 },
  { prop: 'status', label: '状态', type: 'select', options: statusOptions },
  { prop: 'remark', label: '备注', type: 'textarea' }
]

const filters = [
  { prop: 'keyword', label: '关键词' },
  { prop: 'level', label: '级别', type: 'select', options: levelOptions },
  { prop: 'status', label: '状态', type: 'select', options: statusOptions }
]

const defaultForm = () => ({
  code: '',
  name: '',
  level: 'C',
  contactName: '',
  phone: '',
  email: '',
  taxNo: '',
  address: '',
  creditLimit: 0,
  status: 1,
  remark: ''
})

const contactVisible = ref(false)
const contactLoading = ref(false)
const addingContact = ref(false)
const contacts = ref([])
const currentCustomer = ref({})
const contactForm = reactive({ name: '', position: '', phone: '', email: '' })

const contactTitle = computed(() => `联系人 - ${currentCustomer.value.name || ''}`)

async function loadContacts() {
  if (!currentCustomer.value.id) return
  contactLoading.value = true
  try {
    contacts.value = (await customerApi.contacts(currentCustomer.value.id)) || []
  } finally {
    contactLoading.value = false
  }
}

function openContacts(row) {
  currentCustomer.value = row
  contactForm.name = ''
  contactForm.position = ''
  contactForm.phone = ''
  contactForm.email = ''
  contactVisible.value = true
  loadContacts()
}

async function addContact() {
  if (!contactForm.name) {
    ElMessage.warning('请输入联系人姓名')
    return
  }
  addingContact.value = true
  try {
    await customerApi.addContact(currentCustomer.value.id, {
      name: contactForm.name,
      position: contactForm.position,
      phone: contactForm.phone,
      email: contactForm.email
    })
    ElMessage.success('添加成功')
    contactForm.name = ''
    contactForm.position = ''
    contactForm.phone = ''
    contactForm.email = ''
    loadContacts()
  } finally {
    addingContact.value = false
  }
}

async function removeContact(row) {
  await ElMessageBox.confirm(`确认删除联系人「${row.name}」？`, '提示', { type: 'warning' })
  await customerApi.removeContact(currentCustomer.value.id, row.id)
  ElMessage.success('删除成功')
  loadContacts()
}
</script>
