<template>
  <div class="navbar">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item>首页</el-breadcrumb-item>
      <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
    </el-breadcrumb>
    <el-dropdown @command="handleCommand">
      <span class="user-chip">
        <el-avatar :size="28" class="user-avatar">{{ userInitial }}</el-avatar>
        <span class="user-name">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
        <el-icon class="user-arrow"><ArrowDown /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="logout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const userInitial = computed(() => {
  const name = userStore.userInfo?.realName || userStore.userInfo?.username || 'U'
  return name.trim().charAt(0).toUpperCase()
})

async function handleCommand(command: string) {
  if (command === 'logout') {
    await userStore.logout()
    permissionStore.reset()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px 4px 5px;
  border: 1px solid transparent;
  border-radius: 999px;
  cursor: pointer;
  transition: background-color 0.18s ease, border-color 0.18s ease;
}

.user-chip:hover {
  background-color: #f5f5f5;
  border-color: #ece9e4;
}

.user-avatar {
  background-color: #ecf1f9;
  color: #2e5aa0;
  font-size: 13px;
  font-weight: 600;
}

.user-name {
  color: #221f1c;
  font-size: 14px;
  font-weight: 500;
}

.user-arrow {
  color: #8c8a88;
  font-size: 12px;
}

.el-dropdown-menu__item .el-icon {
  margin-right: 6px;
  vertical-align: -2px;
}
</style>
