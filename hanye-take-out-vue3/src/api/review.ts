import request from '@/utils/request'

/**
 * 客户评价相关接口
 */

/** 待评价订单分页：已完成堂食单、还没评价的 */
export const getPendingReviewPageAPI = (params: any) => {
  return request({
    url: '/review/pending',
    method: 'get',
    params
  })
}

/** 评价列表分页（keyword关键词 / status状态） */
export const getReviewPageAPI = (params: any) => {
  return request({
    url: '/review/page',
    method: 'get',
    params
  })
}

/** 提交评价 */
export const submitReviewAPI = (data: any) => {
  return request({
    url: '/review',
    method: 'post',
    data
  })
}

/** 商家回复评价 */
export const replyReviewAPI = (data: any) => {
  return request({
    url: '/review/reply',
    method: 'put',
    data
  })
}

/** 评价显示/隐藏 */
export const toggleReviewStatusAPI = (id: number) => {
  return request({
    url: `/review/status/${id}`,
    method: 'put'
  })
}

/** 删除评价 */
export const deleteReviewAPI = (id: number) => {
  return request({
    url: `/review/${id}`,
    method: 'delete'
  })
}
