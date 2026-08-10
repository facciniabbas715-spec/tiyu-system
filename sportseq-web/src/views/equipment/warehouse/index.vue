<template>
  <el-card>
    <div class="toolbar">
      <el-button v-permission="['equipment:warehouse:add']" type="success" @click="openDialog()">新增仓库</el-button>
    </div>

    <el-table :data="warehouseList" border>
      <el-table-column prop="warehouseCode" label="仓库编码" width="120" />
      <el-table-column prop="warehouseName" label="仓库名称" width="160" />
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column prop="address" label="地址" min-width="180" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="['equipment:warehouse:edit']" link type="primary" @click="openDialog(row)">修改</el-button>
          <el-button v-permission="['equipment:warehouse:remove']" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改仓库' : '新增仓库'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="仓库编码">
          <el-input v-model="form.warehouseCode" :disabled="!!form.id" placeholder="如 WH-01" />
        </el-form-item>
        <el-form-item label="仓库名称">
          <el-input v-model="form.warehouseName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
  addWarehouse,
  deleteWarehouse,
  getWarehouseList,
  updateWarehouse,
  type WarehouseDTO,
  type WarehouseItem,
} from '@/api/equipment/warehouse'

const warehouseList = ref<WarehouseItem[]>([])
const dialogVisible = ref(false)
const form = reactive<WarehouseDTO>({ warehouseCode: '', warehouseName: '', status: 1 })

async function loadData() {
  warehouseList.value = await getWarehouseList()
}

function openDialog(row?: WarehouseItem) {
  Object.assign(form, row ? { ...row } : { warehouseCode: '', warehouseName: '', phone: '', address: '', status: 1 })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.warehouseCode || !form.warehouseName) {
    ElMessage.warning('请填写仓库编码与名称')
    return
  }
  if (form.id) {
    await updateWarehouse(form)
  } else {
    await addWarehouse(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await loadData()
}

async function handleDelete(row: WarehouseItem) {
  await ElMessageBox.confirm(`确认删除仓库「${row.warehouseName}」？`, '提示', { type: 'warning' })
  await deleteWarehouse(row.id)
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
