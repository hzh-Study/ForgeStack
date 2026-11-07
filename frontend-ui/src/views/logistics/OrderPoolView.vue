<template>
  <section class="grid gap-5">
    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="m-0 text-xl font-semibold text-slate-950">订单池</h2>
          <p class="mt-2 text-sm text-slate-500">
            只展示省市区概要、货物、时间与运费；接单前不返回收货联系人、详细地址与备注。
          </p>
        </div>
        <el-button @click="loadList">刷新订单池</el-button>
      </div>

      <el-form class="mt-5" :inline="true" :model="query">
        <el-form-item label="装货省">
          <el-input v-model="query.pickupProvince" placeholder="如 北京市" class="w-32" clearable />
        </el-form-item>
        <el-form-item label="装货市">
          <el-input v-model="query.pickupCity" placeholder="如 北京市" class="w-32" clearable />
        </el-form-item>
        <el-form-item label="卸货省">
          <el-input v-model="query.deliveryProvince" placeholder="如 广东省" class="w-32" clearable />
        </el-form-item>
        <el-form-item label="卸货市">
          <el-input v-model="query.deliveryCity" placeholder="如 广州市" class="w-32" clearable />
        </el-form-item>
        <el-form-item label="货物类型">
          <el-select v-model="query.goodsType" placeholder="全部" clearable class="w-36">
            <el-option v-for="item in GOODS_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="运费区间">
          <el-input-number v-model="query.minFreight" :min="0" :controls="false" placeholder="最低" class="w-24" />
          <span class="mx-1 text-slate-400">-</span>
          <el-input-number v-model="query.maxFreight" :min="0" :controls="false" placeholder="最高" class="w-24" />
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="query.sortBy" class="w-32">
            <el-option label="发布时间" value="publishTime" />
            <el-option label="剩余时间" value="expireTime" />
            <el-option label="运费" value="freightAmount" />
          </el-select>
          <el-select v-model="query.sortOrder" class="ml-2 w-24">
            <el-option label="降序" value="desc" />
            <el-option label="升序" value="asc" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column label="订单号" prop="orderNo" min-width="150" />
        <el-table-column label="装货地" min-width="140">
          <template #default="{ row }">{{ regionText(row, 'pickup') }}</template>
        </el-table-column>
        <el-table-column label="卸货地" min-width="140">
          <template #default="{ row }">{{ regionText(row, 'delivery') }}</template>
        </el-table-column>
        <el-table-column label="货物" min-width="180">
          <template #default="{ row }">
            <span v-for="(goods, index) in row.goods || []" :key="goods.id || index">
              {{ goods.goodsName }} × {{ goods.quantity }}{{ goods.unit }}
              <span v-if="index < (row.goods || []).length - 1">；</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="约定时间" min-width="150">
          <template #default="{ row }">
            <div class="text-xs">提 {{ formatDateTime(row.pickupTime, 'MM-DD HH:mm') }}</div>
            <div class="text-xs">送 {{ formatDateTime(row.deliveryTime, 'MM-DD HH:mm') }}</div>
          </template>
        </el-table-column>
        <el-table-column label="运费(元)" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.freightAmount) }}</template>
        </el-table-column>
        <el-table-column label="剩余" width="120">
          <template #default="{ row }">{{ formatRemainingTime(row.remainingSeconds) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
            <el-button link type="success" @click="handleAccept(row)">接单</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="当前没有可接订单" />
        </template>
      </el-table>

      <div class="mt-5 flex justify-end">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next, total"
          @current-change="loadList"
          @size-change="handleSearch"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="订单池详情" size="620px">
      <div v-if="detailLoading" class="py-10 text-center text-sm text-slate-400">加载中...</div>
      <template v-else-if="detail">
        <p class="m-0 text-lg font-semibold text-slate-900">{{ detail.orderNo }}</p>
        <el-descriptions class="mt-4" :column="2" border>
          <el-descriptions-item label="装货地">{{ regionText(detail, 'pickup') }}</el-descriptions-item>
          <el-descriptions-item label="卸货地">{{ regionText(detail, 'delivery') }}</el-descriptions-item>
          <el-descriptions-item label="约定提货">{{ formatDateTime(detail.pickupTime) }}</el-descriptions-item>
          <el-descriptions-item label="约定送达">{{ formatDateTime(detail.deliveryTime) }}</el-descriptions-item>
          <el-descriptions-item label="运费报价">{{ formatMoney(detail.freightAmount) }} 元</el-descriptions-item>
          <el-descriptions-item label="有效期">{{ Math.round((detail.validHours || 0) / 24) }} 天</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ formatDateTime(detail.publishTime) }}</el-descriptions-item>
          <el-descriptions-item label="剩余时间">{{ formatRemainingTime(detail.remainingSeconds) }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="mt-5 text-sm font-semibold text-slate-800">货物概要</h4>
        <el-table class="mt-2" :data="detail.goods || []" border size="small">
          <el-table-column label="货物名称" prop="goodsName" min-width="110" />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">{{ GOODS_TYPE_MAP[row.goodsType] || row.goodsType || '-' }}</template>
          </el-table-column>
          <el-table-column label="数量" width="90">
            <template #default="{ row }">{{ row.quantity }}{{ row.unit }}</template>
          </el-table-column>
        </el-table>
        <el-alert class="mt-4" type="info" :closable="false" title="接单前不展示收货联系人、电话、详细地址与备注" />
        <div class="mt-5 flex justify-end">
          <el-button type="success" :loading="accepting" @click="handleAccept(detail)">接单</el-button>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { acceptOrder, getOrderPoolDetail, pageOrderPool } from '@/api/orderPool'
import { GOODS_TYPE_MAP, GOODS_TYPE_OPTIONS } from '@/constants/business'
import { formatDateTime, formatMoney, formatRemainingTime } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const query = reactive({
  pickupProvince: '',
  pickupCity: '',
  deliveryProvince: '',
  deliveryCity: '',
  goodsType: '',
  minFreight: null,
  maxFreight: null,
  sortBy: 'publishTime',
  sortOrder: 'desc',
  pageNum: 1,
  pageSize: 10
})
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const accepting = ref(false)

/**
 * 拼接省市区概要，订单池不返回详细地址。
 *
 * @param {Object} row 订单行
 * @param {string} prefix pickup 或 delivery
 * @returns {string} 地区概要
 */
function regionText(row, prefix) {
  return [row[`${prefix}Province`], row[`${prefix}City`], row[`${prefix}District`]].filter(Boolean).join(' ') || '-'
}

/**
 * 加载订单池公开列表。
 */
async function loadList() {
  loading.value = true
  try {
    const result = await pageOrderPool({
      pickupProvince: query.pickupProvince || null,
      pickupCity: query.pickupCity || null,
      deliveryProvince: query.deliveryProvince || null,
      deliveryCity: query.deliveryCity || null,
      goodsType: query.goodsType || null,
      minFreight: query.minFreight,
      maxFreight: query.maxFreight,
      sortBy: query.sortBy,
      sortOrder: query.sortOrder,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    if (result.status === false) {
      throw result
    }
    records.value = result.data?.records || []
    total.value = Number(result.data?.total || 0)
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}

/**
 * 查询时回到第一页。
 */
function handleSearch() {
  query.pageNum = 1
  loadList()
}

/**
 * 重置筛选条件。
 */
function handleReset() {
  Object.assign(query, {
    pickupProvince: '',
    pickupCity: '',
    deliveryProvince: '',
    deliveryCity: '',
    goodsType: '',
    minFreight: null,
    maxFreight: null,
    sortBy: 'publishTime',
    sortOrder: 'desc',
    pageNum: 1,
    pageSize: 10
  })
  loadList()
}

/**
 * 打开订单池公开详情。
 *
 * @param {number} id 订单 ID
 */
async function openDetail(id) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const result = await getOrderPoolDetail(id)
    if (result.status === false) {
      throw result
    }
    detail.value = result.data
  } catch (error) {
    showErrorMessage(error)
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

/**
 * 接单；并发失败时展示后端“订单已被接走”提示并刷新列表。
 *
 * @param {Object} row 订单行
 */
async function handleAccept(row) {
  try {
    await ElMessageBox.confirm(`确认接受订单 ${row.orderNo} 的货主报价？`, '接单确认', { type: 'warning' })
  } catch (error) {
    return
  }
  accepting.value = true
  try {
    const result = await acceptOrder(row.id)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('接单成功，可在我的运单中查看')
    detailVisible.value = false
  } catch (error) {
    showErrorMessage(error)
  } finally {
    accepting.value = false
    loadList()
  }
}

onMounted(loadList)
</script>
