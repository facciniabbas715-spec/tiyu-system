<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.categoryName" placeholder="分类名称" clearable style="width: 200px" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button v-permission="['equipment:category:add']" type="success" @click="openDialog()">
        新增分类
      </el-button>
    </div>

    <el-table :data="categoryList" row-key="id" border default-expand-all>
      <el-table-column prop="categoryName" label="分类名称" min-width="160" />
      <el-table-column prop="categoryCode" label="分类编码" width="120" />
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="['equipment:category:edit']" link type="primary" @click="openDialog(row)">修改</el-button>
          <el-button v-permission="['equipment:category:remove']" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改分类' : '新增分类'" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="上级分类">
          <el-tree-select
            v-model="form.parentId"
            :data="categoryOptions"
            :props="{ label: 'categoryName', value: 'id' }"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类名称">
          <el-input v-model="form.categoryName" />
        </el-form-item>
        <el-form-item label="分类编码">
          <el-input v-model="form.categoryCode" :disabled="!!form.id" placeholder="如 QX，将作为器材编码前缀" />
        </el-form-item>
        <el-form-item label="显示排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
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
  addCategory,
  deleteCategory,
  getCategoryTree,
  updateCategory,
  type CategoryDTO,
  type CategoryItem,
} from '@/api/equipment/category'

const categoryList = ref<CategoryItem[]>([])
const categoryOptions = ref<CategoryItem[]>([])
const query = reactive({ categoryName: '' })
const dialogVisible = ref(false)
const form = reactive<CategoryDTO>({ categoryName: '', categoryCode: '', sortOrder: 0, status: 1 })

async function loadData() {
  categoryList.value = await getCategoryTree(query.categoryName || undefined)
  categoryOptions.value = await getCategoryTree()
}

function openDialog(row?: CategoryItem) {
  Object.assign(form, row ? { ...row } : { categoryName: '', categoryCode: '', sortOrder: 0, status: 1, parentId: 0 })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.categoryName || !form.categoryCode) {
    ElMessage.warning('请填写分类名称与编码')
    return
  }
  if (form.id) {
    await updateCategory(form)
  } else {
    await addCategory(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await loadData()
}

async function handleDelete(row: CategoryItem) {
  await ElMessageBox.confirm(`确认删除分类「${row.categoryName}」？`, '提示', { type: 'warning' })
  await deleteCategory(row.id)
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
