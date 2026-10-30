<template>
  <div class="page">
    <div class="page-header">
      <h2>补货建议</h2>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-card shadow="never">
      <el-alert
        title="以下商品库存低于安全库存，建议及时补货"
        type="warning"
        :closable="false"
        show-icon
      />

      <el-table v-loading="loading" :data="rows" border stripe style="margin-top: 12px">
        <el-table-column prop="productCode" label="商品编码" width="130" />
        <el-table-column prop="productName" label="商品名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="productSpec" label="规格" width="120" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="130" />
        <el-table-column prop="batchNo" label="批次" width="120" />
        <el-table-column label="在手数量" width="110" align="right">
          <template #default="{ row }">{{ Number(row.qtyOnHand || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="可用数量" width="110" align="right">
          <template #default="{ row }">{{ Number(row.availableQty || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="安全库存" width="110" align="right">
          <template #default="{ row }">{{ Number(row.safetyStock || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="建议补货量" width="120" align="right">
          <template #default="{ row }">
            <span class="suggest">{{ Number(row.suggestQty || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { productApi, stockApi } from '@/api'

const loading = ref(false)
const rows = ref([])

async function load() {
  loading.value = true
  try {
    const list = (await stockApi.replenishment()) || []
    const safetyMap = {}
    try {
      const products = await productApi.page({ page: 1, size: 1000 })
      ;(products.records || []).forEach((p) => {
        safetyMap[p.id] = Number(p.safetyStock || 0)
      })
    } catch {
      // 缺少商品查看权限时，安全库存按 0 展示
    }
    rows.value = list.map((item) => {
      const onHand = Number(item.qtyOnHand || 0)
      const safety = safetyMap[item.productId] || 0
      return { ...item, safetyStock: safety, suggestQty: Math.max(safety - onHand, 0) }
    })
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.suggest {
  color: #e6a23c;
  font-weight: 600;
}
</style>
