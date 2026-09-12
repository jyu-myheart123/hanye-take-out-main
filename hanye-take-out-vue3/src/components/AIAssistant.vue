<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Close, Microphone, Promotion } from '@element-plus/icons-vue'
import {
  getDineInCategoryListAPI,
  getDineInDishListAPI,
  dineInSubmitAPI,
} from '@/api/dinein'
import { exportInforAPI, getDataOverViewAPI } from '@/api/statistics'
import { fixStatusAPI, getStatusAPI } from '@/api/shop'

/**
 * AI 助手组件 —— 悬浮在所有页面右下角
 * 小白讲解：这是一个"智能员工小帮手"，员工可以用语音或文字跟它说需求，
 * 它能帮你快速开堂食订单、导出报表、切换营业状态、跳转页面等。
 * 注意：本助手使用【浏览器原生语音识别】+【本地规则引擎】，不需要调用外部大模型，零成本。
 */

const router = useRouter()

// ==================== 基础状态 ====================
// 面板是否展开
const panelVisible = ref(false)
// 输入框文字
const inputText = ref('')
// 消息列表（对话历史）
interface Msg {
  id: number
  role: 'user' | 'ai'
  content: string
  draft?: DraftItem[]
}
const messages = ref<Msg[]>([])
let msgId = 0

// 消息体滚动容器
const bodyRef = ref<HTMLElement | null>(null)

// ==================== 菜品库（AI 识别菜品用）====================
interface Dish {
  id: number
  name: string
  price: number
  image?: string
}
const dishList = ref<Dish[]>([])

/**
 * 加载所有启售菜品到内存，供 AI 匹配菜名用
 * 小白讲解：把菜单全装进"大脑"，用户说"宫保鸡丁"时就能快速找到对应的菜品id和价格
 */
const loadDishList = async () => {
  try {
    const res = await getDineInDishListAPI()
    if (res.data.code === 0) {
      dishList.value = res.data.data || []
    }
  } catch (e) {
    console.warn('AI助手加载菜品库失败', e)
  }
}

// ==================== 语音识别（浏览器原生 Web Speech API）====================
const recording = ref(false)
// 语音识别对象（Chrome/Edge 支持，Firefox 不支持）
let recognition: any = null

/**
 * 初始化语音识别
 * 小白讲解：有些浏览器不支持语音识别，这里做了容错，不支持就只能用文字输入
 */
const initSpeech = () => {
  const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  if (!SR) return
  recognition = new SR()
  recognition.lang = 'zh-CN' // 中文识别
  recognition.interimResults = false // 只返回最终结果，不要中间态
  recognition.continuous = false // 说一句停一句
  // 识别成功时，把文字塞进输入框
  recognition.onresult = (event: any) => {
    const text = event.results[0][0].transcript
    inputText.value = (inputText.value + text).trim()
  }
  recognition.onerror = () => {
    ElMessage.warning('语音识别失败，请改用文字输入')
    recording.value = false
  }
  recognition.onend = () => {
    recording.value = false
  }
}

/**
 * 点击麦克风按钮：开始/停止录音
 */
const toggleRecord = () => {
  if (!recognition) {
    ElMessage.warning('当前浏览器不支持语音识别，请用 Chrome 或 Edge 浏览器，或直接文字输入')
    return
  }
  if (recording.value) {
    recognition.stop()
    recording.value = false
  } else {
    recognition.start()
    recording.value = true
  }
}

// ==================== 规则引擎：解析用户输入 ====================
/**
 * 订单草稿中的一项
 */
interface DraftItem {
  dishId: number
  name: string
  price: number
  number: number
}

// 当前订单草稿（AI 解析出来的，等待员工审核）
const draftItems = ref<DraftItem[]>([])
// 草稿对应的桌号
const draftTableNo = ref('')
// 这条草稿挂在哪条 AI 消息上（方便在消息气泡里展示）
const draftMsgId = ref<number | null>(null)

/**
 * 从一句话里解析出【桌号】
 * 支持：A3桌、3号桌、桌号A3、3桌、包间1 等
 */
