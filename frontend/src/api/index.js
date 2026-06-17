import request from '../utils/request'

function buildSilentConfig(silent) {
  return silent ? { silent: true } : {}
}

export function reportTransportData(data) {
  return request({
    url: '/transport/report',
    method: 'post',
    data
  })
}

export function getTransportRecords(vehicleId, silent = false) {
  return request({
    url: `/transport/vehicle/${vehicleId}`,
    method: 'get',
    ...buildSilentConfig(silent)
  })
}

export function getLatestRecord(vehicleId, silent = false) {
  return request({
    url: `/transport/vehicle/${vehicleId}/latest`,
    method: 'get',
    ...buildSilentConfig(silent)
  })
}

export function getAllVehicles(silent = false) {
  return request({
    url: '/vehicles',
    method: 'get',
    ...buildSilentConfig(silent)
  })
}

export function getVehicleById(id, silent = false) {
  return request({
    url: `/vehicles/${id}`,
    method: 'get',
    ...buildSilentConfig(silent)
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

export function getAllAlerts(silent = false) {
  return request({
    url: '/alerts',
    method: 'get',
    ...buildSilentConfig(silent)
  })
}

export function getUnresolvedAlerts(silent = false) {
  return request({
    url: '/alerts/unresolved',
    method: 'get',
    ...buildSilentConfig(silent)
  })
}

export function getAlertsByVehicle(vehicleId, silent = false) {
  return request({
    url: `/alerts/vehicle/${vehicleId}`,
    method: 'get',
    ...buildSilentConfig(silent)
  })
}

export function resolveAlert(id, resolvedBy = '系统管理员') {
  return request({
    url: `/alerts/${id}/resolve`,
    method: 'put',
    data: { resolvedBy }
  })
}
