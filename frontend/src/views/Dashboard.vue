<template>
  <div class="dashboard">
    <header class="header">
      <div class="header-left">
        <el-icon :size="32" color="#409EFF"><Van /></el-icon>
        <h1>疫苗冷链运输协同大屏</h1>
      </div>
      <div class="header-right">
        <span class="current-time">{{ currentTime }}</span>
        <el-button type="primary" :icon="Refresh" @click="refreshData">刷新数据</el-button>
      </div>
    </header>

    <section class="stats-section">
      <div class="stat-card normal">
        <div class="stat-icon">
          <el-icon :size="40"><Van /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ totalVehicles }}</div>
          <div class="stat-label">在运车辆</div>
        </div>
      </div>
      <div class="stat-card warning">
        <div class="stat-icon">
          <el-icon :size="40"><Warning /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ alertCount }}</div>
          <div class="stat-label">待处理预警</div>
        </div>
      </div>
      <div class="stat-card success">
        <div class="stat-icon">
          <el-icon :size="40"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ normalVehicles }}</div>
          <div class="stat-label">运行正常</div>
        </div>
      </div>
      <div class="stat-card danger">
        <div class="stat-icon">
          <el-icon :size="40"><DataAnalysis /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ distortionAlerts }}</div>
          <div class="stat-label">数据失真预警</div>
        </div>
      </div>
    </section>

    <section class="main-content">
      <div class="panel vehicle-panel">
        <div class="panel-header">
          <h2><el-icon><List /></el-icon> 在运车次列表</h2>
          <el-tag type="info" size="small">共 {{ vehicles.length }} 辆车</el-tag>
        </div>
        <div class="panel-body">
          <el-table
            :data="vehicles"
            style="width: 100%"
            row-key="id"
            @row-click="handleRowClick"
            :row-style="{ cursor: 'pointer' }"
            stripe>
            <el-table-column prop="plateNumber" label="车牌号" width="120">
              <template #default="{ row }">
                <span class="plate-number">{{ row.plateNumber }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="driverName" label="驾驶员" width="100" />
            <el-table-column prop="vaccineType" label="疫苗类型" width="120" />
            <el-table-column prop="vaccineBatch" label="批次号" width="130" />
            <el-table-column label="运输路线" min-width="180">
              <template #default="{ row }">
                <span class="route">
                  {{ row.originProvince }}{{ row.originCity || '' }}
                  <el-icon color="#409EFF"><Right /></el-icon>
                  {{ row.destProvince }}{{ row.destCity || '' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag
                  :type="hasActiveAlert(row.id) ? 'danger' : 'success'"
                  size="small"
                  effect="dark">
                  {{ hasActiveAlert(row.id) ? '预警中' : row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click.stop="goToDetail(row.id)">
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div class="panel alert-panel">
        <div class="panel-header">
          <h2><el-icon color="#F56C6C"><Bell /></el-icon> 预警信息</h2>
          <el-tag v-if="unresolvedAlerts.length > 0" type="danger" size="small" effect="dark">
            {{ unresolvedAlerts.length }} 条未处理
          </el-tag>
          <el-tag v-else type="success" size="small">暂无预警</el-tag>
        </div>
        <div class="panel-body alert-list">
          <el-empty v-if="unresolvedAlerts.length === 0" description="当前无预警信息" :image-size="80" />
          <div
            v-for="alert in unresolvedAlerts"
            :key="alert.id"
            class="alert-item danger-alert"
            @click="goToDetail(alert.vehicleId)">
            <div class="alert-header">
              <el-tag type="danger" effect="dark" size="small">
                {{ alert.alertType === 'DATA_DISTORTION' ? '数据失真预警' : alert.alertType }}
              </el-tag>
              <span class="alert-time">{{ formatTime(alert.alertTime) }}</span>
            </div>
            <div class="alert-plate">{{ alert.plateNumber }}</div>
            <div class="alert-message">{{ alert.alertMessage }}</div>
            <div class="alert-footer">
              <span v-if="alert.temperature !== null">温度: {{ alert.temperature }}°C</span>
              <span v-if="alert.latitude && alert.longitude">
                位置: {{ alert.latitude?.toFixed(4) }}, {{ alert.longitude?.toFixed(4) }}
              </span>
              <el-button
                type="success"
                size="small"
                link
                @click.stop="handleResolveAlert(alert)">
                标记已处理
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <el-dialog
      v-model="showAlertModal"
      :title="currentAlert?.plateNumber + ' - 数据失真预警'"
      width="560px"
      class="alert-modal"
      :close-on-click-modal="false">
      <div class="modal-content">
        <div class="modal-alert-icon">
          <el-icon :size="64" color="#F56C6C"><WarningFilled /></el-icon>
        </div>
        <div class="modal-alert-title">数据失真预警</div>
        <div class="modal-alert-message">{{ currentAlert?.alertMessage }}</div>
        <el-descriptions :column="1" border size="small" class="modal-desc">
          <el-descriptions-item label="车牌号">{{ currentAlert?.plateNumber }}</el-descriptions-item>
          <el-descriptions-item label="上报温度">{{ currentAlert?.temperature }}°C</el-descriptions-item>
          <el-descriptions-item label="预警时间">{{ formatTime(currentAlert?.alertTime) }}</el-descriptions-item>
          <el-descriptions-item label="当前位置">
            <span v-if="currentAlert?.latitude && currentAlert?.longitude">
              {{ currentAlert.latitude?.toFixed(6) }}, {{ currentAlert.longitude?.toFixed(6) }}
            </span>
            <span v-else>暂无位置信息</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="showAlertModal = false">稍后处理</el-button>
        <el-button type="primary" @click="viewVehicleDetail">查看车次详情</el-button>
        <el-button type="success" @click="handleResolveAlert(currentAlert)">标记已处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Van, Warning, CircleCheck, DataAnalysis, List, Right, Bell, WarningFilled } from '@element-plus/icons-vue'
import { getAllVehicles, getUnresolvedAlerts, resolveAlert } from '../api'

const router = useRouter()

const vehicles = ref([])
const unresolvedAlerts = ref([])
const currentTime = ref('')
const showAlertModal = ref(false)
const currentAlert = ref(null)
const alertedIds = ref(new Set())

let timeTimer = null
let dataTimer = null
let alertCheckTimer = null

const totalVehicles = computed(() => vehicles.value.length)
const alertCount = computed(() => unresolvedAlerts.value.length)
const normalVehicles = computed(() => {
  const alertVehicleIds = new Set(unresolvedAlerts.value.map(a => a.vehicleId))
  return vehicles.value.filter(v => !alertVehicleIds.has(v.id)).length
})
const distortionAlerts = computed(() =>
  unresolvedAlerts.value.filter(a => a.alertType === 'DATA_DISTORTION').length
)

function updateTime() {
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  currentTime.value = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function hasActiveAlert(vehicleId) {
  return unresolvedAlerts.value.some(a => a.vehicleId === vehicleId && !a.isResolved)
}

async function fetchVehicles(silent = false) {
  try {
    vehicles.value = await getAllVehicles(silent)
  } catch (e) {
    console.error('加载车辆列表失败', e)
  }
}

async function fetchAlerts(silent = false) {
  try {
    const beforeIds = new Set(unresolvedAlerts.value.map(a => a.id))
    unresolvedAlerts.value = await getUnresolvedAlerts(silent)

    for (const alert of unresolvedAlerts.value) {
      if (!beforeIds.has(alert.id) && !alertedIds.value.has(alert.id)) {
        alertedIds.value.add(alert.id)
        if (alert.alertType === 'DATA_DISTORTION') {
          currentAlert.value = alert
          showAlertModal.value = true
        }
      }
    }
  } catch (e) {
    console.error('加载预警列表失败', e)
  }
}

function refreshData() {
  fetchVehicles(false)
  fetchAlerts(false)
  ElMessage.success('数据已刷新')
}

function handleRowClick(row) {
  goToDetail(row.id)
}

function goToDetail(id) {
  router.push(`/vehicle/${id}`)
}

function viewVehicleDetail() {
  showAlertModal.value = false
  if (currentAlert.value) {
    goToDetail(currentAlert.value.vehicleId)
  }
}

async function handleResolveAlert(alert) {
  if (!alert) return
  try {
    await ElMessageBox.confirm(
      `确认将车牌号【${alert.plateNumber}】的预警标记为已处理？`,
      '预警处理确认',
      { type: 'warning' }
    )
    await resolveAlert(alert.id)
    ElMessage.success('预警已标记为已处理')
    showAlertModal.value = false
    fetchAlerts()
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

onMounted(() => {
  updateTime()
  fetchVehicles(false)
  fetchAlerts(false)

  timeTimer = setInterval(updateTime, 1000)
  dataTimer = setInterval(() => fetchVehicles(true), 10000)
  alertCheckTimer = setInterval(() => fetchAlerts(true), 5000)
})

onUnmounted(() => {
  if (timeTimer) clearInterval(timeTimer)
  if (dataTimer) clearInterval(dataTimer)
  if (alertCheckTimer) clearInterval(alertCheckTimer)
})
</script>

<style scoped>
.dashboard {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #0c1929 0%, #1a2a42 50%, #0c1929 100%);
  display: flex;
  flex-direction: column;
  color: #fff;
  overflow: hidden;
}

.header {
  height: 70px;
  padding: 0 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(0, 0, 0, 0.3);
  border-bottom: 2px solid #1e4d8b;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h1 {
  font-size: 24px;
  font-weight: 600;
  background: linear-gradient(90deg, #4facfe, #00f2fe);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.current-time {
  font-size: 16px;
  color: #4facfe;
  font-family: 'Courier New', monospace;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  padding: 20px 30px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.06);
  border-radius: 10px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 18px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.stat-card.normal { border-left: 4px solid #409EFF; }
.stat-card.normal .stat-icon { color: #409EFF; }
.stat-card.warning { border-left: 4px solid #E6A23C; }
.stat-card.warning .stat-icon { color: #E6A23C; }
.stat-card.success { border-left: 4px solid #67C23A; }
.stat-card.success .stat-icon { color: #67C23A; }
.stat-card.danger { border-left: 4px solid #F56C6C; }
.stat-card.danger .stat-icon { color: #F56C6C; }

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #8aa0bd;
  margin-top: 4px;
}

.main-content {
  flex: 1;
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  padding: 0 30px 20px;
  min-height: 0;
}

.panel {
  background: rgba(255, 255, 255, 0.04);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-header h2 {
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-body {
  flex: 1;
  overflow: auto;
  padding: 12px;
}

.plate-number {
  font-weight: 600;
  color: #4facfe;
  font-family: 'Courier New', monospace;
}

.route {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #c0d0e0;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  padding: 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.alert-item:hover {
  transform: translateX(4px);
}

.danger-alert {
  background: linear-gradient(135deg, rgba(245, 108, 108, 0.2), rgba(245, 108, 108, 0.05));
  border: 1px solid rgba(245, 108, 108, 0.5);
  border-left: 4px solid #F56C6C;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.alert-time {
  font-size: 12px;
  color: #8aa0bd;
}

.alert-plate {
  font-size: 16px;
  font-weight: 700;
  color: #F56C6C;
  margin-bottom: 6px;
}

.alert-message {
  font-size: 13px;
  color: #e0e8f0;
  line-height: 1.5;
  margin-bottom: 8px;
}

.alert-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #8aa0bd;
  gap: 10px;
}

:deep(.el-table) {
  background-color: transparent !important;
  color: #e0e8f0;
}

:deep(.el-table th.el-table__cell) {
  background-color: rgba(79, 172, 254, 0.15) !important;
  color: #4facfe;
  font-weight: 600;
}

:deep(.el-table tr) {
  background-color: transparent !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background-color: rgba(255, 255, 255, 0.03) !important;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

:deep(.el-table::before) {
  display: none;
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell) {
  background-color: rgba(79, 172, 254, 0.1) !important;
}

:deep(.el-dialog) {
  background: #1a2a42 !important;
  border: 1px solid #2a4a72 !important;
}

:deep(.el-dialog__title) {
  color: #F56C6C !important;
  font-size: 18px !important;
  font-weight: 600;
}

:deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #8aa0bd !important;
}

:deep(.el-descriptions__label) {
  background-color: rgba(79, 172, 254, 0.1) !important;
  color: #4facfe !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
  width: 100px;
}

:deep(.el-descriptions__body .el-descriptions__table .el-descriptions__cell) {
  border-color: rgba(255, 255, 255, 0.1) !important;
  color: #e0e8f0 !important;
}

.modal-content {
  text-align: center;
  padding: 10px 0;
}

.modal-alert-icon {
  margin-bottom: 10px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

.modal-alert-title {
  font-size: 24px;
  font-weight: 700;
  color: #F56C6C;
  margin-bottom: 12px;
  letter-spacing: 2px;
}

.modal-alert-message {
  font-size: 15px;
  color: #e0e8f0;
  margin-bottom: 20px;
  line-height: 1.6;
}

.modal-desc {
  text-align: left;
}

:deep(.el-empty__description p) {
  color: #8aa0bd;
}
</style>
