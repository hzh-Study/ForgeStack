<template>
  <section class="grid gap-5">
    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="m-0 text-xl font-semibold text-slate-950">实名认证</h2>
          <p class="mt-2 text-sm text-slate-500">
            实名信息通过后不可修改。模拟校验规则：证件号以 9999 结尾判定不通过。
          </p>
        </div>
        <el-tag v-if="realname" :type="statusInfo.tagType" size="large">{{ statusInfo.label }}</el-tag>
      </div>

      <div v-if="loading" class="mt-6 text-sm text-slate-400">加载中...</div>

      <template v-else-if="realname">
        <el-alert
          v-if="realname.verifyStatus === 'FAILED'"
          class="mt-5"
          type="error"
          :closable="false"
          show-icon
          title="实名校验不通过"
          :description="realname.failReason || '证件信息与姓名不匹配'"
        />
        <el-alert
          v-else-if="realname.verifyStatus === 'PASSED'"
          class="mt-5"
          type="success"
          :closable="false"
          show-icon
          title="实名认证已通过"
          description="实名信息不可重复提交或修改，如需变更请联系平台管理员处理。"
        />

        <el-descriptions class="mt-5" :column="2" border>
          <el-descriptions-item label="真实姓名">{{ realname.realName }}</el-descriptions-item>
          <el-descriptions-item label="证件类型">{{ idTypeLabel(realname.idType) }}</el-descriptions-item>
          <el-descriptions-item label="证件号码">{{ realname.idNo }}</el-descriptions-item>
          <el-descriptions-item label="本人手机号">{{ realname.phone }}</el-descriptions-item>
          <el-descriptions-item label="校验时间">{{ formatDateTime(realname.verifyTime) }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(realname.createTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="realname.failReason" label="不通过原因" :span="2">
            <span class="text-red-500">{{ realname.failReason }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <el-alert
        v-if="!realname && !loading"
        class="mt-5"
        type="info"
        :closable="false"
        title="尚未提交实名信息，请填写本人真实身份信息完成校验。"
      />
    </div>

    <div v-if="showForm" class="rounded-lg border border-slate-200 bg-white p-6">
      <h3 class="m-0 text-lg font-semibold text-slate-900">
        {{ realname ? '重新提交实名信息' : '提交实名信息' }}
      </h3>
      <el-form ref="formRef" class="mt-5" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入证件上的真实姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="证件类型" prop="idType">
          <el-select v-model="form.idType" class="w-60">
            <el-option v-for="item in ID_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="证件号码" prop="idNo">
          <el-input v-model="form.idNo" placeholder="请输入证件号码" maxlength="30" />
        </el-form-item>
        <el-form-item label="本人手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入本人手机号" maxlength="11" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交校验</el-button>
        </el-form-item>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getRealname, submitRealname } from '@/api/realname'
import { ID_TYPE_OPTIONS, REALNAME_STATUS_MAP } from '@/constants/business'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const authStore = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const realname = ref(null)
const formRef = ref()
const form = reactive({
  realName: '',
  idType: 'ID_CARD',
  idNo: '',
  phone: ''
})

/**
 * 已通过实名后不再展示提交表单，未提交或校验失败仍可提交。
 */
const showForm = computed(() => !realname.value || realname.value.verifyStatus !== 'PASSED')

/**
 * 当前实名状态名称与标签颜色。
 */
const statusInfo = computed(() => REALNAME_STATUS_MAP[realname.value?.verifyStatus] || { label: '未提交', tagType: 'info' })

const rules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  idType: [{ required: true, message: '请选择证件类型', trigger: 'change' }],
  idNo: [{ required: true, message: '请输入证件号码', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入本人手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

/**
 * 把证件类型编码转换为中文名称。
 *
 * @param {string} idType 证件类型编码
 * @returns {string} 中文名称
 */
function idTypeLabel(idType) {
  return ID_TYPE_OPTIONS.find((item) => item.value === idType)?.label || idType || '-'
}

/**
 * 加载当前账号实名信息并回填表单。
 */
async function loadRealname() {
  loading.value = true
  try {
    const result = await getRealname()
    if (result.status === false) {
      throw result
    }
    realname.value = result.data || null
    if (realname.value) {
      Object.assign(form, {
        realName: realname.value.realName || '',
        idType: realname.value.idType || 'ID_CARD',
        idNo: realname.value.idNo || '',
        phone: realname.value.phone || authStore.accountInfo?.username || ''
      })
    } else {
      form.phone = authStore.accountInfo?.username || ''
    }
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}

/**
 * 提交实名信息并根据同步校验结果给出提示。
 */
async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const result = await submitRealname({
      realName: form.realName,
      idType: form.idType,
      idNo: form.idNo,
      phone: form.phone
    })
    if (result.status === false) {
      throw result
    }
    realname.value = result.data
    await authStore.fetchProfile()
    if (result.data?.verifyStatus === 'PASSED') {
      showSuccessMessage('实名认证已通过')
    } else {
      showErrorMessage(result.data?.failReason || '实名校验不通过')
    }
  } catch (error) {
    showErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

onMounted(loadRealname)
</script>
