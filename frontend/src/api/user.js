import request from './request'

export const register = (username, phone, nickname, password) =>
  request.post('/user/register', null, { params: { username, phone, nickname, password } })

// account 支持用户名或手机号；返回 { token, user }
export const login = (account, password) =>
  request.post('/user/login', null, { params: { account, password } })

export const logout = () => request.post('/user/logout')
