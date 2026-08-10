<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.configKey" placeholder="参数键" clearable style="width: 180px" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button v-permission="['system:config:add']" type="success" @click="openDialog()">新增参数</el-button>
    </div>

    <el-table :data="configList" border>
      <el-table-column prop="configName" label="参数名称" width="160" />
      <el-table-column prop="configKey" label="参数键" width="180" />
      <el-table-column prop="configValue" label="参数值" min-width="160" />
      <el-table-column label="内置" width="70">
        <template #default="{ row }">
          <el-tag :type="row.configType === 'Y' ? 'warning' : 'info'">{{ row.configType === 'Y' ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="['system:config:edit']" link type="primary" @click="openDialog(row)">修改</el-button>
          <el-button
            v-permission="['system:config:remove']"
            link
            type="danger"
            :disabled="row.configType === 'Y'"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 14px; justify-content: flex-end"
      @change="loadData"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改参数' : '新增参数'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="参数名称">
          <el-input v-model="form.configName" />
        </el-form-item>
        <el-form-item label="参数键">
          <el-input v-model="form.configKey" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="参数值">
          <el-input v-model="form.configValue" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addConfig,
  deleteConfig,
  getConfigPage,
  updateConfig,
  type ConfigDTO,
  type ConfigItem,
} from '@/api/system/config'

const configList = ref<ConfigItem[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, configKey: '' })
const dialogVisible = ref(false)
const form = reactive<ConfigDTO>({ configName: '', configKey: '', configValue: '' })

async function loadData() {
  const data = await getConfigPage(query)
  configList.value = data.records
  total.value = data.total
}

function openDialog(row?: ConfigItem) {
  Object.assign(form, row ? { ...row } : { configName: '', configKey: '', configValue: '', remark: '' })
  dialogVisible.value = true
}

async function handleSave() {
  if (form.id) {
    await updateConfig(form)
  } else {
    await addConfig(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await loadData()
}

async function handleDelete(row: ConfigItem) {
  await ElMessageBox.confirm(`确认删除参数「${row.configName}」？`, '提示', { type: 'warning' })
  await deleteConfig(row.id)
  ElMessage.success('删除成功')
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
