<template>
  <view class="pay_box">
    <view class="time" v-if="isExpired">订单已超时</view>
    <view class="time" v-else>
      支付剩余时间
      <uni-countdown color="#888" :show-day="false" :show-hour="false" :minute="countdownStore.showM" :second="countdownStore.showS" />
    </view>
    <view class="price">￥{{ orderAmount }}</view>
    <view class="shop">嘉园餐厅 - {{ orderNumber }}</view>
    <view class="wechat">
      <image class="pay" src="../../static/icon/pay.png" />
      微信支付
      <image class="choose" src="../../static/icon/choose.png" />
    </view>
    <view class="bottom">
      <button class="comfirm_btn" type="primary" :plain="true" @click="toSuccess">确认支付</button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import {computed, ref} from 'vue'
import {onLoad, onUnload} from '@dcloudio/uni-app'
import {cancelOrderAPI, payOrderAPI} from '@/api/order'
import {useCountdownStore} from '@/stores/modules/countdown'

const countdownStore = useCountdownStore()

const orderId = ref(0)
const orderNumber = ref('')
const orderAmount = ref(0)
const orderTime = ref('')
const isExpired = computed(() => countdownStore.showM <= 0 && countdownStore.showS <= 0)

const clearCountdown = () => {
  if (countdownStore.timer !== undefined) {
    clearInterval(countdownStore.timer)
    countdownStore.timer = undefined
  }
}

const cancelExpiredOrder = async () => {
  if (orderId.value) {
    await cancelOrderAPI(orderId.value)
  }
}

const startCountdown = () => {
  clearCountdown()

  const update = async () => {
    const createdAt = new Date(orderTime.value.replace(' ', 'T')).getTime()
    const remain = createdAt + 15 * 60 * 1000 - Date.now()

    if (remain <= 0) {
      clearCountdown()
      countdownStore.showM = 0
      countdownStore.showS = 0
      await cancelExpiredOrder()
      return
    }

    countdownStore.showM = Math.floor((remain / 1000 / 60) % 60)
    countdownStore.showS = Math.floor((remain / 1000) % 60)
  }

  void update()
  countdownStore.timer = setInterval(() => {
    void update()
  }, 1000) as unknown as number
}

onLoad((options: any) => {
  orderId.value = Number(options?.orderId || 0)
  orderNumber.value = decodeURIComponent(options?.orderNumber || '')
  orderAmount.value = Number(options?.orderAmount || 0)
  orderTime.value = decodeURIComponent(options?.orderTime || '')
  startCountdown()
})

onUnload(() => {
  clearCountdown()
})

const toSuccess = async () => {
  if (isExpired.value) {
    uni.redirectTo({
      url: '/pages/orderDetail/orderDetail?orderId=' + orderId.value,
    })
    return
  }

  const payDTO = {
    orderNumber: orderNumber.value,
    payMethod: 1,
  }

  const res = await payOrderAPI(payDTO)
  if (res.code !== 0) {
    uni.showToast({
      title: res.msg || '支付失败',
      icon: 'none',
    })
    return
  }

  clearCountdown()
  uni.redirectTo({
    url:
      '/pages/submit/success?orderId=' +
      orderId.value +
      '&orderNumber=' +
      encodeURIComponent(orderNumber.value) +
      '&orderAmount=' +
      orderAmount.value +
      '&orderTime=' +
      encodeURIComponent(orderTime.value),
  })
}
</script>

<style lang="less" scoped>
.pay_box {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #333;
  .time {
    display: flex;
    margin-top: 100rpx;
    color: #888;
    font-size: 28rpx;
  }
  .price {
    font-size: 80rpx;
    font-weight: bold;
    margin-top: 20rpx;
  }
  .shop {
    display: flex;
    margin-top: 20rpx;
    font-size: 28rpx;
    color: #888;
  }
  .wechat {
    display: flex;
    width: 90%;
    height: 80rpx;
    line-height: 80rpx;
    background-color: #fff;
    border-radius: 10rpx;
    margin: 100rpx 20rpx;
    position: relative;
    .pay {
      width: 40rpx;
      height: 40rpx;
      padding: 20rpx;
    }
    .choose {
      position: absolute;
      width: 40rpx;
      height: 40rpx;
      top: 20rpx;
      right: 20rpx;
    }
  }
}

.bottom {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 100rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  .comfirm_btn {
    position: absolute;
    bottom: 30rpx;
    width: 600rpx;
    height: 80rpx;
    line-height: 80rpx;
    border-radius: 40rpx;
    background: #00aaff;
    border: none;
    color: #fff;
    font-size: 30rpx;
    text-align: center;
  }
}
</style>

<style>
page {
  background-color: #f8f8f8;
}
</style>
