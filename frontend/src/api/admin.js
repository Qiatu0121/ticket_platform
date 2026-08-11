import request from './request'

export const createActivity = (data) => request.post('/admin/activity', data)

export const addTicket = (activityId, data) =>
  request.post(`/admin/activity/${activityId}/ticket`, data)

// status 传 "PAID" / "VERIFIED" 等枚举名；空字符串则查全部
export const listOrders = (status) =>
  request.get('/admin/orders', { params: { status } })
