<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.username" placeholder="用户名" clearable style="width: 160px" @keyup.enter="loadData" />
      <el-input v-model="query.phone" placeholder="手机号" clearable style="width: 160px" @keyup.enter="loadData" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px">
        <el-option label="正常" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button v-permission="['system:user:add']" type="success" @click="openDialog()">
        新增用户
      </el-button>
    </div>

    <el-table :data="userList" border>
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="姓名" width="110" />
      <el-table-column prop="deptName" label="部门" width="120" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="loginDate" label="最后登录" width="160" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="['system:user:edit']" link type="primary" @click="openDialog(row)">
            修改
          </el-button>
          <el-button v-permission="['system:user:edit']" link type="warning" @click="handleToggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button v-permission="['system:user:resetPwd']" link type="primary" @click="handleResetPassword(row)">
            重置密码
          </el-button>
          <el-button v-permission="['system:user:remove']" link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :total="total"
      layout="total, prev, pager, next, sizes"
      :page-sizes="[10, 20, 50]"
      style="margin-top: 14px; justify-content: flex-end"
      @change="loadData"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改用户' : '新增用户'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录账号" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="8-20位，含字母和数字" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="form.deptId"
            :data="deptOptions"
            :props="{ label: 'deptName', value: 'id' }"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="form.userType" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="教师" value="teacher" />
            <el-option label="学生" value="student" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%">
            <el-option v-for="role in roleList" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
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
  addUser,
  changeUserStatus,
  deleteUser,
  getUserPage,
  resetPassword,
  updateUser,
  type UserDTO,
  type UserItem,
} from '@/api/system/user'
import { getDeptTree, type DeptItem } from '@/api/system/dept'
import { getRolePage, type RoleItem } from '@/api/system/role'

const userList = ref<UserItem[]>([])
const deptOptions = ref<DeptItem[]>([])
const roleList = ref<RoleItem[]>([])
const total = ref(0)
const query = reactive({
  current: 1,
  size: 10,
  username: '',
  phone: '',
  status: undefined as number | undefined,
})
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = (): UserDTO => ({
  username: '',
  password: '',
  realName: '',
  deptId: null,
  userType: 'other',
  status: 1,
  roleIds: [],
})
const form = reactive<UserDTO>(defaultForm())

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度需为8-20位', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}

async function loadData() {
  const data = await getUserPage(query)
  userList.value = data.records
  total.value = data.total
}

async function loadOptions() {
  deptOptions.value = await getDeptTree()
  const roles = await getRolePage({ current: 1, size: 100 })
  roleList.value = roles.records
}

function openDialog(row?: UserItem) {
  Object.assign(form, row ? { ...row, password: undefined } : defaultForm())
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await updateUser(form)
    } else {
      await addUser(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function handleToggleStatus(row: UserItem) {
  await changeUserStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('操作成功')
  await loadData()
}

async function handleResetPassword(row: UserItem) {
  const { value } = await ElMessageBox.prompt(`为用户「${row.username}」设置新密码`, '重置密码', {
    inputType: 'password',
    inputPattern: /^(?=.*[A-Za-z])(?=.*\d).{8,20}$/,
    inputErrorMessage: '密码需8-20位且包含字母和数字',
  })
  await resetPassword(row.id, value)
  ElMessage.success('密码已重置')
}

async function handleDelete(row: UserItem) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  await loadData()
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
