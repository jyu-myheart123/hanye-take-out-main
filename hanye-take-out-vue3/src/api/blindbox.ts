import request from '@/utils/request'

/**
 * 惊喜盲盒相关接口
 * 小白讲解：盲盒配置的增删查，以及付款后随机抽卡（抽中即生成订单）。
 */

/** 盲盒列表（含套餐选项） */
export const getBlindBoxListAPI = () => {
  return request({
    url: '/blindbox/list',
    method: 'get'
  })
}

/** 保存盲盒配置（新增/编辑，含3个套餐） */
export const saveBlindBoxAPI = (data: any) => {
  return request({
    url: '/blindbox',
    method: 'post',
    data
  })
}

/** 删除盲盒 */
export const deleteBlindBoxAPI = (id: number) => {
  return request({
    url: `/blindbox/${id}`,
    method: 'delete'
  })
}

/** 抽卡：随机抽中套餐并生成订单，返回三张卡牌内容 */
export const drawBlindBoxAPI = (data: any) => {
  return request({
    url: '/blindbox/draw',
    method: 'post',
    data
  })
}
