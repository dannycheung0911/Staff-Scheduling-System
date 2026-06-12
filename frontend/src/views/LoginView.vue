<template>
  <div class="login-wrapper">
    <div class="login-box">
      <div class="login-logo">
        <el-icon size="40" color="#409eff"><Tickets /></el-icon>
        <h2>延安三路站排班系统</h2>
        <p>地铁运营三中心</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          style="width:100%; margin-top:8px"
          :loading="loading"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form>
      <div class="login-hint">默认账号: admin / admin123</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    // error handled by axios interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a73e8 0%, #0d47a1 100%);
}
.login-box {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  width: 380px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.login-logo {
  text-align: center;
  margin-bottom: 32px;
}
.login-logo h2 {
  font-size: 20px;
  color: #1a1a2e;
  margin-top: 12px;
}
.login-logo p {
  color: #888;
  font-size: 13px;
  margin-top: 4px;
}
.login-hint {
  text-align: center;
  color: #aaa;
  font-size: 12px;
  margin-top: 16px;
}
</style>
