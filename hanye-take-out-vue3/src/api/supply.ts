import request from '@/utils/request'

/**
 * 供应管理相关接口
 * 小白讲解：供应商、原材料档案，采购记录，月度对账。
 */

/** 供应商列表（含本月统计） */
export const getSupplierListAPI = () => {
  return request({
    url: '/supply/supplier/list',
    method: 'get'
  })
}

/** 保存供应商（新增/编辑） */
export const saveSupplierAPI = (data: any) => {
  return request({
    url: '/supply/supplier',
    method: 'post',
    data
  })
}

/** 删除供应商 */
export const deleteSupplierAPI = (id: number) => {
  return request({
    url: `/supply/supplier/${id}`,
    method: 'delete'
  })
}

/** 原材料列表 */
export const getMaterialListAPI = () => {
  return request({
    url: '/supply/material/list',
    method: 'get'
  })
}

/** 保存原材料（新增/编辑） */
export const saveMaterialAPI = (data: any) => {
  return request({
    url: '/supply/material',
    method: 'post',
    data
  })
}

/** 删除原材料 */
export const deleteMaterialAPI = (id: number) => {
  return request({
    url: `/supply/material/${id}`,
    method: 'delete'
  })
}

/** 采购记录分页 */
export const getPurchasePageAPI = (params: any) => {
  return request({
    url: '/supply/purchase/page',
    method: 'get',
    params
  })
}

/** 新增采购记录 */
export const addPurchaseAPI = (data: any) => {
  return request({
    url: '/supply/purchase',
    method: 'post',
    data
  })
}

/** 月度对账（month=yyyy-MM，默认本月） */
export const getMonthStatementAPI = (month: string) => {
  return request({
    url: '/supply/statement',
    method: 'get',
    params: { month }
  })
}

/** 采购概览：今日/本月的金额与单数 */
export const getSupplyOverviewAPI = () => {
  return request({
    url: '/supply/overview',
    method: 'get'
  })
}