const parseTableNo = (text: string): string => {
  // 匹配 "桌号 A3"、"A3桌"、"3号桌" 等形式
  const m1 = text.match(/桌号?\s*[:：]?\s*([A-Za-z]?\d+)/)
  if (m1) return m1[1]
  const m2 = text.match(/([A-Za-z]?\d+)\s*号?桌/)
  if (m2) return m2[1]
  const m3 = text.match(/([A-Za-z]?\d+)\s*包间/)
  if (m3) return m3[1] + '包间'
  return ''
}

/**
 * 从一句话里解析出【菜品 + 数量】
 * 思路：遍历菜品库，看菜名是否出现在输入里；
 *       找到菜名后，在菜名前面或后面找数字作为数量（默认1份）
 */
const parseDishes = (text: string): DraftItem[] => {
  const items: DraftItem[] = []
  for (const dish of dishList.value) {
    const idx = text.indexOf(dish.name)
    if (idx === -1) continue
    // 找菜名附近的数字（前后各 6 个字符范围内）
    const around = text.slice(Math.max(0, idx - 6), idx + dish.name.length + 6)
    const numMatch = around.match(/(\d+)/)
    const number = numMatch ? parseInt(numMatch[1]) : 1
    items.push({
      dishId: dish.id,
      name: dish.name,
      price: dish.price,
      number,
    })
  }
  return items
}

/**
 * 核心：处理用户输入，判断是【指令】还是【开单需求】
 */
const handleInput = async (raw: string) => {
  const text = raw.trim()
  if (!text) return

  // 先把用户消息加到对话里
  pushMessage('user', text)

  // ---------- 指令1：导出/打印报表 ----------
  if (/报表|导出|打印.*数据|经营数据/.test(text)) {
    await handleExportReport()
    return
  }

  // ---------- 指令2：切换营业状态 ----------
  if (/打烊|营业|关门|开门/.test(text)) {
    await handleToggleShop(text)
    return
  }

  // ---------- 指令3：跳转页面 ----------
  const jumpMatch = text.match(/(?:去|打开|跳转|到|进入)\s*(.+)/)
  if (jumpMatch) {
    handleJump(jumpMatch[1])
    return
  }

  // ---------- 指令4：数据概览/营业额 ----------
  if (/营业额|概览|今天.*数据|经营概况/.test(text)) {
    await handleOverview()
    return
  }

  // ---------- 否则：尝试解析为堂食开单 ----------
  const items = parseDishes(text)
  const tableNo = parseTableNo(text)

  if (items.length === 0) {
    // 没识别到菜品，给一些引导
    pushMessage('ai', '我没听清楚您要点的菜。您可以这样说：\n"A3桌，宫保鸡丁2份，米饭3碗"\n"帮我导出最近的经营报表"\n"切换为打烊状态"')
    return
  }

  // 识别到菜品，生成草稿
  draftItems.value = items
  draftTableNo.value = tableNo
  const total = items.reduce((s, i) => s + i.price * i.number, 0)

  let reply = `已为您识别到 ${items.length} 种菜品，合计 ¥${total.toFixed(2)}。`
  if (!tableNo) {
    reply += '⚠️ 缺少桌号，请在下方补充桌号后再提交。'
  } else {
    reply += `桌号：${tableNo}。请核对菜品和数量，确认无误后点击「确认下单」。`
  }
  pushMessage('ai', reply, items)
}

// ==================== 指令处理函数 ====================

/**
 * 导出经营报表（Excel）
 * 小白讲解：直接调用后端的导出接口，拿到二进制文件后触发浏览器下载
 */
