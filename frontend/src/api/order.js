import request from './request'

// userId 由后端从登录 token 解析，前端不再传
export const grabTicket = (ticketId) =>
  request.post('/order/grab', null, { params: { ticketId } })

export const payOrder = (orderId) =>
  request.post('/order/pay', null, { params: { orderId } })

export const verifyOrder = (orderId) =>
  request.post('/order/verify', null, { params: { orderId } })

export const myOrders = () => request.get('/order/my')
