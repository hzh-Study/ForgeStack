<template>
  <section class="grid gap-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="m-0 text-[22px] font-bold text-slate-950">平台参数</h1>
        <p class="mt-2 text-slate-500">维护订单有效期默认值、提醒时点、月度额度默认值等平台级参数。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增参数</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="paramKey" label="参数键" min-width="200" />
        <el-table-column prop="paramValue" label="参数值" min-width="160" />
        <el-table-column prop="description" label="参数说明" min-width="260" show-overflow-tooltip />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无平台参数" />
        </template>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="editing ? '编辑平台参数' : '新增平台参数'" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="参数键" prop="paramKey">
          <el-input v-model="form.paramKey" maxlength="60" :disabled="editing" placeholder="例如 order.default.valid.hours" />
        </el-form-item>
        <el-form-item label="参数值" prop="paramValue">
          <el-input v-model="form.paramValue" maxlength="200" placeholder="请输入参数值" />
        </el-form-item>
        <el-form-item label="参数说明">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit
            placeholder="说明该参数的用途与取值含义" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Edit, Plus } from '@element-plus/icons-vue'
import { listPlatformParams, savePlatformParam } from '@/api/platform-param'
import { defaultPlatformParamForm } from '@/models/platform-param'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const loading = ref(false)
const saving = ref(false)
const editing = ref(false)
const tableData = ref([])
const formVisible = ref(false)
const formRef = ref()
const form = reactive({ ...defaultPlatformParamForm })

const rules = {
  paramKey: [{ required: true, message: '请输入参数键', trigger: 'blur' }],
  paramValue: [{ required: true, message: '请输入参数值', trigger: 'blur' }]
}

/**
 * 加载全部平台参数。
 */
async function loadData() {
  loading.value = true
  try {
    const result = await listPlatformParams()
    if (result.status === false) {
      throw result
    }
    tableData.value = result.data || []
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}

/**
 * 打开新增参数弹窗。
 */
function openCreate() {
  editing.value = false
  Object.assign(form, { ...defaultPlatformParamForm })
  formVisible.value = true
}

/**
 * 打开编辑参数弹窗，参数键保持只读避免误改键名。
 *
 * @param {Object} row 当前参数行数据
 */
function openEdit(row) {
  editing.value = true
  Object.assign(form, {
    paramKey: row.paramKey,
    paramValue: row.paramValue,
    description: row.description || ''
  })
  formVisible.value = true
}

/**
 * 保存平台参数，新增与修改都通过同一 POST 接口按参数键覆盖。
 */
async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    const result = await savePlatformParam({
      paramKey: form.paramKey.trim(),
      paramValue: form.paramValue.trim(),
      description: form.description?.trim() || null
    })
    if (result.status === false) {
      throw result
    }
    showSuccessMessage(editing.value ? '参数已更新' : '参数新增成功')
    formVisible.value = false
    loadData()
  } catch (error) {
    showErrorMessage(error)
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>
