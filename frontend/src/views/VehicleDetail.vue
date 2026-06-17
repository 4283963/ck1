<template>
  <div class="detail-page">
    <header class="detail-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" @click="goBack" circle />
        <h1>车次详情 - {{ vehicle?.plateNumber || '加载中...' }}</h1>
        <el-tag
          v-if="vehicle"
          :type="hasActiveAlert ? 'danger' : 'success'"
          effect="dark"
          size="large">
          {{ hasActiveAlert ? '数据失真预警' : vehicle.status }}
        </el-tag>
      </div>
      <div class="header-right">
        <span class="refresh-time">最后刷新: {{ lastRefresh }}</span>
        <el-button type="primary" :icon="Refresh" @click="refreshAll">刷新</el-button>
      </div>
    </header>

    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <div v-else-if="!vehicle" class="empty-container">
      <el-empty description="未找到该车辆信息" />
      <el-button type="primary" @click="goBack">返回大屏</el-button>
    </div>

    <div v-else class="detail-content">
      <section class="info-section">
        <div class="section-title">
          <el-icon :size="20"><Van /></el-icon>
          <h2>基本信息</h2>
        </div>
        <el-descriptions :column="3" border class="info-desc">
          <el-descriptions-item label="车牌号">
            <span class="highlight">{{ vehicle.plateNumber }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="驾驶员">
            {{ vehicle.driverName }}
            <span v-if="vehicle.driverPhone" class="sub">（{{ vehicle.driverPhone }}）</span>
          </el-descriptions-item>
          <el-descriptions-item label="车辆状态">
            <el-tag :type="hasActiveAlert ? 'danger' : 'success'" effect="dark">
              {{ hasActiveAlert ? '预警中' : vehicle.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="疫苗类型">{{ vehicle.vaccineType }}</el-descriptions-item>
          <el-descriptions-item label="疫苗批次">{{ vehicle.vaccineBatch || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运输数量">
            {{ vehicle.vaccineCount ? vehicle.vaccineCount + ' 支/剂' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="出发地">
            {{ vehicle.originProvince }}{{ vehicle.originCity || '' }}
          </el-descriptions-item>
          <el-descriptions-item label="目的地">
            {{ vehicle.destProvince }}{{ vehicle.destCity || '' }}
          </el-descriptions-item>
          <el-descriptions-item label="预计到达">
            {{ formatDateTime(vehicle.expectedArrivalTime) || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="发车时间">
            {{ formatDateTime(vehicle.departureTime) || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(vehicle.createTime) || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="location-section">
        <div class="section-title">
          <el-icon :size="20" color="#409EFF"><Location /></el-icon>
          <h2>实时位置</h2>
          <div v-if="latestRecord" class="location-meta">
            <el-tag type="primary" size="small">
              温度: {{ latestRecord.temperature }}°C
            </el-tag>
            <el-tag type="info" size="small">
              {{ formatDateTime(latestRecord.reportTime) }}
            </el-tag>
          </div>
        </div>
        <div class="map-container">
          <div v-if="latestRecord" id="map"></div>
          <el-empty v-else description="暂无位置数据" />
        </div>
        <div v-if="latestRecord" class="location-info">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="纬度">
              <span class="highlight">{{ latestRecord.latitude?.toFixed(6) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="经度">
              <span class="highlight">{{ latestRecord.longitude?.toFixed(6) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="详细地址">
              {{ latestRecord.locationAddress || '暂无地址信息' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </section>

      <section class="records-section">
        <div class="section-title">
          <el-icon :size="20" color="#67C23A"><DataLine /></el-icon>
          <h2>运输记录</h2>
          <el-tag type="success" size="small">共 {{ records.length }} 条</el-tag>
        </div>
        <el-table :data="records" stripe style="width: 100%" max-height="360">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="reportTime" label="上报时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.reportTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="temperature" label="温度(°C)" width="120" align="center">
            <template #default="{ row }">
              <span :class="getTempClass(row.temperature)">{{ row.temperature }}</span>
            </template>
          </el-table-column>
          <el-table-column label="位置" min-width="280">
            <template #default="{ row }">
              <span v-if="row.locationAddress">{{ row.locationAddress }}</span>
              <span v-else>{{ row.latitude?.toFixed(4) }}, {{ row.longitude?.toFixed(4) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-if="vehicleAlerts.length > 0" class="alerts-section">
        <div class="section-title">
          <el-icon :size="20" color="#F56C6C"><Bell /></el-icon>
          <h2>预警记录</h2>
          <el-tag type="danger" size="small">共 {{ vehicleAlerts.length }} 条</el-tag>
        </div>
        <el-table :data="vehicleAlerts" stripe style="width: 100%">
          <el-table-column prop="alertTime" label="预警时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.alertTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="alertType" label="类型" width="140" align="center">
            <template #default="{ row }">
              <el-tag type="danger" effect="dark" size="small">
                {{ row.alertType === 'DATA_DISTORTION' ? '数据失真' : row.alertType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="alertLevel" label="级别" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="danger" size="small">{{ row.alertLevel === 'HIGH' ? '高' : row.alertLevel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="alertMessage" label="预警内容" min-width="300" />
          <el-table-column prop="isResolved" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.isResolved" type="success" size="small">已处理</el-tag>
              <el-tag v-else type="danger" effect="dark" size="small">未处理</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="处理人" width="120">
            <template #default="{ row }">
              {{ row.resolvedBy || '-' }}
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Refresh, Loading, Van, Location, DataLine, Bell } from '@element-plus/icons-vue'
import L from 'leaflet'
import { getVehicleById, getTransportRecords, getLatestRecord, getAlertsByVehicle } from '../api'

const route = useRoute()
const router = useRouter()

const vehicleId = computed(() => Number(route.params.id))
const vehicle = ref(null)
const records = ref([])
const latestRecord = ref(null)
const vehicleAlerts = ref([])
const loading = ref(true)
const lastRefresh = ref('-')

let mapInstance = null
let markerInstance = null
let refreshTimer = null

const hasActiveAlert = computed(() =>
  vehicleAlerts.value.some(a => !a.isResolved)
)

function formatDateTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function getTempClass(temp) {
  if (temp === null || temp === undefined) return ''
  const t = Number(temp)
  if (t < 2 || t > 8) return 'temp-danger'
  if (t < 3 || t > 7) return 'temp-warning'
  return 'temp-normal'
}

function goBack() {
  router.push('/')
}

async function loadVehicle(silent = false) {
  try {
    vehicle.value = await getVehicleById(vehicleId.value, silent)
  } catch (e) {
    console.error('加载车辆信息失败', e)
  }
}

async function loadRecords(silent = false) {
  try {
    records.value = await getTransportRecords(vehicleId.value, silent)
    if (records.value.length > 0) {
      latestRecord.value = records.value[0]
    } else {
      latestRecord.value = await getLatestRecord(vehicleId.value, silent)
    }
  } catch (e) {
    console.error('加载运输记录失败', e)
  }
}

async function loadAlerts(silent = false) {
  try {
    vehicleAlerts.value = await getAlertsByVehicle(vehicleId.value, silent)
  } catch (e) {
    console.error('加载预警记录失败', e)
  }
}

async function refreshAll() {
  loading.value = true
  await Promise.all([loadVehicle(false), loadRecords(false), loadAlerts(false)])
  loading.value = false
  lastRefresh.value = formatDateTime(new Date())
  nextTick(() => initMap())
  ElMessage.success('数据已刷新')
}

function initMap() {
  if (!latestRecord.value || !document.getElementById('map')) return

  const lat = Number(latestRecord.value.latitude)
  const lng = Number(latestRecord.value.longitude)

  if (!mapInstance) {
    mapInstance = L.map('map').setView([lat, lng], 13)

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19
    }).addTo(mapInstance)
  } else {
    mapInstance.setView([lat, lng], 13)
  }

  if (markerInstance) {
    mapInstance.removeLayer(markerInstance)
  }

  const truckIcon = L.divIcon({
    html: `<div style="font-size: 32px; color: #409EFF; text-align: center; text-shadow: 0 2px 4px rgba(0,0,0,0.3);">🚛</div>`,
    className: 'truck-marker',
    iconSize: [40, 40],
    iconAnchor: [20, 20],
    popupAnchor: [0, -16]
  })

  markerInstance = L.marker([lat, lng], { icon: truckIcon })
    .addTo(mapInstance)
    .bindPopup(
      `<div style="padding: 4px;">
        <strong>${vehicle.value?.plateNumber || ''}</strong><br/>
        温度: ${latestRecord.value.temperature}°C<br/>
        时间: ${formatDateTime(latestRecord.value.reportTime)}
      </div>`
    )
    .openPopup()
}

onMounted(async () => {
  await refreshAll()
  refreshTimer = setInterval(async () => {
    await loadVehicle(true)
    await loadRecords(true)
    await loadAlerts(true)
    lastRefresh.value = formatDateTime(new Date())
    nextTick(() => initMap())
  }, 10000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
})
</script>

<style scoped>
.detail-page {
  width: 100%;
  min-height: 100vh;
  background: #f0f2f5;
}

.detail-header {
  background: linear-gradient(135deg, #1a2a42, #2a4a72);
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-left h1 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.refresh-time {
  font-size: 13px;
  color: #8aa0bd;
}

.loading-container,
.empty-container {
  padding: 80px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #8aa0bd;
}

.detail-content {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.section-title h2 {
  font-size: 16px;
  font-weight: 600;
  color: #1a2a42;
  margin: 0;
  flex: 1;
}

.info-section,
.location-section,
.records-section,
.alerts-section {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.highlight {
  color: #409EFF;
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

.sub {
  color: #909399;
  font-size: 13px;
}

.map-container {
  width: 100%;
  height: 380px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  background: #f5f7fa;
}

#map {
  width: 100%;
  height: 100%;
}

.location-meta {
  display: flex;
  gap: 10px;
  flex: 0;
}

.location-info {
  margin-top: 14px;
}

.temp-normal {
  color: #67C23A;
  font-weight: 600;
}

.temp-warning {
  color: #E6A23C;
  font-weight: 600;
}

.temp-danger {
  color: #F56C6C;
  font-weight: 600;
}

:deep(.el-descriptions__label) {
  font-weight: 600;
  color: #606266;
  background-color: #fafafa;
  width: 110px;
}

:deep(.el-descriptions--small .el-descriptions__label) {
  width: 100px;
}
</style>
