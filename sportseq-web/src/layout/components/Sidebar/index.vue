<template>
  <div class="sidebar-logo">
    <h2>体育器材管理</h2>
  </div>
  <el-menu
    class="sidebar-menu"
    background-color="#001529"
    text-color="#a6adb4"
    active-text-color="#409eff"
    :default-active="route.path"
    router
  >
    <template v-for="item in menuRoutes" :key="item.path">
      <el-sub-menu v-if="item.children && item.children.length" :index="item.path">
        <template #title>
          <el-icon v-if="item.meta?.icon"><component :is="item.meta.icon" /></el-icon>
          <span>{{ item.meta?.title }}</span>
        </template>
        <el-menu-item
          v-for="child in item.children"
          :key="child.path"
          :index="child.path"
        >
          <span>{{ child.meta?.title }}</span>
        </el-menu-item>
      </el-sub-menu>
      <el-menu-item v-else :index="item.path">
        <el-icon v-if="item.meta?.icon"><component :is="item.meta.icon" /></el-icon>
        <span>{{ item.meta?.title }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { usePermissionStore } from '@/store/modules/permission'

const route = useRoute()
const permissionStore = usePermissionStore()

const menuRoutes = computed(() => permissionStore.routes)
</script>

<style scoped lang="scss">
.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;

  h2 {
    color: #fff;
    font-size: 16px;
  }
}

.sidebar-menu {
  border-right: none;
}
</style>
