<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerAPI } from '@/api/employee'
import brandLogo from '@/assets/image/logo.png'
import regBg from '@/assets/image/reg.jpg'

const router = useRouter()
const registerRef = ref()

const form = ref({
  account: '',
  password: '',
  repassword: '',
})

const samePwd = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.value.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]{1,10}$/, message: '账号需为 1-10 位字母或数字', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^\S{6,15}$/, message: '密码需为 6-15 位非空字符', trigger: 'blur' },
  ],
  repassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { pattern: /^\S{6,15}$/, message: '密码需为 6-15 位非空字符', trigger: 'blur' },
    { validator: samePwd, trigger: 'blur' },
  ],
}

const registerFn = async () => {
  const valid = await registerRef.value.validate()
  if (!valid) return
  const { data: res } = await registerAPI(form.value)
  if (res.code !== 0) return
  ElMessage.success('注册成功')
  router.push('/login')
}
</script>

<template>
  <div class="auth-shell">
    <section class="auth-hero" :style="{ backgroundImage: `url(${regBg})` }">
      <div class="auth-brand">
        <img :src="brandLogo" alt="嘉园外卖" />
        <div>
          <strong>嘉园外卖</strong>
          <span>餐饮运营后台</span>
        </div>
      </div>

      <div class="auth-copy">
        <span class="auth-badge">New Staff Onboarding</span>
        <h1>为新账号准备一张更像产品首页的欢迎面板。</h1>
        <p>完成注册后即可接入当前后台体系，继续使用现有后端接口、角色逻辑与门店数据流。</p>
      </div>

      <ul class="auth-metrics">
        <li>
          <strong>0</strong>
          <span>后端接口改动</span>
        </li>
        <li>
          <strong>100%</strong>
          <span>沿用现有校验规则</span>
        </li>
        <li>
          <strong>Now</strong>
          <span>立即进入运营后台</span>
        </li>
      </ul>
    </section>

    <section class="auth-panel">
      <div class="auth-card">
        <span class="auth-badge">创建账号</span>
        <h2>注册后台账号</h2>
        <p>填写基础凭据，完成账号创建后即可返回登录页进入系统。</p>

        <el-form ref="registerRef" :model="form" :rules="rules" class="reg-form">
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
          <el-form-item prop="repassword">
            <el-input
              v-model="form.repassword"
              type="password"
              show-password
              placeholder="请再次输入密码"
              size="large"
            >
              <template #prefix>
                <el-icon><CircleCheck /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="registerFn">完成注册</el-button>
          </el-form-item>
        </el-form>

        <div class="auth-switch">
          <span>已经有账号？</span>
          <el-link type="primary" @click="router.push('/login')">返回登录</el-link>
        </div>
      </div>
    </section>
  </div>
</template>

<style lang="less" scoped>
.reg-form {
  :deep(.el-input__wrapper) {
    min-height: 50px;
  }
}
</style>
