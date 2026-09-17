import request from '@/utils/request'

/**
 * 营销活动相关接口
 * 小白讲解：/api 会被代理成后端的 /admin，所以这里写 /promotion 实际请求 /admin/promotion
 */

/** 分页查询营销活动 */
export const getPromotionPageAPI = (params: any) => {
  return request({
    url: '/promotion/page',
    method: 'get',
    params
  })
}

/** 查询当前生效中的活动（开单页顶部提示用） */
export const getActivePromotionAPI = () => {
  return request({
    url: '/promotion/active',
    method: 'get'
  })
}

/** 根据id查活动详情（编辑回显） */
export const getPromotionByIdAPI = (id: number) => {
  return request({
    url: `/promotion/${id}`,
    method: 'get'
  })
}

/** 新增活动 */
export const addPromotionAPI = (data: any) => {
  return request({
    url: '/promotion',
    method: 'post',
    data
  })
}

/** 修改活动 */
export const updatePromotionAPI = (data: any) => {
  return request({
    url: '/promotion',
    method: 'put',
    data
  })
}

/** 启用/停用活动 */
export const togglePromotionStatusAPI = (id: number) => {
  return request({
    url: `/promotion/status/${id}`,
    method: 'put'
  })
}

/** 删除活动 */
export const deletePromotionAPI = (id: number) => {
  return request({
    url: `/promotion/${id}`,
    method: 'delete'
  })
}
