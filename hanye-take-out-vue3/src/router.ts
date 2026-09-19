import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('./views/layout/index.vue'),
      redirect: '/dashboard', // 将dashboard设为首页home
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          // lazy loading
          component: () => import('./views/dashboard/index.vue')
        },
        {
          path: 'statistics',
          name: 'statistics',
          component: () => import('./views/statistics/index.vue')
        },
        {
          // 堂食开单：员工为到店老客户现场代下单（放在数据统计与订单管理之间）
          path: 'dinein',
          name: 'dinein',
          component: () => import('./views/dinein/index.vue')
        },
        {
          path: 'order',
          name: 'order',
          component: () => import('./views/order/index.vue')
        },
        {
          path: 'category',
          name: 'category',
          component: () => import('./views/category/index.vue')
        },
        {
          path: 'category/add',
          name: 'category_add',
          component: () => import('./views/category/add.vue')
        },
        {
          path: 'category/update',
          name: 'category_update',
          component: () => import('./views/category/update.vue')
        },
        {
          path: 'dish',
          name: 'dish',
          component: () => import('./views/dish/index.vue')
        },
        {
          path: 'dish/add',
          name: 'dish_add',
          component: () => import('./views/dish/add.vue')
        },
        {
          path: 'setmeal',
          name: 'setmeal',
          component: () => import('./views/setmeal/index.vue')
        },
        {
          path: 'setmeal/add',
          name: 'setmeal_add',
          component: () => import('./views/setmeal/add.vue')
        },
        {
          path: 'employee',
          name: 'employee',
          component: () => import('./views/employee/index.vue')
        },
        {
          path: 'employee/add',
          name: 'employee_add',
          component: () => import('./views/employee/add.vue')
        },
        {
          path: 'employee/update',
          name: 'employee_update',
          component: () => import('./views/employee/update.vue')
        },
        {
          // 营销活动：满减/折扣/第二份半价/买一送一配置（放在员工管理下面）
          path: 'promotion',
          name: 'promotion',
          component: () => import('./views/promotion/index.vue')
        },
        {
          // 堂食会员：会员信息、储值、积分、等级、消费记录（营销活动下面）
          path: 'member',
          name: 'member',
          component: () => import('./views/member/index.vue')
        },
        {
          // 客户评价：已完成堂食单的评分与改进意见，后厨可查看回复（最下面）
          path: 'review',
          name: 'review',
          component: () => import('./views/review/index.vue')
        },
        {
          // 员工赏罚：顾客服务评分、小费打赏、北极星积分排名、店主奖励惩戒
          path: 'staff',
          name: 'staff',
          component: () => import('./views/staff/index.vue')
        }
      ]
    },
    {
      path: '/login',
      name: 'login',
      // lazy loading
      component: () => import('./views/login/index.vue')
    },
    {
      path: '/reg',
      name: 'reg',
      // lazy loading
      component: () => import('./views/reg/index.vue')
    }
  ]
})

export default router
