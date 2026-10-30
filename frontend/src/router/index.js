import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '仪表盘' } },
      { path: 'system/users', component: () => import('@/views/system/users/index.vue'), meta: { title: '用户管理' } },
      { path: 'system/roles', component: () => import('@/views/system/roles/index.vue'), meta: { title: '角色管理' } },
      { path: 'system/menus', component: () => import('@/views/system/menus/index.vue'), meta: { title: '菜单管理' } },
      { path: 'system/depts', component: () => import('@/views/system/depts/index.vue'), meta: { title: '部门管理' } },
      { path: 'system/dicts', component: () => import('@/views/system/dicts/index.vue'), meta: { title: '字典管理' } },
      { path: 'system/configs', component: () => import('@/views/system/configs/index.vue'), meta: { title: '参数配置' } },
      { path: 'system/logs', component: () => import('@/views/system/logs/index.vue'), meta: { title: '日志管理' } },
      { path: 'masterdata/products', component: () => import('@/views/masterdata/products/index.vue'), meta: { title: '商品管理' } },
      { path: 'masterdata/categories', component: () => import('@/views/masterdata/categories/index.vue'), meta: { title: '商品分类' } },
      { path: 'masterdata/units', component: () => import('@/views/masterdata/units/index.vue'), meta: { title: '计量单位' } },
      { path: 'masterdata/customers', component: () => import('@/views/masterdata/customers/index.vue'), meta: { title: '客户管理' } },
      { path: 'masterdata/suppliers', component: () => import('@/views/masterdata/suppliers/index.vue'), meta: { title: '供应商管理' } },
      { path: 'masterdata/warehouses', component: () => import('@/views/masterdata/warehouses/index.vue'), meta: { title: '仓库管理' } },
      { path: 'inventory/stocks', component: () => import('@/views/inventory/stocks/index.vue'), meta: { title: '库存查询' } },
      { path: 'inventory/flows', component: () => import('@/views/inventory/flows/index.vue'), meta: { title: '库存流水' } },
      { path: 'inventory/transfers', component: () => import('@/views/inventory/transfers/index.vue'), meta: { title: '库存调拨' } },
      { path: 'inventory/stocktakes', component: () => import('@/views/inventory/stocktakes/index.vue'), meta: { title: '库存盘点' } },
      { path: 'inventory/replenishment', component: () => import('@/views/inventory/replenishment/index.vue'), meta: { title: '补货建议' } },
      { path: 'purchase/orders', component: () => import('@/views/purchase/orders/index.vue'), meta: { title: '采购订单' } },
      { path: 'purchase/receipts', component: () => import('@/views/purchase/receipts/index.vue'), meta: { title: '收货入库' } },
      { path: 'sales/orders', component: () => import('@/views/sales/orders/index.vue'), meta: { title: '销售订单' } },
      { path: 'sales/shipments', component: () => import('@/views/sales/shipments/index.vue'), meta: { title: '发货出库' } },
      { path: 'finance/receivables', component: () => import('@/views/finance/receivables/index.vue'), meta: { title: '应收账款' } },
      { path: 'finance/payables', component: () => import('@/views/finance/payables/index.vue'), meta: { title: '应付账款' } },
      { path: 'finance/receipts', component: () => import('@/views/finance/receipts/index.vue'), meta: { title: '收款管理' } },
      { path: 'finance/payments', component: () => import('@/views/finance/payments/index.vue'), meta: { title: '付款管理' } },
      { path: 'finance/accounts', component: () => import('@/views/finance/accounts/index.vue'), meta: { title: '资金账户' } },
      { path: 'workflow/todo', component: () => import('@/views/workflow/todo/index.vue'), meta: { title: '我的待办' } },
      { path: 'workflow/instances', component: () => import('@/views/workflow/instances/index.vue'), meta: { title: '审批实例' } },
      { path: 'workflow/processes', component: () => import('@/views/workflow/processes/index.vue'), meta: { title: '流程定义' } },
      { path: 'crm/leads', component: () => import('@/views/crm/leads/index.vue'), meta: { title: '客户线索' } },
      { path: 'crm/opportunities', component: () => import('@/views/crm/opportunities/index.vue'), meta: { title: '商机管理' } },
      { path: 'crm/followups', component: () => import('@/views/crm/followups/index.vue'), meta: { title: '跟进记录' } },
      { path: 'report/sales', component: () => import('@/views/report/sales/index.vue'), meta: { title: '销售报表' } },
      { path: 'report/purchase', component: () => import('@/views/report/purchase/index.vue'), meta: { title: '采购报表' } },
      { path: 'report/inventory', component: () => import('@/views/report/inventory/index.vue'), meta: { title: '库存报表' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  if (to.meta.public) return true
  const token = getToken()
  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  const store = useUserStore()
  if (!store.loaded) {
    try {
      if (!store.user || !store.user.permissions) {
        await store.fetchMe()
      }
      await store.loadMenus()
    } catch {
      await store.logout()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
  return true
})

export default router