const handleExportReport = async () => {
  pushMessage('ai', '正在为您导出经营报表，请稍候...')
  try {
    const res: any = await exportInforAPI()
    // res.data 是 blob 二进制数据
    const blob = new Blob([res.data], { type: 'application/vnd.ms-excel' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `经营报表_${new Date().toISOString().slice(0, 10)}.xls`
    link.click()
    URL.revokeObjectURL(url)
    pushMessage('ai', '✅ 报表已导出并开始下载！')
  } catch (e) {
    pushMessage('ai', '❌ 报表导出失败，请稍后重试。')
  }
}

/**
 * 切换营业状态
 */
const handleToggleShop = async (text: string) => {
  const wantClose = /打烊|关门/.test(text)
  const target = wantClose ? 0 : 1
  pushMessage('ai', `正在为您切换为「${wantClose ? '打烊' : '营业中'}」状态...`)
  try {
    const res = await fixStatusAPI(target)
    if (res.data.code === 0) {
      pushMessage('ai', `✅ 门店状态已切换为「${wantClose ? '打烊中' : '营业中'}」`)
    } else {
      pushMessage('ai', '❌ 状态切换失败：' + (res.data.msg || '未知错误'))
    }
  } catch (e) {
    pushMessage('ai', '❌ 状态切换失败，请稍后重试。')
  }
}

/**
 * 跳转到指定页面（根据菜单名模糊匹配）
 */
const menuMap: Record<string, string> = {
  控制台: '/dashboard',
  首页: '/dashboard',
  数据统计: '/statistics',
  统计: '/statistics',
  报表: '/statistics',
  堂食: '/dinein',
  开单: '/dinein',
  订单: '/order',
  分类: '/category',
  菜品: '/dish',
  套餐: '/setmeal',
  员工: '/employee',
}
const handleJump = (keyword: string) => {
  for (const key in menuMap) {
    if (keyword.includes(key)) {
      pushMessage('ai', `正在为您跳转到「${key}」页面...`)
      setTimeout(() => router.push(menuMap[key]), 300)
      return
    }
  }
  pushMessage('ai', '抱歉，没有找到对应的页面。您可以说：去订单管理、去菜品管理、去员工管理等。')
}

/**
 * 查询今日数据概览
 */
const handleOverview = async () => {
  pushMessage('ai', '正在查询今日经营概览...')
  try {
    const res = await getDataOverViewAPI({})
    if (res.data.code === 0) {
      const d = res.data.data
      const lines = [
        `📊 今日经营概览：`,
        `营业额：¥${d.turnover?.toFixed(2) ?? '0.00'}`,
        `订单数：${d.orderCount ?? 0} 单`,
        `新增用户：${d.newUser ?? 0} 人`,
      ]
      pushMessage('ai', lines.join('\n'))
    } else {
      pushMessage('ai', '❌ 查询失败：' + (res.data.msg || '未知错误'))
    }
  } catch (e) {
    pushMessage('ai', '❌ 查询失败，请稍后重试。')
  }
}

// ==================== 订单草稿：审核与提交 ====================

/**
 * 草稿总金额
 */
const draftTotal = computed(() =>
  draftItems.value.reduce((s, i) => s + i.price * i.number, 0)
)

/**
 * 草稿是否完整（桌号必填，至少有一个菜品）
 */
const draftComplete = computed(() =>
  draftTableNo.value.trim() !== '' && draftItems.value.length > 0
)

/**
 * 草稿里增减某菜品的数量
 */
const changeDraftNumber = (idx: number, delta: number) => {
  draftItems.value[idx].number += delta
  if (draftItems.value[idx].number < 1) draftItems.value[idx].number = 1
}

/**
 * 从草稿里移除某个菜品
 */
const removeDraftItem = (idx: number) => {
  draftItems.value.splice(idx, 1)
}

/**
 * 员工审核完毕，确认下单
 * 小白讲解：这里调的是堂食开单接口，价格由后端重新核算，前端传的金额不作数
 */
const submitDraft = async () => {
  if (!draftComplete.value) {
    ElMessage.warning('请先补充桌号并确保有菜品')
    return
  }
  try {
    const res = await dineInSubmitAPI({
      tableNo: draftTableNo.value,
      phone: '',
      remark: 'AI助手代客下单',
      items: draftItems.value.map((i) => ({ dishId: i.dishId, number: i.number })),
    })
    if (res.data.code === 0) {
      pushMessage(
        'ai',
        `✅ 下单成功！订单号：${res.data.data.orderNumber}，应付金额：¥${res.data.data.orderAmount}`
      )
      // 清空草稿
      draftItems.value = []
      draftTableNo.value = ''
      draftMsgId.value = null
    } else {
      pushMessage('ai', '❌ 下单失败：' + (res.data.msg || '未知错误'))
    }
  } catch (e) {
    pushMessage('ai', '❌ 下单失败，请稍后重试。')
  }
}

// ==================== 消息辅助方法 ====================
const pushMessage = (role: 'user' | 'ai', content: string, draft?: DraftItem[]) => {
  const id = ++msgId
  messages.value.push({ id, role, content, draft })
  // 把草稿挂到这条 AI 消息上
  if (role === 'ai' && draft) {
    draftMsgId.value = id
  }
  // 滚动到底部
  nextTick(() => {
    if (bodyRef.value) {
      bodyRef.value.scrollTop = bodyRef.value.scrollHeight
    }
  })
}

/**
 * 发送消息
 */
const send = () => {
  const text = inputText.value
  inputText.value = ''
  handleInput(text)
}

/**
 * 展开/收起面板
 */
const togglePanel = () => {
  panelVisible.value = !panelVisible.value
  // 第一次展开时加载菜品库
  if (panelVisible.value && dishList.value.length === 0) {
    loadDishList()
    pushMessage(
      'ai',
      '您好！我是嘉园外卖 AI 助手 🤖\n您可以对我说：\n• "A3桌，宫保鸡丁2份，米饭3碗"\n• "导出经营报表"\n• "切换为打烊状态"\n• "去订单管理"'
    )
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  initSpeech()
})
</script>

<template>
  <!-- 悬浮球：固定在右下角，不遮挡主体内容 -->
  <div class="ai-fab" :class="{ active: panelVisible }" @click="togglePanel" title="AI 助手">
    <el-icon :size="24"><ChatDotRound /></el-icon>
    <!-- 录音时的小红点 -->
    <span v-if="recording" class="ai-fab__dot"></span>
  </div>

  <!-- 对话面板 -->
  <transition name="ai-fade">
    <div v-if="panelVisible" class="ai-panel">
      <!-- 头部 -->
      <div class="ai-panel__header">
        <div class="ai-panel__title">
          <el-icon><Promotion /></el-icon>
          <span>AI 助手</span>
          <el-tag v-if="recording" type="danger" size="small" effect="dark">录音中</el-tag>
        </div>
        <el-icon class="ai-panel__close" @click.stop="panelVisible = false"><Close /></el-icon>
      </div>

      <!-- 消息区 -->
      <div class="ai-panel__body" ref="bodyRef">
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['ai-msg', msg.role]"
        >
          <div class="ai-msg__bubble">{{ msg.content }}</div>

          <!-- 订单草稿卡片：挂在对应的 AI 消息下 -->
          <div v-if="msg.draft && msg.id === draftMsgId" class="ai-draft">
            <div class="ai-draft__head">
              <span>📝 订单草稿</span>
              <span class="ai-draft__total">合计 ¥{{ draftTotal.toFixed(2) }}</span>
            </div>

            <!-- 桌号输入 -->
            <div class="ai-draft__row">
              <span class="ai-draft__label">桌号</span>
              <el-input
                v-model="draftTableNo"
                size="small"
                placeholder="请输入桌号，如 A3"
                style="width: 140px"
              />
              <el-tag v-if="!draftTableNo" type="warning" size="small">必填</el-tag>
            </div>

            <!-- 菜品清单 -->
            <div v-for="(item, idx) in draftItems" :key="item.dishId" class="ai-draft__item">
              <span class="ai-draft__name">{{ item.name }}</span>
              <span class="ai-draft__price">¥{{ item.price.toFixed(2) }}</span>
              <div class="ai-draft__num">
                <el-button size="small" circle @click="changeDraftNumber(idx, -1)">-</el-button>
                <span>{{ item.number }}</span>
                <el-button size="small" circle @click="changeDraftNumber(idx, 1)">+</el-button>
              </div>
              <el-button size="small" type="danger" text @click="removeDraftItem(idx)">移除</el-button>
            </div>

            <!-- 提交按钮：不完整时禁用 -->
            <el-button
              type="success"
              size="small"
              :disabled="!draftComplete"
              class="ai-draft__submit"
              @click="submitDraft"
            >
              确认下单
            </el-button>
            <el-button v-if="!draftComplete" size="small" text type="info" disabled>
              请补充桌号后提交
            </el-button>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="ai-panel__footer">
        <el-input
          v-model="inputText"
          placeholder="说出或输入您的需求..."
          @keyup.enter="send"
        />
        <el-button
          :type="recording ? 'danger' : 'primary'"
          circle
          @click="toggleRecord"
          :title="recording ? '停止录音' : '语音输入'"
        >
          <el-icon><Microphone /></el-icon>
        </el-button>
        <el-button type="success" @click="send">发送</el-button>
      </div>
    </div>
  </transition>
</template>

<style lang="less" scoped>
/* 悬浮球：固定在右下角，距离底部和右侧各 30px，不遮挡主体内容 */
.ai-fab {
  position: fixed;
  right: 30px;
  bottom: 30px;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #67c23a);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.4);
  z-index: 2000;
  transition: transform 0.2s;
  &:hover {
    transform: scale(1.1);
  }
  &.active {
    background: linear-gradient(135deg, #f56c6c, #e6a23c);
  }
  /* 录音时的脉动红点 */
  &__dot {
    position: absolute;
    top: 6px;
    right: 6px;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #f56c6c;
    animation: ai-pulse 1s infinite;
  }
}

@keyframes ai-pulse {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.4); opacity: 0.6; }
  100% { transform: scale(1); opacity: 1; }
}

/* 对话面板：出现在悬浮球左上方 */
.ai-panel {
  position: fixed;
  right: 30px;
  bottom: 96px;
  width: 380px;
  height: 560px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  z-index: 2001;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 18px;
    background: linear-gradient(135deg, #409eff, #67c23a);
    color: #fff;
  }
  &__title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 600;
  }
  &__close {
    cursor: pointer;
    font-size: 18px;
    &:hover { opacity: 0.8; }
  }

  /* 消息滚动区 */
  &__body {
    flex: 1;
    overflow-y: auto;
    padding: 14px;
    background: #f5f7fa;
  }

  /* 输入区 */
  &__footer {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px;
    border-top: 1px solid #ebeef5;
    background: #fff;
  }
}

