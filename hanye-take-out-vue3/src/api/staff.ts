import request from '@/utils/request'

/**
 * 员工赏罚相关接口
 * 小白讲解：员工服务评分、小费打赏、北极星排行榜、店主奖惩和流水查询都在这里。
 */

/** 在职员工简要列表（评分时选择服务员工） */
export const getStaffEmployeesAPI = () => {
  return request({
    url: '/staff/employees',
    method: 'get'
  })
}

/** 提交员工服务评分（成功后返回评分id，用来接着打赏） */
export const submitStaffRatingAPI = (data: any) => {
  return request({
    url: '/staff/rating',
    method: 'post',
    data
  })
}

/** 顾客打赏小费 */
export const staffTipAPI = (data: { ratingId: number; amount: number }) => {
  return request({
    url: '/staff/tip',
    method: 'post',
    data
  })
}

/** 星光排行榜：range=today(今日)/3d(近3日)/7d(近7日)/month(近一月) */
export const getStaffRankAPI = (range: string) => {
  return request({
    url: '/staff/rank',
    method: 'get',
    params: { range }
  })
}

/** 评分记录分页 */
export const getStaffRatingPageAPI = (params: any) => {
  return request({
    url: '/staff/rating/page',
    method: 'get',
    params
  })
}

/** 评分显示/隐藏 */
export const toggleStaffRatingStatusAPI = (id: number) => {
  return request({
    url: `/staff/rating/status/${id}`,
    method: 'put'
  })
}

/** 删除评分 */
export const deleteStaffRatingAPI = (id: number) => {
  return request({
    url: `/staff/rating/${id}`,
    method: 'delete'
  })
}

/** 赏罚流水分页 */
export const getStaffRewardPageAPI = (params: any) => {
  return request({
    url: '/staff/reward/page',
    method: 'get',
    params
  })
}

/** 店主奖励/惩戒（仅管理员） */
export const addStaffRewardAPI = (data: any) => {
  return request({
    url: '/staff/reward',
    method: 'post',
    data
  })
}
