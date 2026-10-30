import request from './request'
import { downloadBlob } from './request'

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  me: () => request.get('/auth/me'),
  logout: () => request.post('/auth/logout'),
  changePassword: (data) => request.post('/auth/password', data)
}

export const userApi = {
  page: (params) => request.get('/system/users', { params }),
  options: () => request.get('/system/users/options'),
  detail: (id) => request.get(`/system/users/${id}`),
  create: (data) => request.post('/system/users', data),
  update: (id, data) => request.put(`/system/users/${id}`, data),
  remove: (id) => request.delete(`/system/users/${id}`),
  resetPassword: (id, password) => request.put(`/system/users/${id}/password`, { password }),
  updateStatus: (id, status) => request.put(`/system/users/${id}/status`, null, { params: { status } })
}

export const roleApi = {
  page: (params) => request.get('/system/roles', { params }),
  list: () => request.get('/system/roles/list'),
  menus: (id) => request.get(`/system/roles/${id}/menus`),
  create: (data) => request.post('/system/roles', data),
  update: (id, data) => request.put(`/system/roles/${id}`, data),
  remove: (id) => request.delete(`/system/roles/${id}`),
  assignMenus: (id, menuIds) => request.put(`/system/roles/${id}/menus`, { menuIds })
}

export const menuApi = {
  tree: () => request.get('/system/menus/tree'),
  treeAll: () => request.get('/system/menus/tree-all'),
  list: () => request.get('/system/menus/list'),
  create: (data) => request.post('/system/menus', data),
  update: (id, data) => request.put(`/system/menus/${id}`, data),
  remove: (id) => request.delete(`/system/menus/${id}`)
}

export const deptApi = {
  tree: () => request.get('/system/depts/tree'),
  list: () => request.get('/system/depts/list'),
  create: (data) => request.post('/system/depts', data),
  update: (id, data) => request.put(`/system/depts/${id}`, data),
  remove: (id) => request.delete(`/system/depts/${id}`)
}

export const dictApi = {
  types: () => request.get('/system/dicts/types'),
  createType: (data) => request.post('/system/dicts/types', data),
  updateType: (id, data) => request.put(`/system/dicts/types/${id}`, data),
  removeType: (id) => request.delete(`/system/dicts/types/${id}`),
  items: (id) => request.get(`/system/dicts/types/${id}/items`),
  itemsByCode: (code) => request.get(`/system/dicts/types/code/${code}/items`),
  createItem: (data) => request.post('/system/dicts/items', data),
  updateItem: (id, data) => request.put(`/system/dicts/items/${id}`, data),
  removeItem: (id) => request.delete(`/system/dicts/items/${id}`)
}

export const configApi = {
  list: (groupCode) => request.get('/system/configs', { params: { groupCode } }),
  save: (data) => request.post('/system/configs', data),
  remove: (id) => request.delete(`/system/configs/${id}`)
}

export const logApi = {
  login: (params) => request.get('/system/logs/login', { params }),
  oper: (params) => request.get('/system/logs/oper', { params })
}

export const categoryApi = {
  tree: () => request.get('/masterdata/categories/tree'),
  page: (params) => request.get('/masterdata/categories', { params }),
  create: (data) => request.post('/masterdata/categories', data),
  update: (id, data) => request.put(`/masterdata/categories/${id}`, data),
  remove: (id) => request.delete(`/masterdata/categories/${id}`)
}

export const unitApi = {
  page: (params) => request.get('/masterdata/units', { params }),
  options: () => request.get('/masterdata/units/options'),
  create: (data) => request.post('/masterdata/units', data),
  update: (id, data) => request.put(`/masterdata/units/${id}`, data),
  remove: (id) => request.delete(`/masterdata/units/${id}`)
}

export const productApi = {
  page: (params) => request.get('/masterdata/products', { params }),
  options: () => request.get('/masterdata/products/options'),
  detail: (id) => request.get(`/masterdata/products/${id}`),
  create: (data) => request.post('/masterdata/products', data),
  update: (id, data) => request.put(`/masterdata/products/${id}`, data),
  remove: (id) => request.delete(`/masterdata/products/${id}`),
  exportCsv: async (params) => {
    const blob = await request.get('/masterdata/products/export', { params, responseType: 'blob' })
    downloadBlob(blob, 'products.csv')
  }
}

