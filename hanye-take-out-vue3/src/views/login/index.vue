<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginAPI } from '@/api/employee'
import { useUserInfoStore } from '@/store'
import brandLogo from '@/assets/image/logo.png'
import loginBg from '@/assets/image/login.jpg'

const router = useRouter()
const userInfoStore = useUserInfoStore()
const loginRef = ref()

const form = ref({
  account: '',
  password: '',
})

const rules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]{1,10}$/, message: '账号需为 1-10 位字母或数字', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^\S{6,15}$/, message: '密码需为 6-15 位非空字符', trigger: 'blur' },
  ],
}

const loginFn = async () => {
  const valid = await loginRef.value.validate()
  if (!valid) return
  const { data: res } = await loginAPI(form.value)
  if (res.code !== 0) return
  userInfoStore.userInfo = res.data
  ElMessage.success('登录成功')
  router.push('/')
}
</script>

<template>
  <div class="auth-shell">
    <section class="auth-hero" :style="{ backgroundImage: `url(${loginBg})` }">
      <div class="auth-brand">
        <img :src="brandLogo" alt="嘉园外卖" />
        <div>
          <strong>嘉园外卖</strong>
          <span>餐饮运营后台</span>
        </div>
      </div>

      <div class="auth-copy">
        <span class="auth-badge">Restaurant Command Center</span>
        <h1>把日常运营放进一块更顺手的工作台。</h1>
        <p>订单提醒、商品管理、经营分析与门店状态集中处理，不改接口，也能让后台体验更像正式产品。</p>
      </div>

      <ul class="auth-metrics">
        <li>
          <strong>24h</strong>
          <span>订单消息持续监听</span>
        </li>
        <li>
          <strong>7+</strong>
          <span>核心业务模块统一收口</span>
        </li>
        <li>
          <strong>1屏</strong>
          <span>看清门店经营状态</span>
        </li>
      </ul>
    </section>

    <section class="auth-panel">
      <div class="auth-card">
        <span class="auth-badge">欢迎回来</span>
        <h2>登录后台</h2>
        <p>输入你的账号和密码，继续处理今天的门店运营。</p>

        <el-form ref="loginRef" :model="form" :rules="rules" class="login-form">
          <el-form-item prop="account">
            <el-input v-model="form.account" placeholder="请输入账号" size="large">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large">
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loginFn">进入系统</el-button>
          </el-form-item>
        </el-form>

        <div class="auth-switch">
          <span>还没有账号？</span>
          <el-link type="primary" @click="$router.push('/reg')">去注册</el-link>
        </div>
      </div>
    </section>
  </div>
</template>

<style lang="less" scoped>
.login-form {
  :deep(.el-input__wrapper) {
    min-height: 50px;
  }
}
</style>
