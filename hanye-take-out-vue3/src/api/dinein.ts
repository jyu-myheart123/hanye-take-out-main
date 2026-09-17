import request from '@/utils/request' // 引入自定义的axios函数

/**
 * 堂食开单 - 查询启用中的分类列表
 * 小白讲解：开单页左侧的分类标签，比如"热菜、凉菜、主食"
 */
export const getDineInCategoryListAPI = () => {
  return request({
    url: '/dinein/categories',
    method: 'get'
  })
}

/**
 * 堂食开单 - 根据分类id查询启售菜品
 * @param categoryId 分类id，不传表示查全部启售菜品
 */
export const getDineInDishListAPI = (categoryId?: number) => {
  return request({
    url: '/dinein/dishes',
    method: 'get',
    params: { categoryId }
  })
}

/**
 * 堂食开单 - 价格试算（不生成订单）
 * 小白讲解：改菜品、填会员手机号、勾选积分抵扣后实时调用，
 * 后端返回原价/活动优惠/会员折扣/积分抵扣/实付金额，页面只展示不自己算账
 * @param params { items 清单, memberPhone 会员手机号, usePoints 是否用积分 }
 */
export const dineInPreviewAPI = (params: any) => {
  return request({
    url: '/dinein/preview',
    method: 'post',
    data: { ...params }
  })
}

/**
 * 堂食开单 - 提交订单
 * 小白讲解：只传菜品id和份数，价格由后端核算，前端传的价格不作数
 * @param params { tableNo 桌号/称呼, phone 电话, remark 备注, items 清单,
 *                 memberPhone 会员手机号(选填), payMethod 1现金 2余额, usePoints 是否用积分 }
 */
export const dineInSubmitAPI = (params: any) => {
  return request({
    url: '/dinein/submit',
    method: 'post',
    data: { ...params }
  })
}
