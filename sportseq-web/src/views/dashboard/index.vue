<template>
  <div class="dashboard">
    <h1>欢迎使用体育器材管理系统</h1>
    <p>数据仪表盘将在统计分析阶段（阶段 10）实现</p>
    <p>当前用户：{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</p>
    <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

async function handleLogout() {
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped lang="scss">
.dashboard {
  padding: 40px;
  text-align: center;
}
</style>
