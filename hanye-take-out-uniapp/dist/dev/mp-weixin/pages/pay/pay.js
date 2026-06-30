"use strict";
const common_vendor = require("../../common/vendor.js");
const api_order = require("../../api/order.js");
const stores_modules_countdown = require("../../stores/modules/countdown.js");
require("../../utils/http.js");
require("../../stores/modules/user.js");
if (!Array) {
  const _easycom_uni_countdown2 = common_vendor.resolveComponent("uni-countdown");
  _easycom_uni_countdown2();
}
const _easycom_uni_countdown = () => "../../node-modules/@dcloudio/uni-ui/lib/uni-countdown/uni-countdown.js";
if (!Math) {
  _easycom_uni_countdown();
}
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "pay",
  setup(__props) {
    const countdownStore = stores_modules_countdown.useCountdownStore();
    const orderId = common_vendor.ref(0);
    const orderNumber = common_vendor.ref("");
    const orderAmount = common_vendor.ref(0);
    const orderTime = common_vendor.ref("");
    const isExpired = common_vendor.computed(() => countdownStore.showM <= 0 && countdownStore.showS <= 0);
    const clearCountdown = () => {
      if (countdownStore.timer !== void 0) {
        clearInterval(countdownStore.timer);
        countdownStore.timer = void 0;
      }
    };
    const cancelExpiredOrder = async () => {
      if (orderId.value) {
        await api_order.cancelOrderAPI(orderId.value);
      }
    };
    const startCountdown = () => {
      clearCountdown();
      const update = async () => {
        const createdAt = new Date(orderTime.value.replace(" ", "T")).getTime();
        const remain = createdAt + 15 * 60 * 1e3 - Date.now();
        if (remain <= 0) {
          clearCountdown();
          countdownStore.showM = 0;
          countdownStore.showS = 0;
          await cancelExpiredOrder();
          return;
        }
        countdownStore.showM = Math.floor(remain / 1e3 / 60 % 60);
        countdownStore.showS = Math.floor(remain / 1e3 % 60);
      };
      void update();
      countdownStore.timer = setInterval(() => {
        void update();
      }, 1e3);
    };
    common_vendor.onLoad((options) => {
      orderId.value = Number((options == null ? void 0 : options.orderId) || 0);
      orderNumber.value = decodeURIComponent((options == null ? void 0 : options.orderNumber) || "");
      orderAmount.value = Number((options == null ? void 0 : options.orderAmount) || 0);
      orderTime.value = decodeURIComponent((options == null ? void 0 : options.orderTime) || "");
      startCountdown();
    });
    common_vendor.onUnload(() => {
      clearCountdown();
    });
    const toSuccess = async () => {
      if (isExpired.value) {
        common_vendor.index.redirectTo({
          url: "/pages/orderDetail/orderDetail?orderId=" + orderId.value
        });
        return;
      }
      const payDTO = {
        orderNumber: orderNumber.value,
        payMethod: 1
      };
      const res = await api_order.payOrderAPI(payDTO);
      if (res.code !== 0) {
        common_vendor.index.showToast({
          title: res.msg || "支付失败",
          icon: "none"
        });
        return;
      }
      clearCountdown();
      common_vendor.index.redirectTo({
        url: "/pages/submit/success?orderId=" + orderId.value + "&orderNumber=" + encodeURIComponent(orderNumber.value) + "&orderAmount=" + orderAmount.value + "&orderTime=" + encodeURIComponent(orderTime.value)
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: isExpired.value
      }, isExpired.value ? {} : {
        b: common_vendor.p({
          color: "#888",
          ["show-day"]: false,
          ["show-hour"]: false,
          minute: common_vendor.unref(countdownStore).showM,
          second: common_vendor.unref(countdownStore).showS
        })
      }, {
        c: common_vendor.t(orderAmount.value),
        d: common_vendor.t(orderNumber.value),
        e: common_vendor.o(toSuccess)
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-8a6251df"], ["__file", "D:/small-third/project1/hanye-take-out-main/hanye-take-out-uniapp/src/pages/pay/pay.vue"]]);
wx.createPage(MiniProgramPage);
