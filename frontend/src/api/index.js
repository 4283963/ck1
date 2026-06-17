import request from '../utils/request'

export function reportTransportData(data) {
  return request({
    url: '/transport/report',
    method: 'post',
    data
  })
}

export function getTransportRecords(vehicleId) {
  return request({
    url: `/transport/vehicle/${vehicleId}`,
    method: 'get'
  })
}

export function getLatestRecord(vehicleId) {
  return request({
    url: `/transport/vehicle/${vehicleId}/latest`,
    method: 'get'
  })
}

export function getAllVehicles() {
  return request({
    url: '/vehicles',
    method: 'get'
  })
}

export function getVehicleById(id) {
  return request({
    url: `/vehicles/${id}`,
    method: 'get'
  })
}

export function createVehicle(data) {
  return request({
    url: '/vehicles',
    method: 'post',
    data
  })
}

export function updateVehicle(id, data) {
  return request({
    url: `/vehicles/${id}`,
    method: 'put',
    data
  })
}

export function deleteVehicle(id) {
  return request({
    url: `/vehicles/${id}`,
    method: 'delete'
  })
}

export function getAllAlerts() {
  return request({
    url: '/alerts',
    method: 'get'
  })
}

export function getUnresolvedAlerts() {
  return request({
    url: '/alerts/unresolved',
    method: 'get'
  })
}

export function resolveAlert(id, resolvedBy = '系统管理员') {
  return request({
    url: `/alerts/${id}/resolve`,
    method: 'put',
    data: { resolvedBy }
  })
}
