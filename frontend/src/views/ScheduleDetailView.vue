<template>
  <div>
    <!-- Alert Banner -->
    <el-alert
      v-if="alerts.length > 0"
      type="error"
      :title="`当前有 ${alerts.length} 条班次预警，请检查并安排替岗`"
      show-icon
      style="margin-bottom:16px"
      :closable="false"
    >
      <template #default>
        <span v-for="a in alerts" :key="a.id" style="margin-right:12px">
          📅 {{ a.workDate }} · <strong>{{ a.shiftCode }}</strong>({{ a.count }}人)
        </span>
      </template>
    </el-alert>

    <el-card>
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between">
          <div style="display:flex;align-items:center;gap:12px">
            <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
            <span style="font-weight:600">{{ fileInfo?.originalName }}</span>
            <el-tag :type="fileInfo?.scheduleType === 'WEEKLY' ? 'warning' : 'primary'" size="small">
              {{ fileInfo?.scheduleType === 'WEEKLY' ? '周班表' : '月班表' }}
            </el-tag>
          </div>
          <div style="display:flex;gap:8px;align-items:center">
            <el-checkbox v-model="showAlertOnly">仅显示预警日期</el-checkbox>
            <el-input
              v-model="searchName"
              placeholder="搜索姓名"
              size="small"
              style="width:140px"
              clearable
              prefix-icon="Search"
            />
          </div>
        </div>
      </template>

      <!-- Legend: Groups by color -->
      <div class="legend-row" v-if="colorGroups.length > 0">
        <span style="font-size:12px;color:#888;margin-right:8px">班组：</span>
        <span
          v-for="g in colorGroups"
          :key="g.color"
          class="legend-tag"
          :style="{ background: g.color + '22', borderColor: g.color, color: g.color }"
        >
          {{ g.names.join('、') }}
        </span>
      </div>

      <!-- Shift count header (sticky) -->
      <div class="count-header" v-if="dates.length > 0">
        <div class="ch-label">班次</div>
        <div
          v-for="d in filteredDates"
          :key="d"
          class="ch-date"
          :class="{ 'ch-today': d === todayISO }"
        >
          <div>{{ formatDay(d) }}</div>
          <div class="ch-week">{{ formatWeek(d) }}</div>
        </div>
      </div>

      <!-- Shift count rows (A1/A2/C1/C2/F1/F2/E2) -->
      <div
        v-for="shift in SHIFTS"
        :key="shift"
        class="count-row"
      >
        <div class="cr-label">{{ shift }}</div>
        <div
          v-for="d in filteredDates"
          :key="d"
          class="cr-cell"
          :class="getCountClass(shift, d)"
        >
          <el-tooltip
            v-if="getCount(shift, d) === 0"
            :content="`${d} ${shift}班次缺岗！`"
            placement="top"
          >
            <span>{{ getCount(shift, d) }}</span>
          </el-tooltip>
          <span v-else>{{ getCount(shift, d) }}</span>
        </div>
      </div>

      <el-divider />

      <!-- Staff schedule table -->
      <el-table
        :data="filteredStaff"
        v-loading="loading"
        border
        :row-class-name="rowClass"
        size="small"
        style="margin-top:8px"
        :cell-style="() => ({ padding: '4px 0' })"
      >
        <el-table-column label="类别" prop="category" width="90" fixed />
        <el-table-column label="姓名" width="80" fixed>
          <template #default="{ row }">
            <span
              :style="row.nameColor ? { color: row.nameColor, fontWeight: 600 } : {}"
            >{{ row.staffName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          v-for="d in filteredDates"
          :key="d"
          :label="formatDay(d)"
          :class-name="d === todayISO ? 'today-col' : ''"
          width="68"
          align="center"
        >
          <template #header>
            <div :class="{ 'today-header': d === todayISO }">
              {{ formatDay(d) }}<br>
              <small style="color:#aaa">{{ formatWeek(d) }}</small>
            </div>
          </template>
          <template #default="{ row }">
            <ShiftCell
              :record="getRecord(row.staffName, d)"
              @updated="handleCellUpdate"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { scheduleApi } from '@/api/schedule'
import ShiftCell from '@/components/ShiftCell.vue'

const SHIFTS = ['A1', 'A2', 'C1', 'C2', 'F1', 'F2', 'E2']

const route = useRoute()
const fileId = parseInt(route.params.fileId)

const fileInfo = ref(null)
const records = ref([])
const counts = ref([])
const alerts = ref([])
const loading = ref(false)
const searchName = ref('')
const showAlertOnly = ref(false)
const todayISO = new Date().toISOString().slice(0, 10)

// All unique dates from records
const dates = computed(() => {
  const set = new Set(records.value.map(r => r.workDate))
  return [...set].sort()
})

// Alert dates set
const alertDates = computed(() => new Set(alerts.value.map(a => a.workDate)))

const filteredDates = computed(() =>
  showAlertOnly.value ? dates.value.filter(d => alertDates.value.has(d)) : dates.value
)

// Unique staff in order
const staffList = computed(() => {
  const seen = new Set()
  const list = []
  for (const r of records.value) {
    if (!seen.has(r.staffName)) {
      seen.add(r.staffName)
      list.push({ staffName: r.staffName, category: r.category, nameColor: r.nameColor })
    }
  }
  return list
})

const filteredStaff = computed(() =>
  staffList.value.filter(s =>
    !searchName.value || s.staffName.includes(searchName.value)
  )
)

// Record lookup map: staffName_date → record
const recordMap = computed(() => {
  const m = {}
  for (const r of records.value) m[`${r.staffName}_${r.workDate}`] = r
  return m
})

function getRecord(staffName, date) {
  return recordMap.value[`${staffName}_${date}`]
}

// Count lookup map
const countMap = computed(() => {
  const m = {}
  for (const c of counts.value) m[`${c.shiftCode}_${c.workDate}`] = c
  return m
})

function getCount(shift, date) {
  return countMap.value[`${shift}_${date}`]?.count ?? '-'
}

function getCountClass(shift, date) {
  const c = countMap.value[`${shift}_${date}`]
  if (!c) return ''
  return c.alert ? 'count-alert' : 'count-ok'
}

// Color groups for legend
const colorGroups = computed(() => {
  const map = {}
  for (const s of staffList.value) {
    if (s.nameColor && s.nameColor !== '#000000') {
      if (!map[s.nameColor]) map[s.nameColor] = []
      map[s.nameColor].push(s.staffName)
    }
  }
  return Object.entries(map).map(([color, names]) => ({ color, names }))
})

function rowClass({ row }) {
  return row.category ? 'category-row' : ''
}

function formatDay(d) {
  return d ? d.slice(5) : ''
}

function formatWeek(d) {
  const days = ['日', '一', '二', '三', '四', '五', '六']
  return '周' + days[new Date(d).getDay()]
}

async function load() {
  loading.value = true
  try {
    const [files] = await Promise.all([scheduleApi.listFiles()])
    fileInfo.value = files.find(f => f.id === fileId)
    const [recs, cnts, als] = await Promise.all([
      scheduleApi.getRecords(fileId),
      scheduleApi.getCounts(fileId),
      scheduleApi.getAlerts(fileId)
    ])
    records.value = recs
    counts.value = cnts
    alerts.value = als
  } finally {
    loading.value = false
  }
}

async function handleCellUpdate() {
  // Reload counts and alerts after edit
  const [cnts, als] = await Promise.all([
    scheduleApi.getCounts(fileId),
    scheduleApi.getAlerts(fileId)
  ])
  counts.value = cnts
  alerts.value = als
}

onMounted(load)
</script>

<style scoped>
.legend-row {
  display: flex; align-items: center; flex-wrap: wrap;
  margin-bottom: 12px; gap: 6px;
}
.legend-tag {
  padding: 2px 10px; border-radius: 20px;
  border: 1.5px solid; font-size: 12px; font-weight: 500;
}
.count-header {
  display: flex; background: #fafafa; border: 1px solid #eee;
  border-radius: 6px; margin-bottom: 4px;
}
.ch-label { width: 48px; text-align: center; padding: 6px 0; font-size: 12px; color: #888; }
.ch-date {
  flex: 1; text-align: center; padding: 4px 0; font-size: 11px;
  color: #555; border-left: 1px solid #eee; min-width: 60px;
}
.ch-week { color: #aaa; font-size: 10px; }
.ch-today { background: #e6f7ff; color: #1890ff; font-weight: 600; }
.count-row { display: flex; border-bottom: 1px solid #f0f0f0; }
.cr-label {
  width: 48px; text-align: center; padding: 4px 0;
  font-size: 12px; font-weight: 600; color: #333;
}
.cr-cell {
  flex: 1; text-align: center; padding: 4px 0;
  font-size: 12px; border-left: 1px solid #f0f0f0; min-width: 60px;
}
.count-alert { background: #fff1f0; color: #f5222d; font-weight: 700; }
.count-ok { color: #52c41a; }
.today-col { background: #e6f7ff !important; }
.today-header { color: #1890ff; font-weight: 700; }
</style>
