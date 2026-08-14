<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.equipmentName" placeholder="器材名称" clearable style="width: 150px" @keyup.enter="loadData" />
      <el-input v-model="query.equipmentCode" placeholder="器材编码" clearable style="width: 170px" @keyup.enter="loadData" />
      <el-tree-select
        v-model="query.categoryId"
        :data="categoryOptions"
        :props="{ label: 'categoryName', value: 'id' }"
        check-strictly
        clearable
        placeholder="分类"
        style="width: 160px"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 100px">
        <el-option label="正常" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button v-permission="['equipment:add']" type="success" @click="openDialog()">新增器材</el-button>
      <el-upload
        :show-file-list="false"
        accept=".xlsx,.xls"
        :http-request="handleImport"
        style="display: inline-block"
      >
        <el-button v-permission="['equipment:import']" type="primary" plain>导入</el-button>
      </el-upload>
      <el-button v-permission="['equipment:export']" type="success" plain @click="handleExport">导出</el-button>
    </div>

    <el-table :data="equipmentList" border>
      <el-table-column prop="equipmentCode" label="器材编码" width="150" />
      <el-table-column prop="equipmentName" label="器材名称" min-width="130" />
      <el-table-column prop="categoryName" label="分类" width="110" />
      <el-table-column prop="brand" label="品牌" width="100" />
      <el-table-column prop="spec" label="规格" width="100" />
      <el-table-column prop="unit" label="单位" width="60" />
      <el-table-column prop="purchasePrice" label="单价" width="90" />
      <el-table-column prop="safeStock" label="安全库存" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="['equipment:edit']" link type="primary" @click="openDialog(row)">修改</el-button>
          <el-button v-permission="['equipment:edit']" link type="warning" @click="handleToggle(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button v-permission="['equipment:remove']" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改器材' : '新增器材'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="器材名称">
          <el-input v-model="form.equipmentName" />
        </el-form-item>
        <el-form-item label="分类">
          <el-tree-select
            v-model="form.categoryId"
            :data="categoryOptions"
            :props="{ label: 'categoryName', value: 'id' }"
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="品牌/型号">
          <el-input v-model="form.brand" placeholder="品牌" style="width: 48%" />
          <el-input v-model="form.model" placeholder="型号" style="width: 48%; margin-left: 4%" />
        </el-form-item>
        <el-form-item label="规格/单位">
          <el-input v-model="form.spec" placeholder="规格" style="width: 48%" />
          <el-input v-model="form.unit" placeholder="单位（个/副/套）" style="width: 48%; margin-left: 4%" />
        </el-form-item>
        <el-form-item label="采购单价">
          <el-input-number v-model="form.purchasePrice" :min="0" :precision="2" style="width: 200px" />
        </el-form-item>
        <el-form-item label="安全库存">
          <el-input-number v-model="form.safeStock" :min="0" style="width: 200px" />
        </el-form-item>
        <el-form-item label="最长借用天数">
          <el-input-number v-model="form.maxBorrowDays" :min="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
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
import type { UploadRequestOptions } from 'element-plus'
import {
  addEquipment,
  changeEquipmentStatus,
  deleteEquipment,
  exportEquipmentUrl,
  getEquipmentPage,
  importEquipment,
  updateEquipment,
  type EquipmentDTO,
  type EquipmentItem,
} from '@/api/equipment/equipment'
import { getCategoryTree, type CategoryItem } from '@/api/equipment/category'
import { downloadFile } from '@/utils/download'

const equipmentList = ref<EquipmentItem[]>([])
const categoryOptions = ref<CategoryItem[]>([])
const total = ref(0)
const query = reactive({
  current: 1,
  size: 10,
  equipmentName: '',
  equipmentCode: '',
  categoryId: undefined as number | undefined,
  status: undefined as number | undefined,
})
const dialogVisible = ref(false)
const form = reactive<EquipmentDTO>({ equipmentName: '', categoryId: 0, unit: '', safeStock: 0, status: 1 })

async function loadData() {
  const data = await getEquipmentPage(query)
  equipmentList.value = data.records
  total.value = data.total
}

async function loadOptions() {
  categoryOptions.value = await getCategoryTree()
}

function openDialog(row?: EquipmentItem) {
  Object.assign(
    form,
    row
      ? { ...row }
      : { equipmentName: '', categoryId: 0, brand: '', model: '', spec: '', unit: '', purchasePrice: 0, safeStock: 0, maxBorrowDays: 7, status: 1, description: '' },
  )
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.equipmentName || !form.categoryId || !form.unit) {
    ElMessage.warning('请填写名称、分类与单位')
    return
  }
  if (form.id) {
    await updateEquipment(form)
  } else {
    await addEquipment(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await loadData()
}

async function handleToggle(row: EquipmentItem) {
  await changeEquipmentStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('操作成功')
  await loadData()
}

async function handleDelete(row: EquipmentItem) {
  await ElMessageBox.confirm(`确认删除器材「${row.equipmentName}」（${row.equipmentCode}）？`, '提示', { type: 'warning' })
  await deleteEquipment(row.id)
  ElMessage.success('删除成功')
  await loadData()
}

async function handleImport(options: UploadRequestOptions) {
  const result = await importEquipment(options.file as File)
  ElMessage.success(`导入完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条`)
  if (result.errors.length) {
    ElMessageBox.alert(result.errors.join('\n'), '失败明细', { type: 'warning' })
  }
  await loadData()
}

async function handleExport() {
  await downloadFile(exportEquipmentUrl(query), `器材列表_${Date.now()}.xlsx`)
}

onMounted(() => {
  loadData()
  loadOptions()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
</style>
