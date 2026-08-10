<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.menuName" placeholder="菜单名称" clearable style="width: 200px" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button v-permission="['system:menu:add']" type="success" @click="openDialog()">
        新增菜单
      </el-button>
    </div>

    <el-table :data="menuList" row-key="id" border default-expand-all>
      <el-table-column prop="menuName" label="菜单名称" min-width="150" />
      <el-table-column label="类型" width="70">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.menuType)">{{ typeText(row.menuType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路由地址" width="140" />
      <el-table-column prop="perms" label="权限标识" min-width="150" />
      <el-table-column prop="icon" label="图标" width="80" />
      <el-table-column prop="orderNum" label="排序" width="70" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="['system:menu:edit']" link type="primary" @click="openDialog(row)">
            修改
          </el-button>
          <el-button v-permission="['system:menu:remove']" link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改菜单' : '新增菜单'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="menuOptions"
            :props="{ label: 'menuName', value: 'id' }"
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio-button value="M">目录</el-radio-button>
            <el-radio-button value="C">菜单</el-radio-button>
            <el-radio-button value="F">按钮</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="路由地址" prop="path">
          <el-input v-model="form.path" placeholder="如 /system 或 user" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 'C'" label="组件路径">
          <el-input v-model="form.component" placeholder="如 system/user/index" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 'F'" label="权限标识" prop="perms">
          <el-input v-model="form.perms" placeholder="如 system:user:add" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名" />
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
  addMenu,
  deleteMenu,
  getMenuTree,
  updateMenu,
  type MenuDTO,
  type MenuItem,
} from '@/api/system/menu'

const menuList = ref<MenuItem[]>([])
const menuOptions = ref<MenuItem[]>([])
const query = reactive({ menuName: '' })
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = (): MenuDTO => ({
  parentId: 0,
  menuName: '',
  menuType: 'C',
  path: '',
  component: '',
  perms: '',
  orderNum: 0,
  status: 1,
})
const form = reactive<MenuDTO>(defaultForm())

const rules: FormRules = {
  parentId: [{ required: true, message: '请选择上级菜单', trigger: 'change' }],
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  path: [{ required: true, message: '请输入路由地址', trigger: 'blur' }],
  perms: [{ required: true, message: '请输入权限标识', trigger: 'blur' }],
}

async function loadData() {
  menuList.value = await getMenuTree(query.menuName || undefined)
  menuOptions.value = await getMenuTree()
}

function openDialog(row?: MenuItem) {
  Object.assign(form, row ? { ...row } : defaultForm())
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await updateMenu(form)
    } else {
      await addMenu(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: MenuItem) {
  await ElMessageBox.confirm(`确认删除菜单「${row.menuName}」？`, '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  await loadData()
}

function typeText(type: string) {
  return { M: '目录', C: '菜单', F: '按钮' }[type] ?? type
}

function typeTag(type: string) {
  return { M: 'warning', C: 'primary', F: 'info' }[type] ?? 'info'
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
