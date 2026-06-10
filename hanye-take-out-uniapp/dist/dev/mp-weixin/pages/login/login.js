"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const api_login = require("../../api/login.js");
const stores_modules_user = require("../../stores/modules/user.js");
require("../../utils/http.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "login",
  setup(__props) {
    let code = "";
    common_vendor.onLoad(async () => {
      const res = await common_vendor.wx$1.login();
      code = res.code;
      console.log("微信登录code:", code);
    });
    const login = async () => {
      console.log("login");
      try {
        const res = await api_login.loginAPI(code);
        console.log(res);
        loginSuccess(res.data);
      } catch (error) {
        console.error("登录失败:", error);
        common_vendor.index.showToast({
          title: "登录失败，请重试",
          icon: "none"
        });
      }
    };
    const testLogin = async () => {
      console.log("test login");
      const mockUser = {
        id: 1,
        name: "测试用户",
        phone: "13800138000",
        gender: 1,
        pic: "",
        token: "mock_token",
        openid: "test_openid_123456"
      };
      loginSuccess(mockUser);
    };
    const loginSuccess = (profile) => {
      const userStore = stores_modules_user.useUserStore();
      userStore.setProfile(profile);
      common_vendor.index.showToast({ icon: "success", title: "登录成功" });
      setTimeout(() => {
        common_vendor.index.switchTab({ url: "/pages/my/my" });
      }, 500);
    };
    return (_ctx, _cache) => {
      return {
        a: common_assets._imports_0,
        b: common_vendor.o(login),
        c: common_vendor.o(testLogin)
      };
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-cdfe2409"], ["__file", "D:/small-third/project1/hanye-take-out-main/hanye-take-out-uniapp/src/pages/login/login.vue"]]);
wx.createPage(MiniProgramPage);
