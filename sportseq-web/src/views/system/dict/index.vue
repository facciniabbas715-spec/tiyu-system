<template>
  <el-row :gutter="14">
    <el-col :span="10">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>字典类型</span>
            <el-button v-permission="['system:dict:add']" type="success" size="small" @click="openTypeDialog()">
              新增
            </el-button>
          </div>
        </template>
        <el-table :data="typeList" highlight-current-row @current-change="handleTypeChange">
          <el-table-column prop="dictName" label="字典名称" min-width="110" />
          <el-table-column prop="dictType" label="字典类型" min-width="110" />
          <el-table-column label="操作" width="110">
            <template #default="{ row }">
              <el-button v-permission="['system:dict:edit']" link type="primary" @click.stop="openTypeDialog(row)">修改</el-button>
              <el-button v-permission="['system:dict:remove']" link type="danger" @click.stop="handleDeleteType(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
    <el-col :span="14">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>字典数据：{{ currentType?.dictType || '请选择字典类型' }}</span>
            <el-button
              v-permission="['system:dict:add']"
              type="success"
              size="small"
              :disabled="!currentType"
              @click="openDataDialog()"
            >
              新增
            </el-button>
          </div>
        </template>
        <el-table :data="dataList" border>
          <el-table-column prop="dictSort" label="排序" width="70" />
          <el-table-column prop="dictLabel" label="标签" width="110" />
          <el-table-column prop="dictValue" label="键值" width="110" />
          <el-table-column prop="isDefault" label="默认" width="70" />
          <el-table-column label="操作" width="110">
            <template #default="{ row }">
              <el-button v-permission="['system:dict:edit']" link type="primary" @click="openDataDialog(row)">修改</el-button>
              <el-button v-permission="['system:dict:remove']" link type="danger" @click="handleDeleteData(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
  </el-row>

  <el-dialog v-model="typeDialogVisible" :title="typeForm.id ? '修改字典类型' : '新增字典类型'" width="480px">
    <el-form :model="typeForm" label-width="90px">
      <el-form-item label="字典名称">
        <el-input v-model="typeForm.dictName" />
      </el-form-item>
      <el-form-item label="字典类型">
        <el-input v-model="typeForm.dictType" :disabled="!!typeForm.id" />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="typeForm.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="typeDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSaveType">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="dataDialogVisible" :title="dataForm.id ? '修改字典数据' : '新增字典数据'" width="480px">
    <el-form :model="dataForm" label-width="90px">
      <el-form-item label="数据标签">
        <el-input v-model="dataForm.dictLabel" />
      </el-form-item>
      <el-form-item label="数据键值">
        <el-input v-model="dataForm.dictValue" />
      </el-form-item>
      <el-form-item label="显示排序">
        <el-input-number v-model="dataForm.dictSort" :min="0" />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="dataForm.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dataDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSaveData">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addDictData,
  addDictType,
  deleteDictData,
  deleteDictType,
  getDictDataPage,
  getDictTypePage,
  updateDictData,
  updateDictType,
  type DictDataItem,
  type DictTypeItem,
} from '@/api/system/dict'

const typeList = ref<DictTypeItem[]>([])
const dataList = ref<DictDataItem[]>([])
const currentType = ref<DictTypeItem | null>(null)
const typeDialogVisible = ref(false)
const dataDialogVisible = ref(false)
const typeForm = reactive<{ id?: number; dictName: string; dictType: string; status: number }>({
  dictName: '',
  dictType: '',
  status: 1,
})
const dataForm = reactive<Partial<DictDataItem> & { id?: number; dictSort: number; status: number }>({
  dictSort: 0,
  status: 1,
})

async function loadTypes() {
  const data = await getDictTypePage({ current: 1, size: 100 })
  typeList.value = data.records
}

async function handleTypeChange(row: DictTypeItem) {
  currentType.value = row
  await loadData()
}

async function loadData() {
  if (!currentType.value) return
  const data = await getDictDataPage({ current: 1, size: 100, dictType: currentType.value.dictType })
  dataList.value = data.records
}

function openTypeDialog(row?: DictTypeItem) {
  Object.assign(typeForm, row ? { ...row } : { dictName: '', dictType: '', status: 1 })
  typeDialogVisible.value = true
}

async function handleSaveType() {
  if (typeForm.id) {
    await updateDictType(typeForm as never)
  } else {
    await addDictType(typeForm)
  }
  ElMessage.success('保存成功')
  typeDialogVisible.value = false
  await loadTypes()
}

async function handleDeleteType(row: DictTypeItem) {
  await ElMessageBox.confirm(`确认删除字典类型「${row.dictName}」？`, '提示', { type: 'warning' })
  await deleteDictType(row.id)
  ElMessage.success('删除成功')
  await loadTypes()
}

function openDataDialog(row?: DictDataItem) {
  Object.assign(dataForm, row ? { ...row } : { dictSort: 0, status: 1, dictType: currentType.value?.dictType })
  dataDialogVisible.value = true
}

async function handleSaveData() {
  if (dataForm.id) {
    await updateDictData(dataForm as never)
  } else {
    await addDictData({ ...dataForm, dictType: currentType.value?.dictType })
  }
  ElMessage.success('保存成功')
  dataDialogVisible.value = false
  await loadData()
}

async function handleDeleteData(row: DictDataItem) {
  await deleteDictData(row.id)
  ElMessage.success('删除成功')
  await loadData()
}

onMounted(loadTypes)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
