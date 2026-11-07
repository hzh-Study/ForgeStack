<template>
  <section class="grid gap-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="m-0 text-[22px] font-bold text-slate-950">基础资料</h1>
        <p class="mt-2 text-slate-500">维护货物类型、费用类型、车辆类型与地区字典，供各端业务选择。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增字典项</el-button>
    </div>

    <el-card shadow="never" class="[&_.el-card__body]:pb-0.5">
      <el-form :inline="true" :model="query">
        <el-form-item label="字典类型">
          <el-select v-model="query.dictType" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="item in DICT_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="item in ENABLE_STATUS_OPTIONS" :key="item.value" :label="item.label"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column label="字典类型" width="130">
          <template #default="{ row }">{{ getMappedLabel(DICT_TYPE_NAME_MAP, row.dictType) }}</template>
        </el-table-column>
        <el-table-column prop="itemCode" label="编码" min-width="140" />
        <el-table-column prop="itemName" label="名称" min-width="160" />
        <el-table-column prop="parentCode" label="上级编码" min-width="130" />
        <el-table-column prop="sortNo" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getTagType(ENABLE_STATUS_TAG_MAP, row.status)" size="small">
              {{ getOptionLabel(ENABLE_STATUS_OPTIONS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无字典项" />
        </template>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑字典项' : '新增字典项'" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="字典类型" prop="dictType">
          <el-select v-model="form.dictType" style="width: 100%">
            <el-option v-for="item in DICT_TYPE_OPTIONS" :key="item.value" :label="item.label"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="编码" prop="itemCode">
          <el-input v-model="form.itemCode" maxlength="40" placeholder="同一类型下唯一" />
        </el-form-item>
        <el-form-item label="名称" prop="itemName">
          <el-input v-model="form.itemName" maxlength="60" placeholder="展示给业务端的名称" />
        </el-form-item>
        <el-form-item label="上级编码">
          <el-input v-model="form.parentCode" placeholder="地区层级使用，可选" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNo" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
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
import { Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { listDictItems, saveDictItem } from '@/api/dict'
import {
  DICT_TYPE_NAME_MAP,
  DICT_TYPE_OPTIONS,
  ENABLE_STATUS_OPTIONS,
  ENABLE_STATUS_TAG_MAP,
  getMappedLabel,
  getOptionLabel,
  getTagType
} from '@/constants/logistics'
import { defaultDictForm, defaultDictQuery } from '@/models/dict'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const formVisible = ref(false)
const formRef = ref()
const query = reactive({ ...defaultDictQuery })
const form = reactive({ ...defaultDictForm })

const rules = {
  dictType: [{ required: true, message: '请选择字典类型', trigger: 'change' }],
  itemCode: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  itemName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

/**
 * 按类型与状态加载字典项列表。
 */
async function loadData() {
  loading.value = true
  try {
    const result = await listDictItems({
      dictType: query.dictType || null,
      status: query.status || null
    })
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
 * 重置字典筛选条件并重新加载。
 */
function handleReset() {
  Object.assign(query, { ...defaultDictQuery })
  loadData()
}

/**
 * 打开新增字典项弹窗并恢复默认表单。
 */
function openCreate() {
  Object.assign(form, { ...defaultDictForm })
  formVisible.value = true
}

/**
 * 打开编辑字典项弹窗，复制当前行数据避免污染表格。
 *
 * @param {Object} row 当前字典项行数据
 */
function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    dictType: row.dictType,
    itemCode: row.itemCode,
    itemName: row.itemName,
    parentCode: row.parentCode || '',
    sortNo: row.sortNo ?? 0,
    status: row.status
  })
  formVisible.value = true
}

/**
 * 保存字典项，新增与编辑都由后端同一接口处理。
 */
async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    const result = await saveDictItem({
      id: form.id,
      dictType: form.dictType,
      itemCode: form.itemCode.trim(),
      itemName: form.itemName.trim(),
      parentCode: form.parentCode?.trim() || null,
      sortNo: form.sortNo ?? 0,
      status: form.status
    })
    if (result.status === false) {
      throw result
    }
    showSuccessMessage(form.id ? '编辑成功' : '新增成功')
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
