import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/views/LayoutView.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'schedule',
        name: 'Schedule',
        component: () => import('@/views/ScheduleView.vue'),
        meta: { title: '排班管理' }
      },
      {
        path: 'schedule/:fileId',
        name: 'ScheduleDetail',
        component: () => import('@/views/ScheduleDetailView.vue'),
        meta: { title: '班表详情' }
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/LogView.vue'),
        meta: { title: '操作日志', role: 'ADMIN' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.token) {
    return '/login'
  }
})

export default router
