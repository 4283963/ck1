import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import VehicleDetail from '../views/VehicleDetail.vue'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: Dashboard,
    meta: { title: '协同大屏' }
  },
  {
    path: '/vehicle/:id',
    name: 'VehicleDetail',
    component: VehicleDetail,
    meta: { title: '车次详情' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '疫苗冷链运输协同系统'
  next()
})

export default router
