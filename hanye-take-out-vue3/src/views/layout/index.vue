<script setup lang="ts" name="layout">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { fixPwdAPI } from '@/api/employee'
import { fixStatusAPI, getStatusAPI } from '@/api/shop'
import { useUserInfoStore } from '@/store'
import brandLogo from '@/assets/image/logo2.png'
import AIAssistant from '@/components/AIAssistant.vue'

const router = useRouter()
const route = useRoute()
const userInfoStore = useUserInfoStore()

const dialogFormVisible = ref(false)
const dialogStatusVisible = ref(false)
const isCollapse = ref(false)
const formLabelWidth = '92px'

const menuList = [
  { title: '控制台', path: '/dashboard', icon: 'PieChart', desc: '经营概览与实时提醒' },
  { title: '数据统计', path: '/statistics', icon: 'DataAnalysis', desc: '营收、用户与订单趋势' },
  { title: '堂食开单', path: '/dinein', icon: 'Bowl', desc: '到店客户现场代下单' },
  { title: '订单管理', path: '/order', icon: 'Tickets', desc: '接单、配送、取消处理' },
  { title: '分类管理', path: '/category', icon: 'Grid', desc: '菜品与套餐分类配置' },
  { title: '套餐管理', path: '/setmeal', icon: 'Food', desc: '套餐信息与上下架' },
  { title: '菜品管理', path: '/dish', icon: 'Dish', desc: '菜品详情、口味与状态' },
  { title: '员工管理', path: '/employee', icon: 'User', desc: '账号、权限与人员信息' },
  { title: '营销活动', path: '/promotion', icon: 'Discount', desc: '满减、折扣、第二份半价' },
  { title: '堂食会员', path: '/member', icon: 'GoldMedal', desc: '储值、积分、等级与消费记录' },
  { title: '客户评价', path: '/review', icon: 'ChatDotRound', desc: '菜品服务评分与改进意见' },
  { title: '员工赏罚', path: '/staff', icon: 'Trophy', desc: '服务评分、小费打赏与北极星排名' },
  { title: '惊喜盲盒', path: '/blindbox', icon: 'Present', desc: '随机套餐三选一翻卡牌' },
  { title: '供应管理', path: '/supply', icon: 'Van', desc: '供应商、采购记录与月底对账' }
]

const form = reactive({
  oldPwd: '',
  newPwd: '',
  rePwd: ''
})

const pwdRef = ref()
const status = ref(1)
const statusActive = ref(1)
const websocket = ref<WebSocket | null>(null)
const audio1 = ref<HTMLAudioElement | null>(null)
const audio2 = ref<HTMLAudioElement | null>(null)

const samePwd = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.newPwd) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = {
  oldPwd: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9]{1,10}$/,
      message: '原密码需为 1-10 位字母或数字',
      trigger: 'blur'
    }
  ],
  newPwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^\S{6,15}$/, message: '新密码需为 6-15 位非空字符', trigger: 'blur' }
  ],
  rePwd: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { pattern: /^\S{6,15}$/, message: '新密码需为 6-15 位非空字符', trigger: 'blur' },
    { validator: samePwd, trigger: 'blur' }
  ]
}

const currentMenu = computed(() => {
  return menuList.find((item) => route.path.startsWith(item.path)) || menuList[0]
})

const activePath = computed(() => currentMenu.value.path)
const currentUserName = computed(() => userInfoStore.userInfo?.account || '未登录')
const currentStatusText = computed(() => (status.value === 1 ? '营业中' : '打烊中'))

const init = async () => {
  const { data: res } = await getStatusAPI()
  status.value = res.data
  statusActive.value = res.data
}

const cancelStatus = () => {
  dialogStatusVisible.value = false
  statusActive.value = status.value
  ElMessage.info('已取消修改')
}

const cancelForm = () => {
  dialogFormVisible.value = false
  form.oldPwd = ''
  form.newPwd = ''
  form.rePwd = ''
  pwdRef.value?.clearValidate?.()
  ElMessage.info('已取消修改')
}

const fixStatus = async () => {
  const { data: res } = await fixStatusAPI(statusActive.value)
  if (res.code !== 0) return
  status.value = statusActive.value
  dialogStatusVisible.value = false
  ElMessage.success('门店状态已更新')
}

