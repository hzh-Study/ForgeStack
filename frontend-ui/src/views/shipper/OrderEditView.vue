<template>
  <section class="grid gap-5">
    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="m-0 text-xl font-semibold text-slate-950">{{ isEdit ? '编辑发货单' : '发布货源' }}</h2>
          <p class="mt-2 text-sm text-slate-500">
            草稿可反复保存，发布后进入订单池；有效期按天维护，提交时换算为小时。
          </p>
        </div>
        <el-tag :type="orderStatusInfo.tagType">{{ orderStatusInfo.label }}</el-tag>
      </div>

      <el-alert
        v-if="!publishQualified"
        class="mt-4"
        type="warning"
        :closable="false"
        show-icon
        title="当前账号尚不具备发布资格"
        description="需要个人实名通过且入驻审核通过后才能发布货源，草稿仍可正常保存。"
      >
        <template #default>
          <div class="mt-2 flex gap-2">
            <el-button size="small" @click="router.push('/realname')">实名认证</el-button>
            <el-button size="small" @click="router.push('/onboarding')">入驻申请</el-button>
          </div>
        </template>
      </el-alert>
      <el-alert
        v-if="isEdit && originalStatus !== 'DRAFT'"
        class="mt-4"
        type="info"
        :closable="false"
        show-icon
        title="当前订单不是草稿，不能继续编辑"
      />

      <el-form ref="formRef" class="mt-5" :model="form" :rules="rules" label-width="120px">
        <div class="grid grid-cols-2 gap-x-8">
          <el-form-item label="运费报价(元)" prop="freightAmount">
            <el-input-number v-model="form.freightAmount" :min="0.01" :precision="2" :controls="false" class="w-full" placeholder="请输入运费" />
          </el-form-item>
          <el-form-item label="有效期(天)" prop="validDays">
            <el-input-number v-model="form.validDays" :min="1" :max="30" class="w-full" />
          </el-form-item>
          <el-form-item label="约定提货时间" prop="pickupTime">
            <el-date-picker
              v-model="form.pickupTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择提货时间"
              class="w-full"
            />
          </el-form-item>
          <el-form-item label="约定送达时间" prop="deliveryTime">
            <el-date-picker
              v-model="form.deliveryTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择送达时间"
              class="w-full"
            />
          </el-form-item>
        </div>
        <el-form-item label="发货备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入备注" />
        </el-form-item>
      </el-form>
    </div>

    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <div class="flex items-center justify-between">
        <h3 class="m-0 text-lg font-semibold text-slate-900">货物明细</h3>
        <el-button :icon="Plus" @click="addGoods">添加货物行</el-button>
      </div>
      <el-table class="mt-4" :data="form.goods" border>
        <el-table-column label="货物名称" min-width="130">
          <template #default="{ row }">
            <el-input v-model="row.goodsName" placeholder="请输入货物名称" />
          </template>
        </el-table-column>
        <el-table-column label="货物类型" min-width="140">
          <template #default="{ row }">
            <el-select v-model="row.goodsType" filterable allow-create default-first-option placeholder="选择或输入编码">
              <el-option v-for="item in GOODS_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="0.01" :precision="2" :controls="false" class="w-full" />
          </template>
        </el-table-column>
        <el-table-column label="重量(吨)" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.weight" :min="0" :precision="2" :controls="false" class="w-full" />
          </template>
        </el-table-column>
        <el-table-column label="体积(m³)" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.volume" :min="0" :precision="2" :controls="false" class="w-full" />
          </template>
        </el-table-column>
        <el-table-column label="单位" width="90">
          <template #default="{ row }">
            <el-input v-model="row.unit" placeholder="件/吨" />
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="130">
          <template #default="{ row }">
            <el-input v-model="row.remark" placeholder="可选" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" :disabled="form.goods.length === 1" @click="removeGoods($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="grid grid-cols-2 gap-5">
      <div v-for="addressInfo in addressSections" :key="addressInfo.key" class="rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="m-0 text-lg font-semibold text-slate-900">{{ addressInfo.title }}</h3>
        <el-form class="mt-4" :model="form[addressInfo.key]" label-width="90px">
          <el-form-item label="省市区">
            <div class="flex w-full gap-2">
              <el-input v-model="form[addressInfo.key].province" placeholder="省" />
              <el-input v-model="form[addressInfo.key].city" placeholder="市" />
              <el-input v-model="form[addressInfo.key].district" placeholder="区/县" />
            </div>
          </el-form-item>
          <el-form-item label="详细地址">
            <el-input v-model="form[addressInfo.key].detail" placeholder="请输入详细地址" maxlength="120" />
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-model="form[addressInfo.key].contactName" placeholder="请输入联系人姓名" maxlength="50" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form[addressInfo.key].contactPhone" placeholder="请输入联系人手机号" maxlength="11" />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <div class="flex justify-end gap-3">
        <el-button @click="router.push('/shipper/orders')">返回列表</el-button>
        <el-button :loading="saving" :disabled="readonly" @click="handleSave(false)">保存草稿</el-button>
        <el-button type="primary" :loading="publishing" :disabled="readonly" @click="handleSave(true)">保存并发布</el-button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMyOrderDetail, publishOrder, saveOrder } from '@/api/orders'
