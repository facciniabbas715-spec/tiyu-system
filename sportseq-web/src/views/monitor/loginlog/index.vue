<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.userName" placeholder="登录账号" clearable style="width: 160px" @keyup.enter="loadData" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px">
        <el-option label="成功" :value="1" />
        <el-option label="失败" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button v-permission="['monitor:loginlog:remove']" type="danger" plain @click="handleClean">清空</el-button>
    </div>

    <el-table :data="logList" border>
      <el-table-column prop="userName" label="账号" width="120" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ipaddr" label="IP" width="130" />
      <el-table-column prop="browser" label="浏览器" width="130" />
      <el-table-column prop="os" label="操作系统" width="140" />
      <el-table-column prop="msg" label="提示信息" min-width="180" />
      <el-table-column prop="loginTime" label="登录时间" width="170" />
    </el-table>

    <el-pagination
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 14px; justify-content: flex-end"
      @change="loadData"
    />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cleanLoginLog, getLoginLogPage, type LoginLogItem } from '@/api/monitor/loginlog'

const logList = ref<LoginLogItem[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, userName: '', status: undefined as number | undefined })

async function loadData() {
  const data = await getLoginLogPage(query)
  logList.value = data.records
  total.value = data.total
}

async function handleClean() {
  await ElMessageBox.confirm('确认清空全部登录日志？此操作不可恢复', '警告', { type: 'warning' })
  await cleanLoginLog()
  ElMessage.success('已清空')
  await loadData()
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
</style>