const fixPwd = async () => {
  const valid = await pwdRef.value.validate()
  if (!valid) return
  const { data: res } = await fixPwdAPI({
    oldPwd: form.oldPwd,
    newPwd: form.newPwd
  })
  if (res.code !== 0) return
  ElMessage.success('密码修改成功')
  cancelForm()
}

const quitFn = () => {
  ElMessageBox.confirm('退出后需要重新登录，是否继续？', '退出登录', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      userInfoStore.userInfo = null
      ElMessage.success('已退出登录')
      router.push('/login')
    })
    .catch(() => {
      ElMessage.info('已取消退出')
    })
}

const notifyMessage = (payload: { type: number; content: string; orderId: number }) => {
  ElNotification({
    title: payload.type === 1 ? '新订单提醒' : '催单提醒',
    message:
      payload.type === 1
        ? `您有新的待接订单，${payload.content}`
        : `${payload.content}，点击后前往订单页处理`,
    duration: 0,
    type: payload.type === 1 ? 'success' : 'warning',
    onClick: () => {
      router.push(`/order?orderId=${payload.orderId}`).catch(() => undefined)
      setTimeout(() => {
        location.reload()
      }, 100)
    }
  })
}

const webSocket = () => {
  const clientId = Math.random().toString(36).slice(2)
  const socketUrl = `ws://localhost:8081/ws/${clientId}`

  if (typeof WebSocket === 'undefined') {
    ElNotification({
      title: '浏览器不支持',
      message: '当前浏览器无法接收实时消息提醒，请更换浏览器重试。',
      type: 'warning',
      duration: 0
    })
    return
  }

  websocket.value = new WebSocket(socketUrl)
  websocket.value.onmessage = (msg) => {
    const payload = JSON.parse(msg.data)
    if (audio1.value) audio1.value.currentTime = 0
    if (audio2.value) audio2.value.currentTime = 0
    if (payload.type === 1) {
      audio1.value?.play()
    } else if (payload.type === 2) {
      audio2.value?.play()
    }
    notifyMessage(payload)
  }

  websocket.value.onerror = () => {
    ElNotification({
      title: '服务异常',
      message: '实时消息连接失败，当前无法接收订单提醒。',
      type: 'error',
      duration: 0
    })
  }
}

onMounted(() => {
  init()
  webSocket()
})

onBeforeUnmount(() => {
  websocket.value?.close()
})
</script>

