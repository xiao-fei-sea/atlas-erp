<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <h1>ERP 企业管理系统</h1>
        <p>销售 / 采购 / 库存 / 财务 / 审批 一体化</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="submit">登 录</el-button>
      </el-form>
      <p class="hint">默认管理员：admin / Admin@123</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'Admin@123' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await store.login(form)
    await store.loadMenus()
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 50%, #0ea5e9 100%);
}

.login-card {
  width: 380px;
  background: #fff;
  border-radius: 12px;
  padding: 36px 32px 24px;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.25);
}

.brand {
  text-align: center;
  margin-bottom: 26px;
}

.brand h1 {
  margin: 0;
  font-size: 22px;
  color: #1e293b;
}

.brand p {
  margin: 8px 0 0;
  color: #94a3b8;
  font-size: 13px;
}

.login-btn {
  width: 100%;
}

.hint {
  margin-top: 16px;
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
}
</style>
