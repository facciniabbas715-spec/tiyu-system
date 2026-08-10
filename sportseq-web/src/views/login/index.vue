<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <h1 class="title">体育器材管理系统</h1>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item prop="code">
          <div class="captcha-row">
            <el-input v-model="form.code" placeholder="验证码" :prefix-icon="Key" />
            <img
              :src="captchaImg"
              class="captcha-img"
              alt="验证码"
              title="点击刷新"
              @click="loadCaptcha"
            />
          </div>
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Key, Lock, User } from '@element-plus/icons-vue'
import { getCaptcha } from '@/api/auth'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaImg = ref('')
const form = reactive({
  username: '',
  password: '',
  code: '',
  uuid: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

async function loadCaptcha() {
  const data = await getCaptcha()
  form.uuid = data.uuid
  captchaImg.value = data.img
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await userStore.login({ ...form })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch {
    form.code = ''
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped lang="scss">
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #1f3b73 0%, #2f6f9f 100%);
}

.login-card {
  width: 400px;
  padding: 8px 16px 16px;
}

.title {
  margin-bottom: 24px;
  text-align: center;
  font-size: 22px;
  color: #303133;
}

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;

  .el-input {
    flex: 1;
  }
}

.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 4px;
  cursor: pointer;
}

.login-btn {
  width: 100%;
  letter-spacing: 6px;
}
</style>
