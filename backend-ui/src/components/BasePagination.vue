<template>
  <div class="mt-5 flex justify-end">
    <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
      :page-sizes="[10, 20, 50]" :teleported="false" layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleChange" @current-change="handleChange" />
  </div>
</template>

<script setup>
defineProps({
  /**
   * 后端返回的总记录数
   */
  total: {
    type: Number,
    default: 0
  }
})

const pageNum = defineModel('pageNum', { type: Number, default: 1 })
const pageSize = defineModel('pageSize', { type: Number, default: 10 })
const emit = defineEmits(['change'])

/**
 * 页码或每页条数变化后通知父页面重新加载列表。
 */
function handleChange() {
  emit('change')
}
</script>