import { GOODS_TYPE_OPTIONS, ORDER_STATUS_MAP } from '@/constants/business'
import {
  createDefaultOrderForm,
  createEmptyGoods,
  fromOrderDetail,
  toOrderSavePayload
} from '@/models/order'
import { useAuthStore } from '@/stores/auth'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const form = reactive(createDefaultOrderForm())
const saving = ref(false)
const publishing = ref(false)
const originalStatus = ref('DRAFT')

const isEdit = computed(() => Boolean(route.params.id))

/**
 * 已实名且入驻通过才能发布货源，草稿不受限制。
 */
const publishQualified = computed(
  () => authStore.accountInfo?.realnameStatus === 'PASSED' && authStore.accountInfo?.onboardingStatus === 'APPROVED'
)

/**
 * 非草稿订单禁止继续编辑，避免覆盖已进入业务的订单。
 */
const readonly = computed(() => isEdit.value && originalStatus.value !== 'DRAFT')

/**
 * 当前订单状态展示信息。
 */
const orderStatusInfo = computed(() => ORDER_STATUS_MAP[originalStatus.value] || { label: '草稿', tagType: 'info' })

const addressSections = [
  { key: 'pickup', title: '装货点' },
  { key: 'delivery', title: '卸货点' }
]

const rules = {
  freightAmount: [{ required: true, message: '请输入运费报价', trigger: 'blur' }],
  validDays: [{ required: true, message: '请输入有效期', trigger: 'blur' }],
  pickupTime: [{ required: true, message: '请选择约定提货时间', trigger: 'change' }],
  deliveryTime: [{ required: true, message: '请选择约定送达时间', trigger: 'change' }],
  remark: [{ max: 500, message: '备注不能超过500个字符', trigger: 'blur' }]
}

/**
 * 新增一行空白货物明细。
 */
function addGoods() {
  form.goods.push(createEmptyGoods())
}

/**
 * 删除指定行的货物明细，至少保留一行。
 *
 * @param {number} index 行索引
 */
function removeGoods(index) {
  if (form.goods.length <= 1) {
    return
  }
  form.goods.splice(index, 1)
}

/**
 * 校验货物明细与地址等非 el-form 字段，返回错误文本。
 *
 * @returns {string} 错误提示，空字符串表示通过
 */
function validateBusinessFields() {
  for (let index = 0; index < form.goods.length; index += 1) {
    const goods = form.goods[index]
    if (!goods.goodsName?.trim()) {
      return `第 ${index + 1} 行货物名称不能为空`
    }
    if (!goods.goodsType?.trim()) {
      return `第 ${index + 1} 行货物类型不能为空`
    }
    if (!goods.quantity || Number(goods.quantity) <= 0) {
      return `第 ${index + 1} 行货物数量必须大于 0`
    }
    if (!goods.unit?.trim()) {
      return `第 ${index + 1} 行计量单位不能为空`
    }
  }
  const addressNames = { pickup: '装货点', delivery: '卸货点' }
  for (const key of ['pickup', 'delivery']) {
    const address = form[key]
    if (!address.province?.trim() || !address.city?.trim() || !address.district?.trim()) {
      return `${addressNames[key]}省市区不能为空`
    }
    if (!address.detail?.trim()) {
      return `${addressNames[key]}详细地址不能为空`
    }
    if (!address.contactName?.trim()) {
      return `${addressNames[key]}联系人不能为空`
    }
    if (!/^1[3-9]\d{9}$/.test(address.contactPhone || '')) {
      return `${addressNames[key]}联系电话格式不正确`
    }
  }
  if (form.pickupTime && form.deliveryTime && form.pickupTime >= form.deliveryTime) {
    return '约定送达时间必须晚于约定提货时间'
  }
  return ''
}

/**
 * 加载草稿详情并回填表单；非草稿订单只读展示。
 */
async function loadOrder() {
  if (!route.params.id) {
    return
  }
  try {
    const result = await getMyOrderDetail(route.params.id)
    if (result.status === false) {
      throw result
    }
    const detail = result.data
    originalStatus.value = detail.orderStatus
    const formData = fromOrderDetail(detail)
    Object.assign(form, formData)
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 保存草稿，可选在保存成功后立即发布。
 *
 * @param {boolean} publish 是否同时发布
 */
async function handleSave(publish) {
  if (readonly.value) {
    showErrorMessage('当前订单不是草稿，不能编辑')
    return
  }
  await formRef.value.validate()
  const businessError = validateBusinessFields()
  if (businessError) {
    showErrorMessage(businessError)
    return
  }
  if (publish && !publishQualified.value) {
    showErrorMessage('实名认证与入驻审核通过后才能发布货源')
    return
  }
  if (publish) {
    try {
      await ElMessageBox.confirm('发布后订单将进入订单池，是否继续？', '发布确认', { type: 'warning' })
    } catch (error) {
      return
    }
  }
  const loadingTarget = publish ? publishing : saving
  loadingTarget.value = true
  try {
    const result = await saveOrder(toOrderSavePayload(form))
    if (result.status === false) {
      throw result
    }
    form.id = result.data.id
    originalStatus.value = result.data.orderStatus
    if (publish) {
      const publishResult = await publishOrder(result.data.id)
      if (publishResult.status === false) {
        throw publishResult
      }
      showSuccessMessage('订单已发布')
      router.push('/shipper/orders')
      return
    }
    showSuccessMessage('草稿已保存')
    if (!route.params.id) {
      router.replace(`/shipper/orders/${result.data.id}/edit`)
    }
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loadingTarget.value = false
  }
}

onMounted(loadOrder)
</script>