export const customerApi = {
  page: (params) => request.get('/masterdata/customers', { params }),
  options: () => request.get('/masterdata/customers/options'),
  detail: (id) => request.get(`/masterdata/customers/${id}`),
  contacts: (id) => request.get(`/masterdata/customers/${id}/contacts`),
  addContact: (id, data) => request.post(`/masterdata/customers/${id}/contacts`, data),
  removeContact: (id, contactId) => request.delete(`/masterdata/customers/${id}/contacts/${contactId}`),
  create: (data) => request.post('/masterdata/customers', data),
  update: (id, data) => request.put(`/masterdata/customers/${id}`, data),
  remove: (id) => request.delete(`/masterdata/customers/${id}`)
}

export const supplierApi = {
  page: (params) => request.get('/masterdata/suppliers', { params }),
  options: () => request.get('/masterdata/suppliers/options'),
  detail: (id) => request.get(`/masterdata/suppliers/${id}`),
  create: (data) => request.post('/masterdata/suppliers', data),
  update: (id, data) => request.put(`/masterdata/suppliers/${id}`, data),
  remove: (id) => request.delete(`/masterdata/suppliers/${id}`)
}

export const warehouseApi = {
  page: (params) => request.get('/masterdata/warehouses', { params }),
  options: () => request.get('/masterdata/warehouses/options'),
  locations: (id) => request.get(`/masterdata/warehouses/${id}/locations`),
  addLocation: (id, data) => request.post(`/masterdata/warehouses/${id}/locations`, data),
  removeLocation: (id, locationId) => request.delete(`/masterdata/warehouses/${id}/locations/${locationId}`),
  create: (data) => request.post('/masterdata/warehouses', data),
  update: (id, data) => request.put(`/masterdata/warehouses/${id}`, data),
  remove: (id) => request.delete(`/masterdata/warehouses/${id}`)
}

export const stockApi = {
  page: (params) => request.get('/inventory/stocks', { params }),
  warnings: () => request.get('/inventory/stocks/warnings'),
  replenishment: () => request.get('/inventory/replenishment'),
  flows: (params) => request.get('/inventory/flows', { params }),
  inbound: (data) => request.post('/inventory/inbound', data),
  outbound: (data) => request.post('/inventory/outbound', data),
  freeze: (data) => request.put('/inventory/stocks/freeze', data),
  release: (data) => request.put('/inventory/stocks/release', data)
}

export const transferApi = {
  page: (params) => request.get('/inventory/transfers', { params }),
  detail: (id) => request.get(`/inventory/transfers/${id}`),
  create: (data) => request.post('/inventory/transfers', data),
  confirm: (id) => request.put(`/inventory/transfers/${id}/confirm`),
  cancel: (id) => request.put(`/inventory/transfers/${id}/cancel`)
}

export const stocktakeApi = {
  page: (params) => request.get('/inventory/stocktakes', { params }),
  detail: (id) => request.get(`/inventory/stocktakes/${id}`),
  create: (data) => request.post('/inventory/stocktakes', data),
  updateActual: (id, itemId, actualQty) => request.put(`/inventory/stocktakes/${id}/items/${itemId}`, { actualQty }),
  submit: (id) => request.put(`/inventory/stocktakes/${id}/submit`),
  approve: (id) => request.put(`/inventory/stocktakes/${id}/approve`)
}

export const purchaseOrderApi = {
  page: (params) => request.get('/purchase/orders', { params }),
  detail: (id) => request.get(`/purchase/orders/${id}`),
  create: (data) => request.post('/purchase/orders', data),
  update: (id, data) => request.put(`/purchase/orders/${id}`, data),
  remove: (id) => request.delete(`/purchase/orders/${id}`),
  submit: (id) => request.put(`/purchase/orders/${id}/submit`),
  cancel: (id) => request.put(`/purchase/orders/${id}/cancel`)
}

export const purchaseReceiptApi = {
  page: (params) => request.get('/purchase/receipts', { params }),
  detail: (id) => request.get(`/purchase/receipts/${id}`),
  create: (data) => request.post('/purchase/receipts', data),
  confirm: (id) => request.put(`/purchase/receipts/${id}/confirm`)
}

export const salesOrderApi = {
  page: (params) => request.get('/sales/orders', { params }),
  detail: (id) => request.get(`/sales/orders/${id}`),
  create: (data) => request.post('/sales/orders', data),
  update: (id, data) => request.put(`/sales/orders/${id}`, data),
  remove: (id) => request.delete(`/sales/orders/${id}`),
  submit: (id) => request.put(`/sales/orders/${id}/submit`),
  cancel: (id) => request.put(`/sales/orders/${id}/cancel`)
}

