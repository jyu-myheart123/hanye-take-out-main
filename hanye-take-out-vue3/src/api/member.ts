import request from '@/utils/request'

/**
 * 堂食会员相关接口
 */

/** 会员分页查询（name姓名 / phone手机号 / level等级） */
export const getMemberPageAPI = (params: any) => {
  return request({
    url: '/member/page',
    method: 'get',
    params
  })
}

/** 根据id查会员详情 */
export const getMemberByIdAPI = (id: number) => {
  return request({
    url: `/member/${id}`,
    method: 'get'
  })
}

/** 根据手机号识别会员（开单页输手机号后调用） */
export const getMemberByPhoneAPI = (phone: string) => {
  return request({
    url: `/member/phone/${phone}`,
    method: 'get'
  })
}

/** 手动新增会员（办卡） */
export const addMemberAPI = (data: any) => {
  return request({
    url: '/member',
    method: 'post',
    data
  })
}

/** 会员充值（本金 amount + 赠送 gift） */
export const rechargeMemberAPI = (data: any) => {
  return request({
    url: '/member/recharge',
    method: 'post',
    data
  })
}

/** 人工调整积分（points 正加负减） */
export const adjustMemberPointsAPI = (data: any) => {
  return request({
    url: '/member/points',
    method: 'put',
    data
  })
}

/** 查询会员流水（消费/充值/积分记录） */
export const getMemberFlowAPI = (memberId: number, params: any) => {
  return request({
    url: `/member/flow/${memberId}`,
    method: 'get',
    params
  })
}

/** 查询全部会员等级规则 */
export const getMemberRulesAPI = () => {
  return request({
    url: '/member/rules',
    method: 'get'
  })
}

/** 修改会员等级规则 */
export const updateMemberRulesAPI = (data: any) => {
  return request({
    url: '/member/rules',
    method: 'put',
    data
  })
}
