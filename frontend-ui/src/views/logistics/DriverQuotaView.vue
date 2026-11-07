<template>
  <section class="grid gap-5">
    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <h2 class="m-0 text-xl font-semibold text-slate-950">司机额度设置</h2>
      <p class="mt-2 text-sm text-slate-500">
        按司机与自然月覆盖报销额度，平台默认 5000 元/月；额度变更只影响新提交的报销单。
      </p>

      <el-form ref="formRef" class="mt-5 max-w-[620px]" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="司机" prop="driverAccountId">
          <el-select v-model="form.driverAccountId" filterable placeholder="请选择本公司司机" class="w-full">
            <el-option
              v-for="driver in drivers"
              :key="driver.driverId"
              :label="`${driver.name}（${driver.phone}）`"
              :value="driver.accountId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年月" prop="yearMonth">
          <el-date-picker
            v-model="form.yearMonth"
            type="month"
            value-format="YYYY-MM"
            placeholder="请选择年月"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="月度额度(元)" prop="amount">
          <el-input-number v-model="form.amount" :min="0" :precision="2" :controls="false" class="w-full" placeholder="请输入额度" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存额度</el-button>
        </el-form-item>
      </el-form>

      <el-alert class="mt-2 max-w-[760px]" type="info" :closable="false" show-icon
        title="超限判定按提交时的累计与额度快照计算" description="已提交的报销单不会因本次额度调整而变化。" />
    </div>

    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <h3 class="m-0 text-lg font-semibold text-slate-900">本公司司机</h3>
      <el-table class="mt-4" :data="drivers" border stripe>
        <el-table-column label="姓名" prop="name" min-width="100" />
        <el-table-column label="手机号" prop="phone" min-width="130" />
        <el-table-column label="账号 ID" prop="accountId" width="100" />
        <el-table-column label="绑定车辆" min-width="120">
          <template #default="{ row }">{{ row.vehiclePlateNo || '未绑定' }}</template>
        </el-table-column>
        <el-table-column label="在职状态" width="100">
          <template #default="{ row }">{{ row.employmentStatus === 'ACTIVE' ? '在职' : row.employmentStatus }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无司机" />
        </template>
      </el-table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import moment from 'moment'
import { pageDrivers } from '@/api/drivers'
import { upsertDriverQuota } from '@/api/reimbursements'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const drivers = ref([])
const saving = ref(false)
const formRef = ref()
const form = reactive({
  driverAccountId: null,
  yearMonth: moment().format('YYYY-MM'),
  amount: null
})

const rules = {
  driverAccountId: [{ required: true, message: '请选择司机', trigger: 'change' }],
  yearMonth: [{ required: true, message: '请选择年月', trigger: 'change' }],
  amount: [{ required: true, message: '请输入月度额度', trigger: 'blur' }]
}

/**
 * 加载本公司司机列表，额度按司机账号 ID 覆盖。
 */
async function loadDrivers() {
  try {
    const result = await pageDrivers({ pageNum: 1, pageSize: 100 })
    if (result.status === false) {
      throw result
    }
    drivers.value = result.data?.records || []
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 保存司机月度额度覆盖。
 */
async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    const result = await upsertDriverQuota({
      driverAccountId: form.driverAccountId,
      yearMonth: form.yearMonth,
      amount: form.amount
    })
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('司机额度已保存')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    saving.value = false
  }
}

onMounted(loadDrivers)
</script>
