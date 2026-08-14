<template>
  <div class="login-page">
    <el-card class="login-card" shadow="never">
      <div class="login-head">
        <svg class="brand-mark" viewBox="0 0 32 32" aria-hidden="true">
          <defs>
            <linearGradient id="login-logo-gradient" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stop-color="#518bdb" />
              <stop offset="1" stop-color="#36bab8" />
            </linearGradient>
          </defs>
          <rect width="32" height="32" rx="9" fill="url(#login-logo-gradient)" />
          <rect x="7.5" y="13.5" width="17" height="5" rx="2.5" fill="#fff" />
          <rect x="4" y="9" width="5.5" height="14" rx="2.75" fill="#fff" />
          <rect x="22.5" y="9" width="5.5" height="14" rx="2.75" fill="#fff" />
        </svg>
        <span class="eyebrow">器材 · 库存 · 借用 · 报废 · 统计</span>
        <h1 class="title">欢迎登录</h1>
        <p class="subtitle">面向学校、企业与体育场馆的器材全生命周期管理平台</p>
      </div>
      <el-form
        ref="formRef"
        class="login-form"
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
    <div class="login-footer">让每一件器材都被妥善管理</div>
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
    const redirect = (route.query.redirect as string) || '/statistics/dashboard'
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
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(90deg, #518bdb 0%, #36bab8 100%);
}

.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(900px 460px at 12% 18%, rgba(255, 255, 255, 0.18), transparent 62%),
    radial-gradient(760px 420px at 88% 82%, rgba(255, 255, 255, 0.14), transparent 58%);
}

.login-card {
  position: relative;
  width: 400px;
  max-width: calc(100vw - 32px);
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 18px;
  box-shadow: 0 20px 25px -5px rgba(15, 41, 68, 0.28), 0 8px 10px -6px rgba(15, 41, 68, 0.22);

  :deep(.el-card__body) {
    padding: 32px 30px 30px;
  }
}

.login-head {
  margin-bottom: 24px;
  text-align: center;
}

.brand-mark {
  width: 48px;
  height: 48px;
  margin-bottom: 14px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 999px;
  background: #ecf1f9;
  color: #2e5aa0;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.06em;
}

.title {
  margin: 14px 0 8px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: #221f1c;
}

.subtitle {
  font-size: 13px;
  color: #797267;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
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
  border: 1px solid #e6e3dd;
  border-radius: 8px;
  cursor: pointer;
  transition: opacity 0.18s ease;
}

.captcha-img:hover {
  opacity: 0.86;
}

.login-btn {
  width: 100%;
  letter-spacing: 6px;
  height: 40px;
}

.login-footer {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 22px;
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.82);
  pointer-events: none;
}
</style>
