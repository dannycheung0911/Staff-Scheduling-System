<template>
  <div class="dashboard">
    <el-row :gutter="16" style="margin-bottom:20px">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background:#e6f7ff;color:#1890ff"><el-icon><Files /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.totalFiles }}</div>
            <div class="stat-label">班表文件</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background:#fff7e6;color:#fa8c16"><el-icon><User /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.totalStaff }}</div>
            <div class="stat-label">在职人员</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background:#fff1f0;color:#f5222d"><el-icon><Warning /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.alertCount }}</div>
            <div class="stat-label">待处理预警</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background:#f6ffed;color:#52c41a"><el-icon><Calendar /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ todayStr }}</div>
            <div class="stat-label">今日日期</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Today's shift status -->
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div style="display:flex;align-items:center;gap:8px">
              <el-icon color="#409eff"><Bell /></el-icon>
              <span style="font-weight:600">今日班次预警</span>
            </div>
          </template>
          <div v-if="todayAlerts.length === 0" class="empty-state">
            <el-icon size="48" color="#52c41a"><CircleCheck /></el-icon>
            <p>今日所有班次均已到岗，无需处理</p>
          </div>
          <div v-else>
            <el-alert
              v-for="a in todayAlerts"
              :key="a.id"
              type="error"
              :closable="false"
              style="margin-bottom:10px"
            >
              <template #title>
                <strong>{{ a.shiftCode }}</strong> 班次缺岗
                — 来源: {{ getFileName(a.fileId) }}
              </template>
              <template #default>
                {{ a.workDate }} 当天 {{ a.shiftCode }} 岗位人数为 <strong>{{ a.count }}</strong>，
                请及时安排替岗人员！
              </template>
            </el-alert>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>
            <span style="font-weight:600">最近上传</span>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="f in recentFiles"
              :key="f.id"
              :timestamp="formatDate(f.uploadTime)"
              placement="top"
            >
              <el-link @click="goToSchedule(f.id)" type="primary">{{ f.originalName }}</el-link>
              <el-tag size="small" :type="f.scheduleType === 'WEEKLY' ? 'warning' : 'primary'" style="margin-left:6px">
                {{ f.scheduleType === 'WEEKLY' ? '周班表' : '月班表' }}
              </el-tag>
            </el-timeline-item>
          </el-timeline>
          <div v-if="recentFiles.length === 0" class="empty-state">暂无上传记录</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { scheduleApi } from '@/api/schedule'

const router = useRouter()
const files = ref([])
const allAlerts = ref([])

const todayStr = new Date().toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
const todayISO = new Date().toISOString().slice(0, 10)

const recentFiles = computed(() => files.value.slice(0, 5))

const todayAlerts = computed(() =>
  allAlerts.value.filter(a => a.workDate === todayISO)
)

const stats = computed(() => ({
  totalFiles: files.value.length,
  totalStaff: 18, // static for now
  alertCount: allAlerts.value.length
}))

function getFileName(fileId) {
  return files.value.find(f => f.id === fileId)?.originalName || ''
}

function formatDate(d) {
  return d ? d.replace('T', ' ').slice(0, 16) : ''
}

function goToSchedule(fileId) {
  router.push(`/schedule/${fileId}`)
}

onMounted(async () => {
  files.value = await scheduleApi.listFiles()
  for (const f of files.value.slice(0, 3)) {
    const a = await scheduleApi.getAlerts(f.id)
    allAlerts.value.push(...a)
  }
})
</script>

<style scoped>
.stat-card :deep(.el-card__body) {
  display: flex; align-items: center; gap: 16px; padding: 20px;
}
.stat-icon {
  width: 48px; height: 48px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; font-size: 22px;
}
.stat-num { font-size: 28px; font-weight: 700; color: #1a1a2e; }
.stat-label { font-size: 13px; color: #888; margin-top: 2px; }
.empty-state { text-align: center; padding: 30px; color: #999; }
.empty-state p { margin-top: 12px; }
</style>
