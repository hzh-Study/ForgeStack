<template>
  <section class="grid gap-5">
    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="m-0 text-xl font-semibold text-slate-950">入驻申请</h2>
          <p class="mt-2 text-sm text-slate-500">
            实名通过后可提交企业主体资料，管理员人工审核通过后才能发布货源或查看订单池。
          </p>
        </div>
        <el-tag v-if="onboarding" :type="statusInfo.tagType" size="large">{{ statusInfo.label }}</el-tag>
      </div>

      <el-alert
        v-if="!realnamePassed"
        class="mt-5"
        type="warning"
        :closable="false"
        show-icon
        title="个人实名尚未通过"
        description="请先完成实名认证，通过后才能提交入驻申请。"
      >
        <template #default>
          <el-button size="small" class="mt-2" @click="router.push('/realname')">前往实名认证</el-button>
        </template>
      </el-alert>

      <el-alert
        v-if="onboarding && onboarding.auditStatus === 'PENDING'"
        class="mt-5"
        type="warning"
        :closable="false"
        show-icon
        title="入驻申请正在审核中"
        description="审核完成前不可重复提交，请耐心等待管理员处理。"
      />
      <el-alert
        v-if="onboarding && onboarding.auditStatus === 'APPROVED'"
        class="mt-5"
        type="success"
        :closable="false"
        show-icon
        title="入驻审核已通过"
        description="当前账号已具备平台业务资格，无需重复提交入驻申请。"
      />
      <el-alert
        v-if="onboarding && onboarding.auditStatus === 'REJECTED'"
        class="mt-5"
        type="error"
        :closable="false"
        show-icon
        title="入驻申请已驳回"
        :description="onboarding.rejectReason || '请修改资料后重新提交'"
      />

      <el-descriptions v-if="onboarding" class="mt-5" :column="2" border>
        <el-descriptions-item label="主体名称">{{ onboarding.subjectName }}</el-descriptions-item>
        <el-descriptions-item label="统一社会信用代码">{{ onboarding.creditCode }}</el-descriptions-item>
        <el-descriptions-item label="法定代表人">{{ onboarding.legalRepName }}</el-descriptions-item>
        <el-descriptions-item label="法人证件号码">{{ onboarding.legalRepIdNo }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ onboarding.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ onboarding.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="所在地区">
          {{ [onboarding.province, onboarding.city, onboarding.district].filter(Boolean).join(' / ') || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatDateTime(onboarding.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="经营范围" :span="2">{{ onboarding.businessScope || '-' }}</el-descriptions-item>
        <el-descriptions-item label="资质附件" :span="2">
          <FileAttachmentList :files="onboarding.files || []" />
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <div v-if="showForm" class="rounded-lg border border-slate-200 bg-white p-6">
      <h3 class="m-0 text-lg font-semibold text-slate-900">
        {{ onboarding ? '修改并重新提交入驻资料' : '提交入驻资料' }}
      </h3>
      <el-form ref="formRef" class="mt-5" :model="form" :rules="rules" label-width="130px">
        <el-form-item label="企业主体名称" prop="subjectName">
          <el-input v-model="form.subjectName" placeholder="请输入营业执照上的主体名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="统一社会信用代码" prop="creditCode">
          <el-input v-model="form.creditCode" placeholder="请输入统一社会信用代码" maxlength="30" />
        </el-form-item>
        <el-form-item label="法定代表人" prop="legalRepName">
          <el-input v-model="form.legalRepName" placeholder="请输入法定代表人姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="法人证件类型" prop="legalRepIdType">
          <el-select v-model="form.legalRepIdType" class="w-60">
            <el-option v-for="item in ID_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="法人证件号码" prop="legalRepIdNo">
          <el-input v-model="form.legalRepIdNo" placeholder="请输入法定代表人证件号码" maxlength="30" />
        </el-form-item>
        <el-form-item label="联系人姓名" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入业务联系人姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="联系人电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系人手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在地区">
          <div class="flex w-full gap-2">
            <el-input v-model="form.province" placeholder="省" />
            <el-input v-model="form.city" placeholder="市" />
            <el-input v-model="form.district" placeholder="区/县" />
          </div>
        </el-form-item>
        <el-form-item label="经营范围">
          <el-input v-model="form.businessScope" type="textarea" :rows="3" placeholder="请输入经营范围" maxlength="300" />
        </el-form-item>
        <el-form-item label="资质附件">
          <AuthUpload v-model="form.attachmentPaths" module="onboarding" file-type="license" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" :disabled="!realnamePassed" @click="handleSubmit">
            提交入驻申请
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getOnboarding, submitOnboarding } from '@/api/onboarding'
import AuthUpload from '@/components/AuthUpload.vue'
import FileAttachmentList from '@/components/FileAttachmentList.vue'
import { AUDIT_STATUS_MAP, ID_TYPE_OPTIONS } from '@/constants/business'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const router = useRouter()
const authStore = useAuthStore()
const onboarding = ref(null)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  subjectName: '',
  creditCode: '',
  legalRepName: '',
  legalRepIdType: 'ID_CARD',
  legalRepIdNo: '',
  contactName: '',
  contactPhone: '',
  province: '',
  city: '',
  district: '',
  businessScope: '',
  attachmentPaths: []
})

/**
 * 个人实名通过后才能提交入驻申请。
 */
const realnamePassed = computed(() => authStore.accountInfo?.realnameStatus === 'PASSED')

/**
 * 待审核或已通过时只展示结果，已驳回时允许修改后重新提交。
 */
const showForm = computed(() => !onboarding.value || onboarding.value.auditStatus === 'REJECTED')

/**
 * 当前入驻审核状态展示信息。
 */
const statusInfo = computed(() => AUDIT_STATUS_MAP[onboarding.value?.auditStatus] || { label: '未提交', tagType: 'info' })

const rules = {
  subjectName: [{ required: true, message: '请输入企业主体名称', trigger: 'blur' }],
  creditCode: [{ required: true, message: '请输入统一社会信用代码', trigger: 'blur' }],
  legalRepName: [{ required: true, message: '请输入法定代表人姓名', trigger: 'blur' }],
  legalRepIdType: [{ required: true, message: '请选择法人证件类型', trigger: 'change' }],
  legalRepIdNo: [{ required: true, message: '请输入法人证件号码', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系人电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '联系电话格式不正确', trigger: 'blur' }
  ]
}

/**
 * 加载当前账号最近一次入驻申请并回填可编辑字段。
 */
async function loadOnboarding() {
  try {
    const result = await getOnboarding()
    if (result.status === false) {
      throw result
    }
    onboarding.value = result.data || null
    if (!onboarding.value) {
      form.contactName = authStore.accountInfo?.name || ''
      form.contactPhone = authStore.accountInfo?.username || ''
      return
    }
    Object.assign(form, {
      subjectName: onboarding.value.subjectName || '',
      creditCode: onboarding.value.creditCode || '',
      legalRepName: onboarding.value.legalRepName || '',
      legalRepIdType: onboarding.value.legalRepIdType || 'ID_CARD',
      legalRepIdNo: onboarding.value.legalRepIdNo || '',
      contactName: onboarding.value.contactName || '',
      contactPhone: onboarding.value.contactPhone || '',
      province: onboarding.value.province || '',
      city: onboarding.value.city || '',
      district: onboarding.value.district || '',
      businessScope: onboarding.value.businessScope || '',
      attachmentPaths: (onboarding.value.files || []).map((file) => file.filePath).filter(Boolean)
    })
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 提交入驻申请，成功后重新加载状态与最新附件列表。
 */
async function handleSubmit() {
  if (!realnamePassed.value) {
    showErrorMessage('请先完成实名认证并通过校验')
    return
  }
  await formRef.value.validate()
  submitting.value = true
  try {
    const result = await submitOnboarding({
      subjectName: form.subjectName,
      creditCode: form.creditCode,
      legalRepName: form.legalRepName,
      legalRepIdType: form.legalRepIdType,
      legalRepIdNo: form.legalRepIdNo,
      contactName: form.contactName,
      contactPhone: form.contactPhone,
      province: form.province,
      city: form.city,
      district: form.district,
      businessScope: form.businessScope,
      attachmentPaths: form.attachmentPaths
    })
    if (result.status === false) {
      throw result
    }
    onboarding.value = result.data
    await authStore.fetchProfile()
    showSuccessMessage('入驻申请已提交，请等待管理员审核')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

onMounted(loadOnboarding)
</script>
