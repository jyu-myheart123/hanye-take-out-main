<template>
  <view class="viewport">
    <view class="logo">
      <image src="@/static/images/login.png"></image>
    </view>
    <view class="login">
      <!-- 小程序端授权登录 -->
      <button class="button" @tap="login">微信快捷登录</button>
      <view class="extra">
        <view class="caption">
          <text>其他登录方式</text>
        </view>
        <view class="options">
          <button class="small_btn" @tap="testLogin">测试登录</button>
        </view>
      </view>
      <view class="tips">登录/注册即视为你同意《服务条款》和《嘉园外卖隐私协议》</view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import {loginAPI} from '@/api/login';
import {onLoad} from '@dcloudio/uni-app';
import {useUserStore} from '@/stores/modules/user';
import type {LoginResult} from '@/types/user';

// 先调用wx.login()，获取 code 登录凭证
let code = '';
onLoad(async () => {
  const res = await wx.login();
  code = res.code;
  console.log('微信登录code:', code);
});
// 再携带code发送登录请求
// 获取用户手机号码
const login = async () => {
  console.log('login');
  try {
    // 登录请求
    const res = await loginAPI(code);
    console.log(res);
    // 成功提示
    loginSuccess(res.data);
  } catch (error) {
    console.error('登录失败:', error);
    uni.showToast({
      title: '登录失败，请重试',
      icon: 'none'
    });
  }
};

// 测试登录，使用纯前端模拟
const testLogin = async () => {
  console.log('test login');
  // 模拟登录成功，创建一个模拟用户对象
  const mockUser = {
    id: 1,
    name: '测试用户',
    phone: '13800138000',
    gender: 1,
    pic: '',
    token: 'mock_token',
    openid: 'test_openid_123456'
  };
  loginSuccess(mockUser);
};

const loginSuccess = (profile: LoginResult) => {
  // 保存会员信息
  const userStore = useUserStore();
  userStore.setProfile(profile);
  // 成功提示
  uni.showToast({icon: 'success', title: '登录成功'});
  setTimeout(() => {
    // 页面跳转
    uni.switchTab({url: '/pages/my/my'});
  }, 500);
};

const tips = async () => {
  // 模拟登录
  const mockUser = {
    id: 1,
    name: '测试用户',
    phone: '13800138000',
    gender: 1,
    pic: '',
    token: 'mock_token'
  };
  loginSuccess(mockUser);
};
</script>

<style lang="less" scoped>
page {
  height: 100%;
}

.viewport {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20rpx 40rpx;
}

.logo {
  flex: 1;
  text-align: center;

  image {
    width: 220rpx;
    height: 220rpx;
    margin-top: 15vh;
  }
}

.login {
  display: flex;
  flex-direction: column;
  height: 60vh;
  padding: 40rpx 20rpx 20rpx;

  .input {
    width: 100%;
    height: 80rpx;
    font-size: 28rpx;
    border-radius: 72rpx;
    border: 1px solid #ddd;
    padding-left: 30rpx;
    margin-bottom: 20rpx;
  }

  .button {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 80rpx;
    font-size: 28rpx;
    border-radius: 72rpx;
    background-color: #22ccff;
    color: #fff;
  }

  .extra {
    flex: 1;
    padding: 70rpx 70rpx 0;

    .caption {
      width: 440rpx;
      line-height: 1;
      border-top: 1rpx solid #ddd;
      font-size: 26rpx;
      color: #999;
      position: relative;

      text {
        transform: translate(-40%);
        background-color: #fff;
        position: absolute;
        top: -12rpx;
        left: 50%;
      }
    }

    .options {
      display: flex;
      justify-content: center;
      align-items: center;
      margin-top: 70rpx;

      button {
        padding: 0;
        background-color: transparent;
      }
      .small_btn {
        width: 300rpx;
        height: 80rpx;
        font-size: 28rpx;
        border-radius: 72rpx;
        color: #22ccff;
        border: #22ccff solid 1rpx;
      }
    }
  }
}

.tips {
  position: absolute;
  bottom: 80rpx;
  left: 20rpx;
  right: 20rpx;
  font-size: 22rpx;
  color: #999;
  text-align: center;
}
</style>
