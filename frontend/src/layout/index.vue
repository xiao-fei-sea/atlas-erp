<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '72px' : '232px'" class="aside">
      <div class="logo" :class="{ 'logo-collapsed': collapsed }">
        <div class="logo-mark">E</div>
        <div v-if="!collapsed" class="logo-copy">
          <strong>ERP Suite</strong>
          <span>企业运营平台</span>
        </div>
      </div>
      <el-scrollbar class="aside-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="transparent"
          text-color="#aec3bc"
          active-text-color="#ffffff"
          router
        >
          <sidebar-item v-for="item in visibleMenus" :key="item.id" :item="item" />
        </el-menu>
      </el-scrollbar>
      <div v-if="!collapsed" class="aside-foot">
        <span class="status-dot"></span>
        <span>服务运行中</span>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <span class="header-divider"></span>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <span class="header-date">{{ todayLabel }}</span>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-info">
              <span class="user-avatar">{{ userInitial }}</span>
              <span class="user-meta">
                <span class="user-name">{{ user.realName || user.username || '未登录' }}</span>
                <span class="user-role">{{ roleLabel }}</span>
              </span>
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="roles" disabled>
                  {{ (user.roles || []).join(' / ') || '无角色' }}
                </el-dropdown-item>
                <el-dropdown-item command="password" divided>修改密码</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </el-main>
    </el-container>

    <el-dialog v-model="passwordVisible" title="修改密码" width="420px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="passwordVisible = false">取消</el-button>
          <el-button type="primary" @click="submitPassword">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { authApi } from '@/api'
import SidebarItem from './SidebarItem.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const collapsed = ref(false)

const user = computed(() => store.user)
const visibleMenus = computed(() =>
  (store.menuTree || []).filter((m) => m.status === 1 && m.type !== 'BUTTON' && m.visible !== 0)
)
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')
const userInitial = computed(() => {
  const name = user.value.realName || user.value.username || 'E'
  return name.slice(0, 1).toUpperCase()
})
const roleNames = {
  ADMIN: '系统管理员',
  SALES_MANAGER: '销售经理',
  SALES: '销售专员',
  PURCHASE_MANAGER: '采购经理',
  PURCHASE: '采购专员',
  WAREHOUSE: '仓库管理员',
  FINANCE: '财务专员'
}
const roleLabel = computed(() => roleNames[(user.value.roles || [])[0]] || '业务用户')
const todayLabel = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())

const passwordVisible = ref(false)
const passwordFormRef = ref()
const passwordForm = reactive({ oldPassword: '', newPassword: '' })
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

async function handleCommand(command) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    await store.logout()
    router.push('/login')
  } else if (command === 'password') {
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordVisible.value = true
  }
}

async function submitPassword() {
  await passwordFormRef.value.validate()
  await authApi.changePassword(passwordForm)
  ElMessage.success('密码修改成功，请重新登录')
  passwordVisible.value = false
  await store.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
  background: var(--erp-canvas);
}

.aside {
  z-index: 10;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: radial-gradient(circle at 12% 0%, #18383f 0%, #0d2028 46%, #08151a 100%);
  box-shadow: 16px 0 42px rgba(8, 27, 33, 0.13);
  transition: width 0.24s ease;
}

.logo {
  display: flex;
  flex: 0 0 64px;
  align-items: center;
  gap: 11px;
  padding: 0 18px;
  overflow: hidden;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-collapsed {
  justify-content: center;
  padding: 0;
}

.logo-mark {
  display: grid;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 10px;
  background: linear-gradient(135deg, #2ca596 0%, #0d655d 100%);
  box-shadow: 0 8px 18px rgba(11, 93, 87, 0.35);
  color: #ffffff;
  font-size: 16px;
  font-weight: 700;
}

.logo-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  color: #ffffff;
  line-height: 1.15;
}

.logo-copy strong {
  font-size: 15px;
  font-weight: 650;
  letter-spacing: 0.2px;
}

.logo-copy span {
  margin-top: 5px;
  color: #8eaaa2;
  font-size: 11px;
  letter-spacing: 0.8px;
}

.aside-scroll {
  flex: 1;
  min-height: 0;
}

.aside-scroll :deep(.el-scrollbar__view) {
  padding: 10px 0 12px;
}

.aside :deep(.el-menu) {
  border-right: 0;
  background: transparent;
}

.aside :deep(.el-menu-item),
.aside :deep(.el-sub-menu__title) {
  height: 44px;
  margin: 3px 10px;
  border-radius: 9px;
  color: #aec3bc;
  font-size: 13px;
  line-height: 44px;
}

.aside :deep(.el-menu-item:hover),
.aside :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #ffffff;
}

.aside :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(63, 181, 161, 0.2), rgba(63, 181, 161, 0.06));
  box-shadow: inset 3px 0 0 #5fc5b0;
  color: #ffffff;
  font-weight: 550;
}

.aside :deep(.el-sub-menu .el-menu-item) {
  min-width: auto;
  padding-left: 48px !important;
}

.aside :deep(.el-menu--collapse .el-menu-item),
.aside :deep(.el-menu--collapse .el-sub-menu__title) {
  margin: 3px 8px;
}

.aside-foot {
  display: flex;
  flex: 0 0 50px;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  color: #8eaaa2;
  font-size: 12px;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #58c6ad;
  box-shadow: 0 0 0 3px rgba(88, 198, 173, 0.12);
}

.header {
  position: relative;
  z-index: 9;
  display: flex;
  height: 64px;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid rgba(24, 58, 48, 0.08);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 1px 0 rgba(24, 58, 48, 0.02);
  backdrop-filter: blur(14px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn {
  cursor: pointer;
  color: #4d625b;
  font-size: 18px;
  transition: color 0.16s ease;
}

.collapse-btn:hover {
  color: var(--erp-jade);
}

.header-divider {
  width: 1px;
  height: 18px;
  background: #dce6e2;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.header-date {
  color: #80918b;
  font-size: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 5px 9px 5px 5px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.16s ease;
}

.user-info:hover {
  background: #f1f7f4;
}

.user-avatar {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 10px;
  background: linear-gradient(135deg, #1a9186, #0a5d57);
  box-shadow: 0 6px 14px rgba(15, 118, 110, 0.2);
  color: #ffffff;
  font-size: 13px;
  font-weight: 650;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.15;
}

.user-name {
  color: #263832;
  font-size: 13px;
  font-weight: 550;
}

.user-role {
  color: #879790;
  font-size: 11px;
}

.user-arrow {
  color: #92a29c;
  font-size: 12px;
}

.main {
  padding: 0;
  overflow: auto;
  background:
    radial-gradient(circle at 96% 2%, rgba(15, 118, 110, 0.06), transparent 30%),
    transparent;
}

@media (max-width: 760px) {
  .header {
    padding: 0 16px;
  }

  .header-date,
  .header-divider {
    display: none;
  }

  .user-meta {
    display: none;
  }
}
</style>
