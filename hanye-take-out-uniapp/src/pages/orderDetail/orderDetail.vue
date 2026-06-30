<template>
  <view class="white_box">
    <view class="orderDetail">{{ statusText }}</view>
    <view class="time_box" v-if="order.status === 1">
      <view class="time" v-if="isExpired">订单已超时</view>
      <view class="time" v-else>
        支付剩余时间
        <uni-countdown color="#888" :show-day="false" :show-hour="false" :minute="countdownStore.showM" :second="countdownStore.showS" />
      </view>
    </view>
    <view class="btn_box">
      <view class="reOrder" v-if="Number(order.status) <= 2" @click="cancelOrder">取消订单</view>
      <view class="toPay" v-if="order.status === 1 && !isExpired" @click="toPay">立即支付</view>
      <view class="pushOrder" v-if="order.status === 2" @click="pushOrder">催单</view>
      <view class="reOrder" v-if="order.status === 2 || order.status === 6" @click="reOrder">再来一单</view>
    </view>
  </view>

  <view class="white_box">
    <view class="word_text">
      <text class="word_style">嘉园餐厅</text>
    </view>
    <view class="order-type">
      <view class="type_item" v-for="(obj, index) in order.orderDetailList" :key="index">
        <view class="dish_img">
          <image mode="aspectFill" :src="obj.pic" class="dish_img_url"></image>
        </view>
        <view class="dish_info">
          <view class="dish_name">{{ obj.name }}</view>
          <view v-if="obj.dishFlavor" class="dish_flavor">{{ obj.dishFlavor }}</view>
          <view class="dish_amount">
            <text v-if="obj.number && obj.number > 0" class="dish_number">x {{ obj.number }}</text>
          </view>
          <view class="dish_price"><text class="ico">￥</text> {{ obj.amount }}</view>
        </view>
      </view>
      <view class="word_text">
        <view class="word_left">打包费</view>
        <view class="word_right">￥{{ order.packAmount || 0 }}</view>
      </view>
      <view class="word_text">
        <view class="word_left">配送费</view>
        <view class="word_right">￥6</view>
      </view>
      <view class="all_price">
        <text class="word_right">总价 ￥{{ order.amount || 0 }}</text>
      </view>
    </view>
  </view>

  <view class="white_box">
    <view class="text_center" @click="connectShop">联系商家</view>
  </view>

  <view class="white_box">
    <view class="bottom_text">
      <view class="text_left">备注</view>
      <view class="text_right">{{ order.remark || '无' }}</view>
    </view>
    <view class="bottom_text">
      <view class="text_left">餐具份数</view>
      <view class="text_right">{{ tablewareText }}</view>
    </view>
    <view class="bottom_text">
      <view class="text_left">发票</view>
      <view class="text_right">本店不支持线上发票，请联系商家处理</view>
    </view>
  </view>

  <view class="white_box">
    <view class="bottom_text">
      <view class="text_left">订单号</view>
      <view class="text_right">{{ order.number }}</view>
    </view>
    <view class="bottom_text">
      <view class="text_left">下单时间</view>
      <view class="text_right">{{ order.orderTime }}</view>
    </view>
    <view class="bottom_text">
      <view class="text_left">地址</view>
      <view class="text_right">{{ order.address }}</view>
    </view>
  </view>

  <pushMsg ref="childComp"></pushMsg>
</template>

<script lang="ts" setup>
import {computed, reactive, ref} from 'vue'
import {onLoad, onUnload} from '@dcloudio/uni-app'
import pushMsg from '../../components/message/pushMsg.vue'
import {cancelOrderAPI, getOrderAPI, reOrderAPI, urgeOrderAPI} from '@/api/order'
import {cleanCartAPI} from '@/api/cart'
import {useCountdownStore} from '@/stores/modules/countdown'
import type {OrderVO} from '@/types/order'

const childComp: any = ref(null)
const countdownStore = useCountdownStore()

const statusMap: Record<number, string> = {
  0: '全部订单',
  1: '等待支付',
  2: '等待商家接单',
  3: '商家已接单',
  4: '正在配送中',
  5: '订单已完成',
  6: '订单已取消',
}

const order = reactive<OrderVO>({
  id: 0,
  number: '',
  status: 0,
  addressBookId: 0,
  orderTime: new Date(),
  orderDetailList: [],
})

const statusText = computed(() => statusMap[Number(order.status)] || '订单详情')
const tablewareText = computed(() => {
  if (order.tablewareNumber === -1) return '无需餐具'
  if (order.tablewareNumber === 0) return '按餐量提供'
  return order.tablewareNumber || 0
})
const isExpired = computed(() => countdownStore.showM <= 0 && countdownStore.showS <= 0)

const clearCountdown = () => {
  if (countdownStore.timer !== undefined) {
    clearInterval(countdownStore.timer)
    countdownStore.timer = undefined
  }
}

