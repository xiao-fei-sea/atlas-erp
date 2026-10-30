import { expect, test } from '@playwright/test'

test.describe('ERP 前端端到端', () => {
  test('管理员登录后可访问主要业务模块并加载真实数据', async ({ page }) => {
    const pageErrors = []
    page.on('pageerror', (error) => pageErrors.push(error.message))

    await page.goto('/')
    await expect(page).toHaveURL(/\/login/)

    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.getByRole('button', { name: '登 录' }).click()

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 20_000 })
    await expect(page.getByRole('heading', { name: '经营仪表盘' })).toBeVisible()
    await expect(page.locator('.stat-card').first()).toBeVisible()
    await expect(page.locator('.el-table__row').first()).toBeVisible()

    await page.goto('/masterdata/products')
    await expect(page.getByRole('heading', { name: '商品管理' })).toBeVisible()
    await expect(page.locator('.el-table__row').first()).toBeVisible()

    await page.goto('/inventory/stocks')
    await expect(page.getByRole('heading', { name: '库存查询' })).toBeVisible()
    await expect(page.locator('.el-table__row').first()).toBeVisible()

    await page.goto('/purchase/orders')
    await expect(page.getByRole('heading', { name: '采购订单' })).toBeVisible()

    await page.goto('/sales/orders')
    await expect(page.getByRole('heading', { name: '销售订单' })).toBeVisible()

    await page.goto('/workflow/todo')
    await expect(page.getByRole('heading', { name: '我的待办' })).toBeVisible()

    await page.goto('/report/sales')
    await expect(page.getByRole('heading', { name: '销售报表' })).toBeVisible()
    await expect(page.locator('.el-table__row').first()).toBeVisible()

    expect(pageErrors, `页面脚本错误: ${pageErrors.join(' | ')}`).toHaveLength(0)
  })

  test('未登录访问受保护页面应跳转登录页', async ({ page }) => {
    await page.goto('/sales/orders')
    await expect(page).toHaveURL(/\/login/)
  })

  test('错误密码应提示登录失败且停留在登录页', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('bad-password')
    await page.getByRole('button', { name: '登 录' }).click()
    await expect(page.locator('.el-message')).toContainText('用户名或密码错误')
    await expect(page).toHaveURL(/\/login/)
  })
})
