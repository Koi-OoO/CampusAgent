import request from './request'

export function signup(data) {
  return request.post('/activity/signup', data)
}

export function cancelSignup(activityId) {
  return request.delete(`/activity/signup/${activityId}`)
}

export function getMySignups(params) {
  return request.get('/activity/signup/my', { params })
}
