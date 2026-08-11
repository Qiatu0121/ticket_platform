import request from './request'

export const listActivities = () => request.get('/activity/list')

export const getActivityDetail = (id) => request.get(`/activity/${id}`)