/* 单条消息 */
.ai-msg {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  &.user {
    align-items: flex-end;
  }
  &.ai {
    align-items: flex-start;
  }
  &__bubble {
    max-width: 85%;
    padding: 8px 12px;
    border-radius: 10px;
    font-size: 13px;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-word;
  }
  &.user &__bubble {
    background: #409eff;
    color: #fff;
    border-bottom-right-radius: 2px;
  }
  &.ai &__bubble {
    background: #fff;
    color: #303133;
    border: 1px solid #ebeef5;
    border-bottom-left-radius: 2px;
  }
}

/* 订单草稿卡片 */
.ai-draft {
  width: 100%;
  margin-top: 8px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  padding: 10px;
  font-size: 13px;
  &__head {
    display: flex;
    justify-content: space-between;
    font-weight: 600;
    margin-bottom: 8px;
  }
  &__total {
    color: #f56c6c;
  }
  &__row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }
  &__label {
    color: #909399;
  }
  &__item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 0;
    border-top: 1px dashed #ebeef5;
  }
  &__name {
    flex: 1;
  }
  &__price {
    color: #f56c6c;
    min-width: 60px;
    text-align: right;
  }
  &__num {
    display: flex;
    align-items: center;
    gap: 6px;
    :deep(.el-button) {
      width: 24px;
      height: 24px;
      padding: 0;
    }
  }
  &__submit {
    width: 100%;
    margin-top: 8px;
  }
}

/* 面板展开动画 */
.ai-fade-enter-active,
.ai-fade-leave-active {
  transition: all 0.2s ease;
}
.ai-fade-enter-from,
.ai-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