const startCountdown = () => {
  clearCountdown()

  const update = async () => {
    const createdAt = new Date(String(order.orderTime).replace(' ', 'T')).getTime()
    const remain = createdAt + 15 * 60 * 1000 - Date.now()

    if (remain <= 0) {
      clearCountdown()
      countdownStore.showM = 0
      countdownStore.showS = 0
      if (order.status === 1) {
        await cancelOrderAPI(order.id as number)
        await getOrderDetail()
      }
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

const getOrderDetail = async () => {
  const res = await getOrderAPI(order.id as number)
  Object.assign(order, res.data)

  if (order.status === 1) {
    startCountdown()
  } else {
    clearCountdown()
    countdownStore.showM = 0
    countdownStore.showS = 0
  }
}

onLoad(async (options) => {
  order.id = Number(options?.orderId || 0)
  await getOrderDetail()
})

onUnload(() => {
  clearCountdown()
})

const cancelOrder = async () => {
  const res = await cancelOrderAPI(order.id as number)
  if (res.code !== 0) {
    uni.showModal({
      title: '提示',
      content: '商家已接单，如需取消订单请联系商家',
      showCancel: false,
    })
  }
  await getOrderDetail()
}

const pushOrder = async () => {
  await urgeOrderAPI(order.id as number)
  childComp.value?.openPopup()
}

const reOrder = async () => {
  await cleanCartAPI()
  await reOrderAPI(order.id as number)
  uni.redirectTo({
    url: '/pages/order/order',
  })
}

const connectShop = () => {
  uni.makePhoneCall({
    phoneNumber: '1999',
  })
}

const toPay = () => {
  clearCountdown()
  uni.redirectTo({
    url:
      '/pages/pay/pay?orderId=' +
      order.id +
      '&orderNumber=' +
      encodeURIComponent(order.number as string) +
      '&orderAmount=' +
      order.amount +
      '&orderTime=' +
      encodeURIComponent(String(order.orderTime)),
  })
}
</script>

<style lang="less" scoped>
.white_box {
  margin: 20rpx;
  background-color: #fff;
  border-radius: 20rpx;
  .orderDetail {
    padding: 20rpx 0;
    font-size: 36rpx;
    color: #333333;
    font-weight: bold;
    text-align: center;
  }
  .time_box {
    padding: 20rpx 0;
    font-size: 24rpx;
    color: #333333;
    text-align: center;
    .time {
      display: flex;
      justify-content: center;
      font-size: 32rpx;
      color: #666666;
      font-weight: bold;
    }
  }
  .btn_box {
    display: flex;
    justify-content: center;
    .reOrder {
      width: 25%;
      padding: 15rpx 0;
      border: 1px solid #cccccc;
      border-radius: 10rpx;
      margin: 15rpx 10rpx;
      font-size: 28rpx;
      color: #333333;
      text-align: center;
    }
    .toPay,
    .pushOrder {
      width: 25%;
      padding: 15rpx 0;
      border: 1px solid #22ccff;
      background-color: #22ccff;
      border-radius: 10rpx;
      margin: 15rpx 10rpx;
      font-size: 28rpx;
      color: #ffffff;
      text-align: center;
    }
  }
  .order-type {
    padding: 40rpx 0 10rpx 0;
    .type_item {
      display: flex;
      margin-bottom: 30rpx;
      .dish_img {
        width: 100rpx;
        margin: 0 20rpx 0 32rpx;
        .dish_img_url {
          display: block;
          width: 100rpx;
          height: 100rpx;
          border-radius: 8rpx;
        }
      }
      .dish_info {
        position: relative;
        flex: 1;
        margin-right: 20rpx;
        .dish_name {
          font-size: 30rpx;
          font-weight: bold;
          color: #20232a;
        }
        .dish_flavor {
          font-size: 24rpx;
          color: #818693;
          height: 30rpx;
          line-height: 30rpx;
          margin-top: 10rpx;
        }
        .dish_amount {
          font-size: 24rpx;
          color: #818693;
          height: 30rpx;
          line-height: 30rpx;
          margin-top: 10rpx;
          .dish_number {
            padding: 10rpx 0;
            font-size: 24rpx;
          }
        }
        .dish_price {
          position: absolute;
          right: 20rpx;
          bottom: 40rpx;
          display: flex;
          font-size: 32rpx;
          color: #e94e3c;
          font-family: DIN, DIN-Medium;
          font-weight: 500;
          .ico {
            line-height: 42rpx;
            font-size: 24rpx;
          }
        }
      }
    }
  }
  .text_center {
    text-align: center;
    font-size: 32rpx;
    color: #333333;
    font-weight: bold;
    padding: 20rpx 0;
  }
  .word_text {
    display: flex;
    align-items: center;
    margin: 0 20rpx 0 30rpx;
    border-bottom: 1px solid #efefef;
    height: 120rpx;
    line-height: 120rpx;
    .word_left {
      width: 50%;
      height: 44rpx;
      opacity: 1;
      font-size: 32rpx;
      text-align: left;
      color: #333333;
      line-height: 44rpx;
      letter-spacing: 0px;
    }
    .word_right {
      width: 50%;
      height: 44rpx;
      opacity: 1;
      font-size: 32rpx;
      text-align: right;
      color: #333333;
      line-height: 44rpx;
      letter-spacing: 0px;
      padding-right: 20rpx;
    }
  }
  .all_price {
    position: relative;
    margin: 0 16rpx 0 22rpx;
    height: 120rpx;
    line-height: 120rpx;
    .word_right {
      position: absolute;
      height: 44rpx;
      opacity: 1;
      font-size: 32rpx;
      text-align: left;
      color: #333333;
      line-height: 44rpx;
      letter-spacing: 0px;
      top: 30rpx;
      right: 28rpx;
    }
  }
  .bottom_text {
    display: flex;
    align-items: center;
    margin: 0 20rpx 0 30rpx;
    height: 100rpx;
    line-height: 100rpx;
    .text_left {
      width: 30%;
      height: 44rpx;
      opacity: 1;
      font-size: 32rpx;
      text-align: left;
      color: #333333;
      line-height: 44rpx;
      letter-spacing: 0px;
    }
    .text_right {
      width: 70%;
      height: 44rpx;
      font-size: 24rpx;
      text-align: right;
      color: #666666;
      line-height: 44rpx;
      letter-spacing: 0px;
      padding-right: 20rpx;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}
</style>

<style>
page {
  background-color: #f8f8f8;
}
</style>
