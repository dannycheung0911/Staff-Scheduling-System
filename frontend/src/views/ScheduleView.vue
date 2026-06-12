<template>
  <div>
    <!-- Upload card -->
    <el-card style="margin-bottom:20px">
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between">
          <span style="font-weight:600">上传班表</span>
          <el-tag type="info" size="small">支持 月班表 / 周班表</el-tag>
        </div>
      </template>
      <el-upload
        drag
        accept=".xlsx,.xls"
        :before-upload="handleUpload"
        :show-file-list="false"
      >
        <el-icon size="48" color="#c0c4cc"><Upload /></el-icon>
        <div style="margin-top:12px;color:#606266">
          拖拽文件到此处，或<em style="color:#409eff">点击上传</em>
        </div>
        <div style="font-size:12px;color:#aaa;margin-top:4px">支持 .xlsx / .xls 格式</div>
      </el-upload>
      <el-progress
        v-if="uploading"
        :percentage="uploadPct"
        style="margin-top:12px"
        status="striped"
        striped-flow
      />
    </el-card>

    <!-- File list -->
    <el-card>
      <template #header>
        <span style="font-weight:600">班表文件列表</span>
      </template>
      <el-table :data="files" v-loading="loading" stripe>
        <el-table-column prop="originalName" label="文件名" min-width="200" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.scheduleType === 'WEEKLY' ? 'warning' : 'primary'" size="small">
              {{ row.scheduleType === 'WEEKLY' ? '周班表' : '月班表' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="周期" width="160">
          <template #default="{ row }">
            <span v-if="row.scheduleType === 'WEEKLY'">{{ row.weekRange }}</span>
            <span v-else>{{ row.year }}年{{ row.month }}月</span>
          </template>
        </el-table-column>
        <el-table-column prop="uploadedBy" label="上传人" width="100" />
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }">{{ formatDate(row.uploadTime) }}</template>
        </el-table-column>
        <el-table-column label="预警" width="80" align="center">
          <template #default="{ row }">
            <el-badge :value="alertMap[row.id] || 0" :hidden="!alertMap[row.id]" type="danger">
              <el-icon color="#f56c6c" v-if="alertMap[row.id]"><Warning /></el-icon>
              <el-icon color="#67c23a" v-else><CircleCheck /></el-icon>
            </el-badge>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="$router.push(`/schedule/${row.id}`)">
              <el-icon><Edit /></el-icon> 查看/编辑
            </el-button>
            <el-popconfirm title="确认删除此班表？" @confirm="deleteFile(row.id)">
              <template #reference>
                <el-button type="danger" text size="small"><el-icon><Delete /></el-icon></el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
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

async function loadFiles() {
  loading.value = true
  try {
    files.value = await scheduleApi.listFiles()
    for (const f of files.value) {
      const alerts = await scheduleApi.getAlerts(f.id)
      alertMap.value[f.id] = alerts.length
    }
  } finally {
    loading.value = false
  }
}

async function handleUpload(file) {
  uploading.value = true
  uploadPct.value = 0
  try {
    await scheduleApi.upload(file, (e) => {
      uploadPct.value = Math.round(e.loaded / e.total * 100)
    })
    ElMessage.success('上传并解析成功！')
    await loadFiles()
  } catch (e) {
    // error handled in http.js
  } finally {
    uploading.value = false
  }
  return false  // prevent default upload
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
