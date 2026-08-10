<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.title" placeholder="模块标题" clearable style="width: 160px" @keyup.enter="loadData" />
      <el-input v-model="query.operName" placeholder="操作人" clearable style="width: 140px" @keyup.enter="loadData" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px">
        <el-option label="成功" :value="0" />
        <el-option label="异常" :value="1" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button v-permission="['monitor:operlog:remove']" type="danger" plain @click="handleClean">清空</el-button>
    </div>

    <el-table :data="logList" border>
      <el-table-column prop="title" label="模块" width="120" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">{{ businessTypeText(row.businessType) }}</template>
      </el-table-column>
      <el-table-column prop="operName" label="操作人" width="100" />
      <el-table-column prop="requestMethod" label="方式" width="80" />
      <el-table-column prop="operUrl" label="请求地址" min-width="160" show-overflow-tooltip />
      <el-table-column prop="operIp" label="IP" width="120" />
      <el-table-column prop="costTime" label="耗时(ms)" width="90" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '成功' : '异常' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="operTime" label="操作时间" width="170" />
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
import { cleanOperLog, getOperLogPage, type OperLogItem } from '@/api/monitor/operlog'

const logList = ref<OperLogItem[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, title: '', operName: '', status: undefined as number | undefined })

async function loadData() {
  const data = await getOperLogPage(query)
  logList.value = data.records
  total.value = data.total
}

function businessTypeText(type: number) {
  return ['其它', '新增', '修改', '删除', '审核', '导出', '导入', '领用', '归还'][type] ?? '其它'
}

async function handleClean() {
  await ElMessageBox.confirm('确认清空全部操作日志？此操作不可恢复', '警告', { type: 'warning' })
  await cleanOperLog()
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