export const salesShipmentApi = {
  page: (params) => request.get('/sales/shipments', { params }),
  detail: (id) => request.get(`/sales/shipments/${id}`),
  create: (data) => request.post('/sales/shipments', data),
  confirm: (id) => request.put(`/sales/shipments/${id}/confirm`)
}

export const accountApi = {
  page: (params) => request.get('/finance/accounts', { params }),
  options: () => request.get('/finance/accounts/options'),
  create: (data) => request.post('/finance/accounts', data),
  update: (id, data) => request.put(`/finance/accounts/${id}`, data),
  remove: (id) => request.delete(`/finance/accounts/${id}`)
}

export const receivableApi = {
  page: (params) => request.get('/finance/receivables', { params }),
  open: (customerId) => request.get('/finance/receivables/open', { params: { customerId } }),
  aging: () => request.get('/finance/receivables/aging')
}

export const payableApi = {
  page: (params) => request.get('/finance/payables', { params }),
  open: (supplierId) => request.get('/finance/payables/open', { params: { supplierId } }),
  aging: () => request.get('/finance/payables/aging')
}

export const receiptApi = {
  page: (params) => request.get('/finance/receipts', { params }),
  create: (data) => request.post('/finance/receipts', data),
  writeoff: (id, allocations) => request.put(`/finance/receipts/${id}/writeoff`, { allocations })
}

export const paymentApi = {
  page: (params) => request.get('/finance/payments', { params }),
  create: (data) => request.post('/finance/payments', data),
  writeoff: (id, allocations) => request.put(`/finance/payments/${id}/writeoff`, { allocations })
}

export const workflowApi = {
  processes: (params) => request.get('/workflow/processes', { params }),
  processDetail: (id) => request.get(`/workflow/processes/${id}`),
  createProcess: (data) => request.post('/workflow/processes', data),
  updateProcess: (id, data) => request.put(`/workflow/processes/${id}`, data),
  deleteProcess: (id) => request.delete(`/workflow/processes/${id}`),
  pendingTasks: () => request.get('/workflow/tasks/pending'),
  approve: (id, opinion) => request.post(`/workflow/tasks/${id}/approve`, { opinion }),
  reject: (id, opinion) => request.post(`/workflow/tasks/${id}/reject`, { opinion }),
  transferOptions: () => request.get('/workflow/tasks/transfer-options'),
  transfer: (id, targetUserId, reason) => request.post(`/workflow/tasks/${id}/transfer`, { targetUserId, reason }),
  instances: (params) => request.get('/workflow/instances', { params }),
  instanceDetail: (id) => request.get(`/workflow/instances/${id}`),
  cancel: (id, reason) => request.put(`/workflow/instances/${id}/cancel`, { reason })
}

export const crmApi = {
  leads: (params) => request.get('/crm/leads', { params }),
  createLead: (data) => request.post('/crm/leads', data),
  updateLead: (id, data) => request.put(`/crm/leads/${id}`, data),
  convertLead: (id) => request.put(`/crm/leads/${id}/convert`),
  deleteLead: (id) => request.delete(`/crm/leads/${id}`),
  opportunities: (params) => request.get('/crm/opportunities', { params }),
  createOpportunity: (data) => request.post('/crm/opportunities', data),
  updateOpportunity: (id, data) => request.put(`/crm/opportunities/${id}`, data),
  updateStage: (id, stage) => request.put(`/crm/opportunities/${id}/stage`, null, { params: { stage } }),
  deleteOpportunity: (id) => request.delete(`/crm/opportunities/${id}`),
  followUps: (params) => request.get('/crm/followups', { params }),
  addFollowUp: (data) => request.post('/crm/followups', data),
  visits: (params) => request.get('/crm/visits', { params }),
  createVisit: (data) => request.post('/crm/visits', data),
  updateVisitStatus: (id, status) => request.put(`/crm/visits/${id}/status`, null, { params: { status } })
}

export const reportApi = {
  dashboard: () => request.get('/report/dashboard'),
  sales: (params) => request.get('/report/sales', { params }),
  purchase: (params) => request.get('/report/purchase', { params }),
  inventory: (params) => request.get('/report/inventory', { params }),
  exportSales: async (params) => downloadBlob(await request.get('/report/sales/export', { params, responseType: 'blob' }), 'sales-report.csv'),
  exportPurchase: async (params) => downloadBlob(await request.get('/report/purchase/export', { params, responseType: 'blob' }), 'purchase-report.csv'),
  exportLowStock: async () => downloadBlob(await request.get('/report/inventory/export', { responseType: 'blob' }), 'low-stock.csv')
}
