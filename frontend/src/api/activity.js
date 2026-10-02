import request from './request'

export function getActivityList(params) {
  return request.get('/activity/list', { params })
}

export function getActivityDetail(id) {
  return request.get(`/activity/${id}`)
}

export function getCategories() {
  return request.get('/category/list')
}
