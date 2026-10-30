<template>
  <el-sub-menu v-if="menus.length" :index="String(item.id)">
    <template #title>
      <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
      <span>{{ item.name }}</span>
    </template>
    <sidebar-item v-for="child in menus" :key="child.id" :item="child" />
  </el-sub-menu>
  <el-menu-item v-else :index="item.path">
    <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
    <template #title>
      <span>{{ item.name }}</span>
    </template>
  </el-menu-item>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  item: { type: Object, required: true }
})

const menus = computed(() =>
  (props.item.children || []).filter((c) => c.status === 1 && c.type !== 'BUTTON')
)
</script>
