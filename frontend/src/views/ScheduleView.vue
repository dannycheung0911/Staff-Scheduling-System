<template>
  <div>
    <!-- 上传卡片 -->
    <el-card style="margin-bottom:20px">
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between">
          <span style="font-weight:600">上传班表</span>
        </div>
      </template>

      <!-- 第一步：选择模板类型 -->
      <div class="upload-step">
        <div class="step-label">第一步：选择班表类型</div>
        <el-radio-group v-model="scheduleType" size="large">
          <el-radio-button value="MONTHLY">
            <el-icon><Calendar /></el-icon> 月班表
          </el-radio-button>
          <el-radio-button value="WEEKLY">
            <el-icon><Date /></el-icon> 周班表
          </el-radio-button>
        </el-radio-group>
        <el-tag :type="scheduleType === 'MONTHLY' ? 'primary' : 'warning'" style="margin-left:12px">
          {{ scheduleType === 'MONTHLY' ? '适用于整月排班表（如：5月班表）' : '适用于单周排班表（如：6月15日-6月21日）' }}
        </el-tag>
      </div>

      <!-- 第二步：上传文件 -->
      <div class="upload-step">
        <div class="step-label">第二步：上传文件</div>
        <el-upload
          drag
          accept=".xlsx,.xls"
          :before-upload="handleUpload"
          :show-file-list="false"
          :disabled="uploading"
        >
          <el-icon size="48" color="#c0c4cc"><Upload /></el-icon>
          <div style="margin-top:12px;color:#606266">
            拖拽文件到此处，或<em style="color:#409eff">点击上传</em>
          </div>
          <div style="font-size:12px;color:#aaa;margin-top:4px">
            支持 .xlsx / .xls 格式，当前模式：
            <strong :style="{ color: scheduleType === 'MONTHLY' ? '#409eff' : '#e6a23c' }">
              {{ scheduleType === 'MONTHLY' ? '月班表' : '周班表' }}
            </strong>
          </div>
        </el-upload>
        <el-progress
          v-if="uploading"
          :percentage="uploadPct"
          style="margin-top:12px"
          status="striped"
          striped-flow
          :duration="6"
        />
      </div>
    </el-card>

    <!-- 文件列表 -->
    <el-card>
      <template #header>
        <span style="font-weight:600">班表文件列表</span>
      </template>
      <el-table :data="files" v-loading="loading" stripe>
        <el-table-column prop="originalName" label="文件名" min-width="220" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.scheduleType === 'WEEKLY' ? 'warning' : 'primary'" size="small">
              {{ row.scheduleType === 'WEEKLY' ? '周班表' : '月班表' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="周期" width="180">
          <template #default="{ row }">
            <span v-if="row.scheduleType === 'WEEKLY'">{{ row.weekRange }}</span>
            <span v-else>{{ row.year }}年{{ row.month }}月</span>
          </template>
        </el-table-column>
        <el-table-column prop="uploadedBy" label="上传人" width="90" />
        <el-table-column label="上传时间" width="155">
          <template #default="{ row }">{{ formatDate(row.uploadTime) }}</template>
        </el-table-column>
        <el-table-column label="预警" width="80" align="center">
          <template #default="{ row }">
            <el-tooltip :content="alertMap[row.id] ? `${alertMap[row.id]}条缺岗预警` : '正常'" placement="top">
              <el-badge :value="alertMap[row.id] || 0" :hidden="!alertMap[row.id]" type="danger">
                <el-icon :color="alertMap[row.id] ? '#f56c6c' : '#67c23a'" size="18">
                  <component :is="alertMap[row.id] ? 'Warning' : 'CircleCheck'" />
                </el-icon>
              </el-badge>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="$router.push(`/schedule/${row.id}`)">
              <el-icon><Edit /></el-icon> 查看/编辑
            </el-button>
            <el-popconfirm title="确认删除此班表？所有记录将一并清除" @confirm="deleteFile(row.id)">
              <template #reference>
                <el-button type="danger" text size="small"><el-icon><Delete /></el-icon></el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && files.length === 0" description="暂无班表文件，请先上传" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { scheduleApi } from '@/api/schedule'

const files = ref([])
const loading = ref(false)
const uploading = ref(false)
const uploadPct = ref(0)
const alertMap = ref({})
const scheduleType = ref('MONTHLY')

async function loadFiles() {
  loading.value = true
  try {
    files.value = await scheduleApi.listFiles()
    // 异步加载预警数
    for (const f of files.value) {
      scheduleApi.getAlerts(f.id).then(alerts => {
        alertMap.value[f.id] = alerts.length
      })
    }
  } finally {
    loading.value = false
  }
}

async function handleUpload(file) {
  uploading.value = true
  uploadPct.value = 0
  try {
    await scheduleApi.upload(file, scheduleType.value, (e) => {
      if (e.total) uploadPct.value = Math.round(e.loaded / e.total * 100)
    })
    ElMessage.success('上传并解析成功！')
    await loadFiles()
  } finally {
    uploading.value = false
    uploadPct.value = 0
  }
  return false // 阻止 el-upload 默认上传
}

async function deleteFile(id) {
  await scheduleApi.deleteFile(id)
  ElMessage.success('已删除')
  await loadFiles()
}

function formatDate(d) {
  return d ? d.replace('T', ' ').slice(0, 16) : ''
}

onMounted(loadFiles)
</script>

<style scoped>
.upload-step {
  margin-bottom: 20px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}
.step-label {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}
</style>
