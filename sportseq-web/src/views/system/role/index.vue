<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.roleName" placeholder="角色名称" clearable style="width: 180px" @keyup.enter="loadData" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px">
        <el-option label="正常" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button v-permission="['system:role:add']" type="success" @click="openDialog()">
        新增角色
      </el-button>
    </div>

    <el-table :data="roleList" border>
      <el-table-column prop="roleName" label="角色名称" width="150" />
      <el-table-column prop="roleKey" label="权限字符" width="180" />
      <el-table-column prop="roleSort" label="排序" width="70" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column prop="remark" label="备注" min-width="140" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="['system:role:edit']" link type="primary" @click="openDialog(row)">
            修改
          </el-button>
          <el-button v-permission="['system:role:assignMenu']" link type="warning" @click="openMenuDialog(row)">
            分配权限
          </el-button>
          <el-button v-permission="['system:role:remove']" link type="danger" @click="handleDelete(row)">
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改角色' : '新增角色'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey">
          <el-input v-model="form.roleKey" :disabled="form.id === 1" placeholder="如 equipment_admin" />
        </el-form-item>
        <el-form-item label="显示排序">
          <el-input-number v-model="form.roleSort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="menuDialogVisible" :title="`分配权限：${currentRole?.roleName}`" width="480px">
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        :props="{ label: 'menuName', children: 'children' }"
        default-expand-all
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleAssignMenus">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import type { ElTree } from 'element-plus'
import {
  addRole,
  assignRoleMenus,
  deleteRole,
  getRoleMenuIds,
  getRolePage,
  updateRole,
  type RoleDTO,
  type RoleItem,
} from '@/api/system/role'
import { getMenuTree, type MenuItem } from '@/api/system/menu'

const roleList = ref<RoleItem[]>([])
const menuTree = ref<MenuItem[]>([])
const total = ref(0)
const query = reactive({
  current: 1,
  size: 10,
  roleName: '',
  status: undefined as number | undefined,
})
const dialogVisible = ref(false)
const menuDialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const menuTreeRef = ref<InstanceType<typeof ElTree>>()
const currentRole = ref<RoleItem | null>(null)

const defaultForm = (): RoleDTO => ({ roleName: '', roleKey: '', roleSort: 0, status: 1 })
const form = reactive<RoleDTO>(defaultForm())

const rules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }],
}

async function loadData() {
  const data = await getRolePage(query)
  roleList.value = data.records
  total.value = data.total
}

function openDialog(row?: RoleItem) {
  Object.assign(form, row ? { ...row } : defaultForm())
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await updateRole(form)
    } else {
      await addRole(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function openMenuDialog(row: RoleItem) {
  currentRole.value = row
  menuDialogVisible.value = true
  if (!menuTree.value.length) {
    menuTree.value = await getMenuTree()
  }
  const checkedIds = await getRoleMenuIds(row.id)
  await nextTick()
  menuTreeRef.value?.setCheckedKeys(checkedIds)
}

async function handleAssignMenus() {
  if (!currentRole.value) return
  const checked = menuTreeRef.value?.getCheckedKeys() as number[]
  const halfChecked = menuTreeRef.value?.getHalfCheckedKeys() as number[]
  saving.value = true
  try {
    await assignRoleMenus(currentRole.value.id, [...checked, ...halfChecked])
    ElMessage.success('分配成功')
    menuDialogVisible.value = false
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: RoleItem) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
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
