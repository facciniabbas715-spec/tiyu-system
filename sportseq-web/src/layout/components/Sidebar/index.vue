<template>
  <div class="sidebar-logo">
    <svg class="logo-mark" viewBox="0 0 32 32" aria-hidden="true">
      <defs>
        <linearGradient id="logo-gradient" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#518bdb" />
          <stop offset="1" stop-color="#36bab8" />
        </linearGradient>
      </defs>
      <rect width="32" height="32" rx="9" fill="url(#logo-gradient)" />
      <rect x="7.5" y="13.5" width="17" height="5" rx="2.5" fill="#fff" />
      <rect x="4" y="9" width="5.5" height="14" rx="2.75" fill="#fff" />
      <rect x="22.5" y="9" width="5.5" height="14" rx="2.75" fill="#fff" />
    </svg>
    <div class="logo-text">
      <span class="logo-title">体育器材管理</span>
      <span class="logo-sub">SPORTSEQ · 全生命周期</span>
    </div>
  </div>
  <el-menu
    class="sidebar-menu"
    background-color="transparent"
    text-color="#4a453f"
    active-text-color="#2e5aa0"
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
  gap: 10px;
  padding: 0 18px;

  .logo-mark {
    width: 32px;
    height: 32px;
    flex-shrink: 0;
  }

  .logo-text {
    display: flex;
    flex-direction: column;
    line-height: 1.2;
    min-width: 0;
  }

  .logo-title {
    font-size: 15px;
    font-weight: 700;
    color: #221f1c;
    white-space: nowrap;
  }

  .logo-sub {
    margin-top: 2px;
    font-size: 10px;
    letter-spacing: 0.08em;
    color: #8c8a88;
    white-space: nowrap;
  }
}

.sidebar-menu {
  border-right: none;
  padding: 8px 10px 16px;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 42px;
    line-height: 42px;
    border-radius: 8px;
    border: 1px solid transparent;
    margin: 2px 0;
    transition: background-color 0.15s ease, color 0.15s ease;
  }

  :deep(.el-menu-item:hover),
  :deep(.el-sub-menu__title:hover) {
    background-color: #f0eee9;
    color: #221f1c;
  }

  :deep(.el-menu-item.is-active) {
    background-color: #fff;
    border-color: #e6e3dd;
    box-shadow: 0 1px 2px rgba(34, 31, 28, 0.06);
    color: #2e5aa0;
    font-weight: 600;
  }

  :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
    color: #221f1c;
    font-weight: 600;
  }

  :deep(.el-sub-menu__icon-arrow) {
    color: #8c8a88;
  }
}
</style>
