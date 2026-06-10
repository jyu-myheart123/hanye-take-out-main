# 嘉园外卖 - 管理员后台

## 项目说明

嘉园外卖系统管理员后台前端，基于 Vue3 + TypeScript + Vite 开发。

## 技术栈

- Vue 3.4+
- TypeScript 5+
- Vite 5+
- Element Plus
- Pinia
- Vue Router 4
- Axios

## 环境要求

- Node.js 16+
- npm 8+

## 安装依赖

```bash
npm install
```

## 开发模式

```bash
npm run dev
```

## 构建生产版本

```bash
npm run build
```

## 接口配置

接口地址配置在 `src/utils/request.ts` 中：

```typescript
const baseURL = 'http://localhost:8081'  // 开发环境
```

## 项目结构

```
src/
├── api/          # API接口封装
├── components/   # 通用组件
├── router/       # 路由配置
├── stores/       # 状态管理
├── utils/        # 工具函数
├── views/        # 页面组件
├── assets/       # 静态资源
└── styles/       # 全局样式
```

## 页面说明

| 页面 | 路径 | 说明 |
|------|------|------|
| 控制台 | /home | 数据统计概览 |
| 订单管理 | /order | 订单列表与操作 |
| 分类管理 | /category | 菜品/套餐分类 |
| 菜品管理 | /dish | 菜品列表与CRUD |
| 套餐管理 | /setmeal | 套餐列表与CRUD |
| 员工管理 | /employee | 员工账号管理 |
