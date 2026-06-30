"use strict";
const common_vendor = require("../../common/vendor.js");
const api_order = require("../../api/order.js");
const api_cart = require("../../api/cart.js");
const stores_modules_countdown = require("../../stores/modules/countdown.js");
require("../../utils/http.js");
require("../../stores/modules/user.js");
if (!Array) {
  const _easycom_uni_countdown2 = common_vendor.resolveComponent("uni-countdown");
  _easycom_uni_countdown2();
}
const _easycom_uni_countdown = () => "../../node-modules/@dcloudio/uni-ui/lib/uni-countdown/uni-countdown.js";
if (!Math) {
  (_easycom_uni_countdown + pushMsg)();
}
const pushMsg = () => "../../components/message/pushMsg.js";
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "orderDetail",
  setup(__props) {
    const childComp = common_vendor.ref(null);
    const countdownStore = stores_modules_countdown.useCountdownStore();
    const statusMap = {
      0: "全部订单",
      1: "等待支付",
      2: "等待商家接单",
      3: "商家已接单",
      4: "正在配送中",
      5: "订单已完成",
      6: "订单已取消"
    };
    const order = common_vendor.reactive({
      id: 0,
      number: "",
      status: 0,
      addressBookId: 0,
      orderTime: /* @__PURE__ */ new Date(),
      orderDetailList: []
    });
    const statusText = common_vendor.computed(() => statusMap[Number(order.status)] || "订单详情");
    const tablewareText = common_vendor.computed(() => {
      if (order.tablewareNumber === -1)
        return "无需餐具";
      if (order.tablewareNumber === 0)
        return "按餐量提供";
      return order.tablewareNumber || 0;
    });
    const isExpired = common_vendor.computed(() => countdownStore.showM <= 0 && countdownStore.showS <= 0);
    const clearCountdown = () => {
      if (countdownStore.timer !== void 0) {
        clearInterval(countdownStore.timer);
        countdownStore.timer = void 0;
      }
    };
    const startCountdown = () => {
      clearCountdown();
      const update = async () => {
        const createdAt = new Date(String(order.orderTime).replace(" ", "T")).getTime();
        const remain = createdAt + 15 * 60 * 1e3 - Date.now();
        if (remain <= 0) {
          clearCountdown();
          countdownStore.showM = 0;
          countdownStore.showS = 0;
          if (order.status === 1) {
            await api_order.cancelOrderAPI(order.id);
            await getOrderDetail();
          }
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
    const getOrderDetail = async () => {
      const res = await api_order.getOrderAPI(order.id);
      Object.assign(order, res.data);
      if (order.status === 1) {
        startCountdown();
      } else {
        clearCountdown();
        countdownStore.showM = 0;
        countdownStore.showS = 0;
      }
    };
    common_vendor.onLoad(async (options) => {
      order.id = Number((options == null ? void 0 : options.orderId) || 0);
      await getOrderDetail();
    });
    common_vendor.onUnload(() => {
      clearCountdown();
    });
    const cancelOrder = async () => {
      const res = await api_order.cancelOrderAPI(order.id);
      if (res.code !== 0) {
        common_vendor.index.showModal({
          title: "提示",
          content: "商家已接单，如需取消订单请联系商家",
          showCancel: false
        });
      }
      await getOrderDetail();
    };
    const pushOrder = async () => {
      var _a;
      await api_order.urgeOrderAPI(order.id);
      (_a = childComp.value) == null ? void 0 : _a.openPopup();
    };
    const reOrder = async () => {
      await api_cart.cleanCartAPI();
      await api_order.reOrderAPI(order.id);
      common_vendor.index.redirectTo({
        url: "/pages/order/order"
      });
    };
    const connectShop = () => {
      common_vendor.index.makePhoneCall({
        phoneNumber: "1999"
      });
    };
    const toPay = () => {
      clearCountdown();
      common_vendor.index.redirectTo({
        url: "/pages/pay/pay?orderId=" + order.id + "&orderNumber=" + encodeURIComponent(order.number) + "&orderAmount=" + order.amount + "&orderTime=" + encodeURIComponent(String(order.orderTime))
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.t(statusText.value),
        b: order.status === 1
      }, order.status === 1 ? common_vendor.e({
        c: isExpired.value
      }, isExpired.value ? {} : {
        d: common_vendor.p({
          color: "#888",
          ["show-day"]: false,
          ["show-hour"]: false,
          minute: common_vendor.unref(countdownStore).showM,
          second: common_vendor.unref(countdownStore).showS
        })
      }) : {}, {
        e: Number(order.status) <= 2
      }, Number(order.status) <= 2 ? {
        f: common_vendor.o(cancelOrder)
      } : {}, {
        g: order.status === 1 && !isExpired.value
      }, order.status === 1 && !isExpired.value ? {
        h: common_vendor.o(toPay)
      } : {}, {
        i: order.status === 2
      }, order.status === 2 ? {
        j: common_vendor.o(pushOrder)
      } : {}, {
        k: order.status === 2 || order.status === 6
      }, order.status === 2 || order.status === 6 ? {
        l: common_vendor.o(reOrder)
      } : {}, {
        m: common_vendor.f(order.orderDetailList, (obj, index, i0) => {
          return common_vendor.e({
            a: obj.pic,
            b: common_vendor.t(obj.name),
            c: obj.dishFlavor
          }, obj.dishFlavor ? {
            d: common_vendor.t(obj.dishFlavor)
          } : {}, {
            e: obj.number && obj.number > 0
          }, obj.number && obj.number > 0 ? {
            f: common_vendor.t(obj.number)
          } : {}, {
            g: common_vendor.t(obj.amount),
            h: index
          });
        }),
        n: common_vendor.t(order.packAmount || 0),
        o: common_vendor.t(order.amount || 0),
        p: common_vendor.o(connectShop),
        q: common_vendor.t(order.remark || "无"),
        r: common_vendor.t(tablewareText.value),
        s: common_vendor.t(order.number),
        t: common_vendor.t(order.orderTime),
        v: common_vendor.t(order.address),
        w: common_vendor.sr(childComp, "2d945b00-1", {
          "k": "childComp"
        })
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-2d945b00"], ["__file", "D:/small-third/project1/hanye-take-out-main/hanye-take-out-uniapp/src/pages/orderDetail/orderDetail.vue"]]);
wx.createPage(MiniProgramPage);
