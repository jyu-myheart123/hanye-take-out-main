# 嘉园外卖 - 微信小程序

## 项目说明

嘉园外卖系统微信小程序前端，基于 UniApp + Vue3 + TypeScript 开发。

## 技术栈

- UniApp 3+
- Vue 3.4+
- TypeScript 5+
- Vite 5+
- Uni UI

## 环境要求

- Node.js 16+
- npm 8+
- HBuilderX（推荐）或 VS Code + 插件

## 安装依赖

```bash
npm install
```

## 开发模式

```bash
# H5版本（浏览器调试）
npm run dev:h5

# 微信小程序版本
npm run dev:mp-weixin
```

## 构建生产版本

```bash
# H5版本
npm run build:h5

# 微信小程序版本
npm run build:mp-weixin
```

## 微信小程序开发

1. 运行 `npm run dev:mp-weixin`
2. 使用微信开发者工具导入 `dist/build/mp-weixin` 目录
3. 修改项目 appid 为自己的小程序 appid

## 接口配置

接口地址配置在 `src/utils/request.ts` 中：

```typescript
const baseURL = 'http://localhost:8081'  // 开发环境
```

## 项目结构

```
src/
├── api/          # API接口封装
├── pages/        # 页面文件
│   ├── index/    # 首页
│   ├── shop/     # 店铺页
│   ├── dish/     # 菜品详情
│   ├── setmeal/  # 套餐详情
│   ├── cart/     # 购物车
│   ├── order/    # 订单页
│   ├── address/  # 地址管理
│   ├── login/    # 登录页
│   └── user/     # 用户中心
├── static/       # 静态资源
└── utils/        # 工具函数
```

## 页面说明

| 页面 | 路径 | 说明 |
|------|------|------|
| 首页 | /pages/index/index | 首页店铺展示 |
| 登录 | /pages/login/login | 用户登录 |
| 菜品详情 | /pages/dish/index | 菜品详情与购买 |
| 套餐详情 | /pages/setmeal/index | 套餐详情与购买 |
| 购物车 | /pages/cart/index | 购物车管理 |
| 订单确认 | /pages/order/index | 订单提交 |
| 订单列表 | /pages/order/list | 历史订单 |
| 地址管理 | /pages/address/index | 收货地址 |
| 用户中心 | /pages/user/index | 个人中心 |
