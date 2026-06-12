<template>
  <el-card>
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between">
        <span style="font-weight:600">操作日志</span>
        <el-input
          v-model="searchUser"
          placeholder="按用户名搜索"
          size="small"
          style="width:180px"
          @change="loadLogs"
          clearable
        />
      </div>
    </template>

    <el-table :data="logs" v-loading="loading" stripe size="small">
      <el-table-column prop="operateTime" label="时间" width="160">
        <template #default="{ row }">{{ row.operateTime?.replace('T', ' ').slice(0, 19) }}</template>
      </el-table-column>
      <el-table-column prop="username" label="操作人" width="100" />
      <el-table-column prop="operation" label="操作" width="100">
        <template #default="{ row }">
          <el-tag :type="opType(row.operation)" size="small">{{ row.operation }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="detail" label="详情" min-width="300" />
      <el-table-column prop="ipAddress" label="IP" width="130" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-icon :color="row.success ? '#52c41a' : '#f5222d'">
            <component :is="row.success ? 'CircleCheck' : 'CircleClose'" />
          </el-icon>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top:16px;justify-content:flex-end;display:flex"
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      @change="loadLogs"
    />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '@/api/http'

const logs = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(50)
const total = ref(0)
const searchUser = ref('')

async function loadLogs() {
  loading.value = true
  try {
    const res = await http.get('/logs', {
      params: { page: page.value - 1, size: size.value, username: searchUser.value }
    })
    logs.value = res.content
    total.value = res.totalElements
  } finally {
    loading.value = false
  }
}

function opType(op) {
  const m = { LOGIN: 'primary', UPLOAD: 'success', EDIT_CELL: 'warning', DELETE: 'danger' }
  return m[op] || 'info'
}

onMounted(loadLogs)
</script>
