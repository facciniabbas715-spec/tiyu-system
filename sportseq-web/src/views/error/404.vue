<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped lang="scss">
.not-found {
  text-align: center;
  padding-top: 120px;
}

h1 {
  font-size: 72px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: 0.02em;
  background: linear-gradient(90deg, #518bdb, #36bab8);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

p {
  margin: 14px 0 28px;
  font-size: 15px;
  color: #797267;
}
</style>