<template>
  <div class="layout-shell">
    <el-dialog v-model="dialogStatusVisible" title="门店营业状态" width="560px">
      <div class="status-dialog">
        <el-radio-group v-model="statusActive" class="status-radio-group">
          <el-radio :value="1" size="large" class="status-radio">
            <div class="status-radio-copy">
              <strong>营业中</strong>
              <span>系统自动接收新的外卖订单，适合门店正常出餐时使用。</span>
            </div>
          </el-radio>
          <el-radio :value="0" size="large" class="status-radio">
            <div class="status-radio-copy">
              <strong>打烊中</strong>
              <span>暂停自动接单，适合备货、休息或临时关闭门店时使用。</span>
            </div>
          </el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelStatus">取消</el-button>
          <el-button type="primary" @click="fixStatus">确认</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogFormVisible" title="修改密码" width="520px">
      <el-form ref="pwdRef" :model="form" :rules="rules">
        <el-form-item prop="oldPwd" label="原密码" :label-width="formLabelWidth">
          <el-input v-model="form.oldPwd" autocomplete="off" show-password />
        </el-form-item>
        <el-form-item prop="newPwd" label="新密码" :label-width="formLabelWidth">
          <el-input v-model="form.newPwd" autocomplete="off" show-password />
        </el-form-item>
        <el-form-item prop="rePwd" label="确认密码" :label-width="formLabelWidth">
          <el-input v-model="form.rePwd" autocomplete="off" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelForm">取消</el-button>
          <el-button type="primary" @click="fixPwd">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <div class="layout-grid">
      <aside class="side-panel" :class="{ collapsed: isCollapse }">
        <div class="brand-panel">
          <img :src="brandLogo" alt="嘉园外卖" />
          <div v-if="!isCollapse" class="brand-copy">
            <strong>嘉园外卖</strong>
            <span>餐饮后台管理台</span>
          </div>
        </div>

        <div v-if="!isCollapse" class="brand-intro">
          <p>统一处理订单、商品、门店与经营数据，让日常运营界面更像一个完整产品。</p>
        </div>

        <el-menu
          :default-active="activePath"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          class="aside-menu"
        >
          <template v-for="item in menuList" :key="item.path">
            <el-menu-item :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <template #title>
                <div class="menu-copy">
                  <span>{{ item.title }}</span>
                  <small>{{ item.desc }}</small>
                </div>
              </template>
            </el-menu-item>
          </template>
        </el-menu>

        <div v-if="!isCollapse" class="side-bottom">
          <div class="status-pill" :class="{ closed: status !== 1 }">
            <span class="status-dot"></span>
            {{ currentStatusText }}
          </div>
          <p>实时消息会在此后台持续监听。</p>
        </div>
      </aside>

      <div class="main-shell">
        <header class="topbar">
          <div class="topbar-left">
            <button class="collapse-toggle" type="button" @click="isCollapse = !isCollapse">
              <el-icon v-if="isCollapse"><Expand /></el-icon>
              <el-icon v-else><Fold /></el-icon>
            </button>
            <div class="title-block">
              <span class="eyebrow">{{ currentMenu.title }}</span>
              <h1>{{ currentMenu.desc }}</h1>
            </div>
          </div>

          <div class="topbar-right">
            <button class="shop-status-chip" type="button" @click="dialogStatusVisible = true">
              <span class="chip-dot"></span>
              {{ currentStatusText }}
            </button>

            <el-dropdown>
              <div class="user-chip">
                <div class="user-avatar">{{ currentUserName.slice(0, 1).toUpperCase() }}</div>
                <div class="user-copy">
                  <strong>{{ currentUserName }}</strong>
                  <span>欢迎回来</span>
                </div>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="dialogFormVisible = true">修改密码</el-dropdown-item>
                  <el-dropdown-item @click="quitFn">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <audio ref="audio1" hidden>
              <source src="../../assets/preview.mp3" type="audio/mp3" />
            </audio>
            <audio ref="audio2" hidden>
              <source src="../../assets/reminder.mp3" type="audio/mp3" />
            </audio>
          </div>
        </header>

        <main class="content-shell">
          <router-view></router-view>
        </main>

        <footer class="layout-footer">
          <span>JiaYuan Take Out Admin</span>
          <span>让订单、商品与经营数据在同一块工作台里流动</span>
        </footer>
      </div>
    </div>
    <!-- 全局悬浮 AI 助手：所有页面都能看到，不遮挡主体内容 -->
    <AIAssistant />
  </div>
</template>

<style lang="less" scoped>
.layout-shell {
  min-height: 100vh;
  padding: 18px;
}

.layout-grid {
  min-height: calc(100vh - 36px);
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 18px;
}

.side-panel,
.main-shell {
  border-radius: 32px;
  box-shadow: var(--shadow-lg);
}

.side-panel {
  display: flex;
  flex-direction: column;
  padding: 18px;
  color: var(--text-light);
  background: linear-gradient(180deg, rgba(23, 50, 57, 0.98), rgba(23, 50, 57, 0.9)),
    radial-gradient(circle at top right, rgba(239, 143, 53, 0.2), transparent 26%);
}

.side-panel.collapsed {
  padding-inline: 14px;
}

.brand-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 22px;

  img {
    width: 52px;
    height: 52px;
    border-radius: 18px;
    object-fit: cover;
    background: rgba(255, 255, 255, 0.12);
    padding: 6px;
  }
}

.brand-copy {
  strong {
    display: block;
    font-size: 1.15rem;
    letter-spacing: 0.02em;
  }

  span {
    color: rgba(255, 255, 255, 0.64);
    font-size: 0.88rem;
  }
}

.brand-intro {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.7;
  margin-bottom: 18px;
}

.aside-menu {
  flex: 1;
  border: none;
  background: transparent;
}

:deep(.aside-menu.el-menu) {
  background: transparent;
}

:deep(.aside-menu .el-menu-item) {
  height: auto;
  min-height: 58px;
  margin: 0 0 10px;
  border-radius: 20px;
  color: rgba(255, 255, 255, 0.76);
  background: transparent;
  line-height: 1.25;
  padding: 14px 16px !important;
}

