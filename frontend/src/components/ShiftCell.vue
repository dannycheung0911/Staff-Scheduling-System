<template>
  <div class="shift-cell" @dblclick="startEdit">
    <template v-if="!editing">
      <el-tag
        v-if="record"
        :type="shiftType(record.shiftCode)"
        size="small"
        class="shift-tag"
        :class="{ 'edited': record.manuallyEdited }"
      >
        {{ record.shiftCode || '休' }}
      </el-tag>
      <span v-else class="empty-cell">—</span>
    </template>
    <template v-else>
      <el-select
        v-model="editValue"
        size="small"
        style="width:80px"
        @change="saveEdit"
        @blur="cancelEdit"
        ref="selectRef"
        :teleported="false"
      >
        <el-option v-for="opt in shiftOptions" :key="opt" :value="opt" :label="opt">
          <el-tag :type="shiftType(opt)" size="small">{{ opt }}</el-tag>
        </el-option>
      </el-select>
    </template>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { scheduleApi } from '@/api/schedule'

const props = defineProps({ record: Object })
const emit = defineEmits(['updated'])

const editing = ref(false)
const editValue = ref('')
const selectRef = ref()

const shiftOptions = [
  'A1', 'A2', 'C1', 'C2', 'F1', 'F2', 'E2',
  '跟A1', '跟F1', '跟F2', '跟E2', '跟C1', '跟C2',
  '休', '白', '年', '婚', '支'
]

function shiftType(code) {
  if (!code || code === '休' || code === '年' || code === '婚') return 'info'
  if (code.startsWith('A')) return 'primary'
  if (code.startsWith('C')) return 'success'
  if (code.startsWith('F')) return 'warning'
  if (code.startsWith('E')) return 'danger'
  if (code.startsWith('跟')) return ''
  return 'info'
}

async function startEdit() {
  if (!props.record) return
  editing.value = true
  editValue.value = props.record.shiftCode
  await nextTick()
  selectRef.value?.focus()
}

async function saveEdit() {
  if (!props.record || editValue.value === props.record.shiftCode) {
    cancelEdit()
    return
  }
  try {
    await scheduleApi.updateCell(props.record.id, editValue.value)
    props.record.shiftCode = editValue.value
    props.record.manuallyEdited = true
    ElMessage.success('已更新')
    emit('updated')
  } catch (e) {}
  editing.value = false
}

function cancelEdit() {
  editing.value = false
}
</script>

<style scoped>
.shift-cell {
  min-height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 2px;
}
.shift-tag { cursor: pointer; }
.shift-tag.edited { outline: 2px solid #faad14; outline-offset: 1px; }
.empty-cell { color: #ddd; font-size: 12px; }
</style>
