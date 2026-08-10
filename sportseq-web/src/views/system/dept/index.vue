<template>
  <el-card>
    <div class="toolbar">
      <el-input
        v-model="query.deptName"
        placeholder="部门名称"
        clearable
        style="width: 220px"
        @keyup.enter="loadData"
      />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button v-permission="['system:dept:add']" type="success" @click="openDialog()">
        新增部门
      </el-button>
    </div>

    <el-table :data="deptList" row-key="id" border default-expand-all>
      <el-table-column prop="deptName" label="部门名称" min-width="160" />
      <el-table-column prop="orderNum" label="排序" width="70" />
      <el-table-column prop="leader" label="负责人" width="110" />
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="['system:dept:edit']"
            link
            type="primary"
            @click="openDialog(row)"
          >
            修改
          </el-button>
          <el-button
            v-permission="['system:dept:remove']"
            link
            type="danger"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改部门' : '新增部门'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="deptOptions"
            :props="{ label: 'deptName', value: 'id' }"
            check-strictly
            placeholder="选择上级部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="显示排序">
          <el-input-number v-model="form.orderNum" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  addDept,
  deleteDept,
  getDeptTree,
  updateDept,
  type DeptDTO,
  type DeptItem,
} from '@/api/system/dept'

const deptList = ref<DeptItem[]>([])
const deptOptions = ref<DeptItem[]>([])
const query = reactive({ deptName: '' })
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = (): DeptDTO => ({
  parentId: 100,
  deptName: '',
  orderNum: 0,
  status: 1,
})
const form = reactive<DeptDTO>(defaultForm())

const rules: FormRules = {
  parentId: [{ required: true, message: '请选择上级部门', trigger: 'change' }],
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
}

async function loadData() {
  deptList.value = await getDeptTree(query.deptName || undefined)
  deptOptions.value = await getDeptTree()
}

function openDialog(row?: DeptItem) {
  Object.assign(form, row ? { ...row } : defaultForm())
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await updateDept(form)
    } else {
      await addDept(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: DeptItem) {
  await ElMessageBox.confirm(`确认删除部门「${row.deptName}」？`, '提示', { type: 'warning' })
  await deleteDept(row.id)
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
