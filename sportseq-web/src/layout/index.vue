<template>
  <el-container class="app-wrapper">
    <el-aside width="220px" class="sidebar">
      <Sidebar />
    </el-aside>
    <el-container>
      <el-header class="navbar">
        <Navbar />
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import Sidebar from './components/Sidebar/index.vue'
import Navbar from './components/Navbar/index.vue'
</script>

<style scoped lang="scss">
.app-wrapper {
  height: 100vh;
}

.sidebar {
  background: #001529;
  overflow-x: hidden;
}

.navbar {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.app-main {
  background: #f0f2f5;
}
</style>
