<template>
  <div class="login-page">
    <div class="login-shell">
      <aside class="login-story">
        <div class="brand-lockup">
          <span class="brand-mark">E</span>
          <span class="brand-copy">
            <strong>ERP Suite</strong>
            <small>企业运营平台</small>
          </span>
        </div>

        <div class="story-body">
          <p class="story-date">{{ todayLabel }}</p>
          <h1>把订单、库存与资金，收在同一张经营视图里。</h1>
          <p class="story-text">
            从销售接单到采购入库，从审批流转到财务核销，业务数据在同一条链路上持续沉淀。
          </p>
          <div class="feature-list">
            <div v-for="item in features" :key="item" class="feature-item">
              <span class="feature-dot"></span>
              <span>{{ item }}</span>
            </div>
          </div>
        </div>

        <div class="story-foot">
          <span>模块化单体</span>
          <span class="foot-line"></span>
          <span>v1.0</span>
        </div>
      </aside>

      <main class="login-panel">
        <div class="panel-head">
          <h2>登录工作台</h2>
          <p>使用企业账号继续处理今天的业务。</p>
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
        <p class="hint">请使用管理员分配的账号登录</p>
      </main>
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
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const todayLabel = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())
const features = ['订单与审批联动', '库存实时可追溯', '应收应付自动生成', '经营数据统一汇总']

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
  display: grid;
  min-height: 100vh;
  padding: 32px;
  place-items: center;
  background:
    radial-gradient(circle at 10% 10%, rgba(15, 118, 110, 0.16), transparent 32%),
    radial-gradient(circle at 90% 90%, rgba(176, 120, 47, 0.1), transparent 28%),
    linear-gradient(135deg, #eef5f1 0%, #f7f9f7 48%, #e9f0ed 100%);
}

.login-shell {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  width: min(960px, 100%);
  min-height: 570px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 32px 80px rgba(13, 43, 36, 0.18);
}

.login-story {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 42px 46px 34px;
  background: radial-gradient(circle at 16% 2%, #17464a 0%, #0b2028 50%, #07151b 100%);
  color: #eef7f3;
}

.login-story::before {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 46px 46px;
  content: '';
  mask-image: linear-gradient(145deg, #000000 0%, transparent 72%);
}

.login-story::after {
  position: absolute;
  right: -90px;
  bottom: -110px;
  width: 300px;
  height: 300px;
  border: 1px solid rgba(96, 199, 178, 0.2);
  border-radius: 50%;
  box-shadow:
    0 0 0 42px rgba(96, 199, 178, 0.04),
    0 0 0 86px rgba(96, 199, 178, 0.025);
  content: '';
}

.brand-lockup,
.story-body,
.story-foot {
  position: relative;
  z-index: 1;
}

.brand-lockup {
  display: flex;
  align-items: center;
  gap: 11px;
}

.brand-mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 11px;
  background: linear-gradient(135deg, #31ad9c, #0d675e);
  box-shadow: 0 10px 24px rgba(10, 100, 92, 0.36);
  color: #ffffff;
  font-size: 17px;
  font-weight: 700;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  line-height: 1;
}

.brand-copy strong {
  color: #ffffff;
  font-size: 16px;
  font-weight: 650;
  letter-spacing: 0.3px;
}

.brand-copy small {
  color: #8daaa2;
  font-size: 11px;
  letter-spacing: 1px;
}

.story-body {
  max-width: 430px;
  margin: auto 0;
}

.story-date {
  margin: 0 0 18px;
  color: #77b5a7;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.6px;
}

.story-body h1 {
  margin: 0;
  color: #f3fbf8;
  font-size: clamp(30px, 3.1vw, 42px);
  font-weight: 650;
  letter-spacing: -1px;
  line-height: 1.32;
}

.story-text {
  max-width: 390px;
  margin: 20px 0 0;
  color: #9eb7b0;
  font-size: 14px;
  line-height: 1.8;
}

.feature-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 20px;
  margin-top: 30px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #c7d9d3;
  font-size: 12px;
}

.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #5cc6b0;
  box-shadow: 0 0 0 3px rgba(92, 198, 176, 0.1);
}

.story-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6f9087;
  font-size: 11px;
  letter-spacing: 0.4px;
}

.foot-line {
  width: 24px;
  height: 1px;
  background: #3e5f58;
}

.login-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px clamp(34px, 4.5vw, 64px);
  background: #ffffff;
}

.panel-head {
  margin-bottom: 26px;
}

.panel-head h2 {
  margin: 0;
  color: #182923;
  font-size: 25px;
  font-weight: 650;
  letter-spacing: -0.5px;
}

.panel-head p {
  margin: 9px 0 0;
  color: #80918b;
  font-size: 13px;
}

.login-btn {
  width: 100%;
  height: 46px;
  margin-top: 4px;
  font-size: 14px;
  letter-spacing: 1px;
}

.hint {
  margin: 22px 0 0;
  padding-top: 18px;
  border-top: 1px solid #edf2f0;
  color: #9aaaa4;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 820px) {
  .login-page {
    padding: 18px;
  }

  .login-shell {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .login-story {
    padding: 28px 30px;
  }

  .story-body {
    margin: 34px 0 24px;
  }

  .story-body h1 {
    font-size: 30px;
  }

  .feature-list {
    display: none;
  }

  .story-foot {
    display: none;
  }

  .login-panel {
    padding: 34px 30px 38px;
  }
}
</style>
