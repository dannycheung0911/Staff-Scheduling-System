<template>
  <el-container class="app-layout">
    <!-- Sidebar -->
    <el-aside width="220px" class="sidebar">
      <div class="sidebar-logo">
        <el-icon size="22" color="#fff"><Tickets /></el-icon>
        <span>排班管理系统</span>
      </div>
      <el-menu
        router
        :default-active="route.path"
        background-color="#001529"
        text-color="#ffffffa0"
        active-text-color="#ffffff"
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><House /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/schedule">
          <el-icon><Calendar /></el-icon>
          <span>排班管理</span>
        </el-menu-item>
        <el-menu-item v-if="auth.user?.role === 'ADMIN'" index="/logs">
          <el-icon><Document /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- Header -->
      <el-header class="app-header">
        <div class="header-title">{{ route.meta.title || '延安三路站' }}</div>
        <div class="header-right">
          <el-badge :value="alertCount" :hidden="alertCount === 0" type="danger">
            <el-button text @click="showAlertDrawer = true">
              <el-icon><Bell /></el-icon>
              预警提示
            </el-button>
          </el-badge>
          <el-divider direction="vertical" />
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar size="small" :style="{ background: '#409eff' }">
                {{ (auth.user?.realName || 'U').charAt(0) }}
              </el-avatar>
              <span>{{ auth.user?.realName || auth.user?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- Main content -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <!-- Alert Drawer -->
  <el-drawer v-model="showAlertDrawer" title="班次预警" size="380px">
    <div v-if="allAlerts.length === 0" style="text-align:center;color:#999;padding:40px">
      <el-icon size="48"><CircleCheck /></el-icon>
      <p style="margin-top:12px">当前无预警</p>
    </div>
    <div v-else>
      <el-alert
        v-for="alert in allAlerts"
        :key="alert.id"
        type="error"
        :title="`${alert.workDate} | ${alert.shiftCode} 班次缺岗`"
        :description="`当前 ${alert.shiftCode} 人数为 ${alert.count}，需要安排替岗！`"
        show-icon
        style="margin-bottom:10px"
        :closable="false"
      />
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { scheduleApi } from '@/api/schedule'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const showAlertDrawer = ref(false)
const allAlerts = ref([])

const alertCount = computed(() => allAlerts.value.length)

async function loadAlerts() {
  try {
    const files = await scheduleApi.listFiles()
    const alerts = []
    for (const f of files.slice(0, 3)) {
      const a = await scheduleApi.getAlerts(f.id)
      alerts.push(...a)
    }
    allAlerts.value = alerts
  } catch (e) {}
}

onMounted(() => {
  loadAlerts()
  setInterval(loadAlerts, 60000) // refresh every minute
})

function handleCommand(cmd) {
  if (cmd === 'logout') {
    auth.logout()
    router.push('/login')
    ElMessage.success('已退出')
  }
}
</script>

<style scoped>
.app-layout { height: 100vh; }
.sidebar { background: #001529; overflow: hidden; }
.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border-bottom: 1px solid #ffffff15;
}
.sidebar-menu { border-right: none; }
.app-header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,.08);
}
.header-title { font-size: 16px; font-weight: 600; color: #1a1a2e; }
.header-right { display: flex; align-items: center; gap: 12px; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #333; }
.app-main { padding: 20px; background: #f0f2f5; overflow-y: auto; }
</style>