:deep(.aside-menu .el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
}

:deep(.aside-menu .el-menu-item.is-active) {
  color: #fff;
  background: linear-gradient(135deg, rgba(239, 143, 53, 0.96), rgba(221, 107, 32, 0.92));
  box-shadow: 0 16px 28px rgba(239, 143, 53, 0.24);
}

:deep(.aside-menu .el-menu-item .el-icon) {
  font-size: 1.15rem;
}

.menu-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;

  small {
    color: rgba(255, 255, 255, 0.56);
    font-size: 0.74rem;
    font-style: normal;
  }
}

:deep(.aside-menu .el-menu-item.is-active .menu-copy small) {
  color: rgba(255, 255, 255, 0.84);
}

.side-bottom {
  margin-top: 12px;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.06);

  p {
    margin-top: 10px;
    color: rgba(255, 255, 255, 0.6);
    font-size: 0.88rem;
    line-height: 1.6;
  }
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(31, 140, 139, 0.18);
  color: #9ef0dd;
  font-weight: 700;
}

.status-pill.closed {
  background: rgba(239, 143, 53, 0.2);
  color: #ffd2a6;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.main-shell {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  padding: 18px;
  background: rgba(255, 251, 246, 0.58);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.72);
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 6px 6px 20px;
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-toggle,
.shop-status-chip {
  border: none;
  cursor: pointer;
}

.collapse-toggle {
  width: 48px;
  height: 48px;
  border-radius: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.76);
  color: var(--text-main);
  box-shadow: inset 0 0 0 1px rgba(23, 50, 57, 0.06);

  .el-icon {
    font-size: 1.25rem;
  }
}

.title-block {
  .eyebrow {
    display: inline-block;
    padding: 6px 10px;
    border-radius: 999px;
    background: var(--brand-soft);
    color: var(--brand-deep);
    font-size: 0.78rem;
    font-weight: 700;
    margin-bottom: 8px;
  }

  h1 {
    font-size: clamp(1.35rem, 2vw, 2rem);
    letter-spacing: -0.03em;
  }
}

.shop-status-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  height: 44px;
  border-radius: 999px;
  background: #fff7ef;
  color: var(--brand-deep);
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(239, 143, 53, 0.18);
}

.chip-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 14px 8px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: inset 0 0 0 1px rgba(23, 50, 57, 0.06);
  cursor: pointer;
}

.user-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--accent), #52b6b5);
  color: #fff;
  font-weight: 800;
}

.user-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.2;

  strong {
    font-size: 0.94rem;
  }

  span {
    color: var(--text-sub);
    font-size: 0.8rem;
  }
}

.content-shell {
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

.layout-footer {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 8px 4px;
  color: var(--text-sub);
  font-size: 0.85rem;
}

.status-radio-group {
  width: 100%;
  display: grid;
  gap: 12px;
}

.status-radio {
  margin-right: 0;
  width: 100%;
  min-height: 88px;
  padding: 16px 18px;
  border-radius: 18px;
  background: #fffaf4;
  border: 1px solid rgba(239, 143, 53, 0.12);
}

.status-radio-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
  white-space: normal;

  strong {
    color: var(--text-main);
  }

  span {
    color: var(--text-sub);
    line-height: 1.6;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 1100px) {
  .layout-grid {
    grid-template-columns: 88px minmax(0, 1fr);
  }

  .side-panel {
    padding-inline: 14px;
  }

  .brand-copy,
  .brand-intro,
  .side-bottom,
  .menu-copy small {
    display: none;
  }
}

@media (max-width: 768px) {
  .layout-shell {
    padding: 10px;
  }

  .layout-grid {
    grid-template-columns: 1fr;
  }

  .side-panel {
    display: none;
  }

  .main-shell {
    border-radius: 24px;
  }

  .topbar,
  .layout-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .topbar-right {
    width: 100%;
    justify-content: space-between;
    flex-wrap: wrap;
  }
}
</style>

<style lang="less">
.status-radio.is-checked {
  border-color: rgba(239, 143, 53, 0.4);
  box-shadow: 0 14px 24px rgba(239, 143, 53, 0.12);
}

.status-radio .el-radio__input {
  align-self: flex-start;
  margin-top: 2px;
}

.status-radio .el-radio__label {
  width: calc(100% - 26px);
}
</style>
