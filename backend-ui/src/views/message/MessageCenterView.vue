<template>
  <section class="grid gap-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="m-0 text-[22px] font-bold text-slate-950">消息中心</h1>
        <p class="mt-2 text-slate-500">查看系统通知与站内会话，打开会话即自动已读，未读数每 30 秒刷新。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openNewConversation">发起会话</el-button>
    </div>

    <el-card shadow="never" class="[&_.el-card__body]:pt-2">
      <el-tabs v-model="activeTab">
        <el-tab-pane name="notifications">
          <template #label>
            <span>系统通知</span>
            <span v-if="unread.notificationUnread > 0" class="ml-1 text-xs text-red-500">
              ({{ unread.notificationUnread }})
            </span>
          </template>
          <div v-loading="notificationLoading" class="grid gap-3">
            <div v-for="item in notifications" :key="item.id"
              class="cursor-pointer rounded-lg border p-3 transition-colors"
              :class="item.isRead === 0 ? 'border-blue-200 bg-blue-50/50' : 'border-slate-200 hover:bg-slate-50'"
              @click="handleNotificationClick(item)">
              <div class="flex flex-wrap items-center gap-2">
                <el-tag size="small" type="info">{{ getMappedLabel(NOTICE_TYPE_NAME_MAP, item.type) }}</el-tag>
                <span class="text-sm font-semibold text-slate-800">{{ item.title || '系统通知' }}</span>
                <span class="ml-auto text-xs text-slate-400">{{ formatDateTime(item.createTime) }}</span>
              </div>
              <div class="mt-2 whitespace-pre-wrap text-sm text-slate-600">{{ item.content }}</div>
              <div v-if="item.isRead === 0" class="mt-2 text-xs text-blue-600">未读，点击标记为已读</div>
            </div>
            <el-empty v-if="!notifications.length" description="暂无系统通知" />
          </div>
          <BasePagination v-model:page-num="notificationQuery.pageNum"
            v-model:page-size="notificationQuery.pageSize" :total="notificationTotal"
            @change="loadNotifications()" />
        </el-tab-pane>

        <el-tab-pane name="conversations">
          <template #label>
            <span>站内会话</span>
            <span v-if="unread.conversationUnread > 0" class="ml-1 text-xs text-red-500">
              ({{ unread.conversationUnread }})
            </span>
          </template>
          <div class="grid gap-4 lg:grid-cols-[300px_1fr]">
            <div v-loading="conversationLoading" class="rounded-lg border border-slate-200">
              <div class="border-b border-slate-100 px-3 py-2 text-sm font-semibold text-slate-700">会话列表</div>
              <div class="max-h-[560px] overflow-y-auto">
                <div v-for="item in conversations" :key="item.conversationId"
                  class="cursor-pointer border-b border-slate-100 px-3 py-2 transition-colors hover:bg-slate-50"
                  :class="item.conversationId === selectedConversationId ? 'bg-blue-50' : ''"
                  @click="openConversation(item.conversationId)">
                  <div class="flex items-center gap-2">
                    <span class="min-w-0 flex-1 truncate text-sm font-medium text-slate-800">
                      {{ item.targetName || '未知账号' }}
                    </span>
                    <el-tag :type="getTagType(ROLE_TAG_MAP, item.targetRole)" size="small">
                      {{ getMappedLabel(ROLE_NAME_MAP, item.targetRole) }}
                    </el-tag>
                    <el-badge v-if="item.unreadCount > 0" :value="item.unreadCount" :max="99" />
                  </div>
                  <div class="mt-1 truncate text-xs text-slate-500">
                    {{ item.lastMessageContent || '暂无消息' }}
                  </div>
                  <div class="mt-1 text-[11px] text-slate-400">
                    {{ item.lastMessageTime ? formatDateTime(item.lastMessageTime) : '' }}
                  </div>
                </div>
                <el-empty v-if="!conversations.length" description="暂无会话" :image-size="60" />
              </div>
            </div>

            <div class="flex min-h-[560px] flex-col rounded-lg border border-slate-200">
              <template v-if="selectedConversation">
                <div class="flex flex-wrap items-center gap-2 border-b border-slate-100 px-3 py-2">
                  <span class="text-sm font-semibold text-slate-800">
                    {{ selectedConversation.targetName || '未知账号' }}
                  </span>
                  <el-tag :type="getTagType(ROLE_TAG_MAP, selectedConversation.targetRole)" size="small">
                    {{ getMappedLabel(ROLE_NAME_MAP, selectedConversation.targetRole) }}
                  </el-tag>
                  <span v-if="selectedConversation.relatedOrderId" class="text-xs text-slate-500">
                    关联订单 #{{ selectedConversation.relatedOrderId }}
                  </span>
                </div>
                <div ref="messagePaneRef" v-loading="messageLoading"
                  class="flex-1 overflow-y-auto bg-slate-50 p-3">
                  <div v-for="message in conversationMessages" :key="message.id" class="mb-2 flex"
                    :class="isMine(message) ? 'justify-end' : 'justify-start'">
                    <div class="max-w-[72%] rounded-lg px-3 py-2 text-sm"
                      :class="isMine(message) ? 'bg-blue-600 text-white' : 'bg-white text-slate-700 shadow-sm'">
                      <div class="whitespace-pre-wrap break-words">{{ message.content }}</div>
                      <div class="mt-1 text-[11px]"
                        :class="isMine(message) ? 'text-blue-100' : 'text-slate-400'">
                        {{ formatDateTime(message.createTime) }}
                      </div>
                    </div>
                  </div>
                  <el-empty v-if="!conversationMessages.length" description="暂无消息，发送第一条消息吧"
                    :image-size="60" />
                </div>
                <div class="flex items-center justify-center border-t border-slate-100 py-1">
                  <el-pagination v-model:current-page="conversationPageNum" :page-size="conversationPageSize"
                    :total="conversationTotal" layout="prev, pager, next" small :teleported="false"
                    @current-change="handleConversationPageChange" />
                </div>
                <div class="border-t border-slate-100 p-3">
                  <div class="flex items-end gap-2">
                    <el-input v-model="sendContent" type="textarea" :rows="2" maxlength="1000"
                      placeholder="输入文本消息，点击发送" />
                    <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
                  </div>
                </div>
              </template>
              <el-empty v-else description="请选择左侧会话查看消息" />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="newConversationVisible" title="发起新会话" width="500px">
      <el-form label-width="90px">
        <el-form-item label="目标账号" required>
          <el-select v-model="targetAccountId" filterable remote clearable :remote-method="searchTargets"
            :loading="targetSearching" placeholder="输入手机号或名称搜索账号" style="width: 100%">
            <el-option v-for="item in targetOptions" :key="item.accountId"
              :label="`${item.displayName}（${item.roleName}） ${item.username}`" :value="item.accountId" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联订单">
          <el-input v-model="newOrderId" placeholder="可选，填写关联订单 ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newConversationVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateConversation">创建会话</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { pageAccounts } from '@/api/account'
import {
  createConversation,
  getConversationMessages,
  getUnreadCount,
  listConversations,
  pageNotifications,
  readNotification,
  sendConversationMessage
} from '@/api/message'
import BasePagination from '@/components/BasePagination.vue'
import {
  getMappedLabel,
  getTagType,
  NOTICE_TYPE_NAME_MAP,
  ROLE_NAME_MAP,
  ROLE_TAG_MAP
} from '@/constants/logistics'
import { defaultNotificationQuery } from '@/models/message'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const authStore = useAuthStore()
const activeTab = ref('notifications')
const unread = reactive({ total: 0, notificationUnread: 0, conversationUnread: 0 })

const notificationQuery = reactive({ ...defaultNotificationQuery })
const notificationTotal = ref(0)
const notifications = ref([])
const notificationLoading = ref(false)

const conversations = ref([])
const conversationLoading = ref(false)
const selectedConversationId = ref(null)
const conversationMessages = ref([])
const conversationTotal = ref(0)
const conversationPageNum = ref(1)
const conversationPageSize = ref(20)
const messageLoading = ref(false)
const messagePaneRef = ref()
const sendContent = ref('')
const sending = ref(false)

const newConversationVisible = ref(false)
const targetOptions = ref([])
const targetSearching = ref(false)
const targetAccountId = ref(null)
const newOrderId = ref('')
const creating = ref(false)

let pollingTimer = null

/**
 * 当前选中的会话信息。
 */
const selectedConversation = computed(() => conversations.value
  .find((item) => item.conversationId === selectedConversationId.value) || null)

/**
 * 拉取未读汇总，轮询失败时保持上一次展示避免页面闪烁。
 *
 * @param {boolean} silent 是否静默处理异常
 */
async function loadUnreadCount(silent = false) {
  try {
    const result = await getUnreadCount()
    if (result.status === false) {
      throw result
    }
    Object.assign(unread, {
      total: result.data?.total || 0,
      notificationUnread: result.data?.notificationUnread || 0,
      conversationUnread: result.data?.conversationUnread || 0
    })
  } catch (error) {
    if (!silent) {
      showErrorMessage(error)
    }
  }
}

/**
 * 加载系统通知分页数据。
 *
 * @param {boolean} silent 是否静默处理异常
 */
async function loadNotifications(silent = false) {
  notificationLoading.value = true
  try {
    const result = await pageNotifications({
      pageNum: notificationQuery.pageNum,
      pageSize: notificationQuery.pageSize
    })
    if (result.status === false) {
      throw result
    }
    notifications.value = result.data.records || []
    notificationTotal.value = result.data.total || 0
  } catch (error) {
    if (!silent) {
      showErrorMessage(error)
    }
  } finally {
    notificationLoading.value = false
  }
}

/**
 * 加载当前账号的会话列表。
 *
 * @param {boolean} silent 是否静默处理异常
 */
async function loadConversations(silent = false) {
  conversationLoading.value = true
  try {
    const result = await listConversations()
    if (result.status === false) {
      throw result
    }
    conversations.value = result.data || []
  } catch (error) {
    if (!silent) {
      showErrorMessage(error)
    }
  } finally {
    conversationLoading.value = false
  }
}

/**
 * 点击未读通知时逐条标记已读，并刷新未读汇总。
 *
 * @param {Object} item 通知列表项
 */
async function handleNotificationClick(item) {
  if (item.isRead === 1) {
    return
  }
  try {
    const result = await readNotification(item.id)
    if (result.status === false) {
      throw result
    }
    item.isRead = 1
    await loadUnreadCount(true)
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 加载会话消息；默认定位到最后一页并自动滚动到底部。
 *
 * @param {number} conversationId 会话 ID
 * @param {boolean} stayPage 是否保持当前页码
 * @param {boolean} silent 是否静默处理异常
 */
async function loadConversationMessages(conversationId, stayPage = false, silent = false) {
  messageLoading.value = true
  try {
    if (stayPage) {
      const result = await getConversationMessages(conversationId, {
        pageNum: conversationPageNum.value,
        pageSize: conversationPageSize.value
      })
      if (result.status === false) {
        throw result
      }
      conversationMessages.value = result.data.records || []
      conversationTotal.value = result.data.total || 0
    } else {
      const firstResult = await getConversationMessages(conversationId, {
        pageNum: 1,
        pageSize: conversationPageSize.value
      })
      if (firstResult.status === false) {
        throw firstResult
      }
      const total = firstResult.data.total || 0
      const lastPage = Math.max(1, Math.ceil(total / conversationPageSize.value))
      if (lastPage > 1) {
        const lastResult = await getConversationMessages(conversationId, {
          pageNum: lastPage,
          pageSize: conversationPageSize.value
        })
        if (lastResult.status === false) {
          throw lastResult
        }
        conversationMessages.value = lastResult.data.records || []
        conversationTotal.value = lastResult.data.total || 0
        conversationPageNum.value = lastPage
      } else {
        conversationMessages.value = firstResult.data.records || []
        conversationTotal.value = total
        conversationPageNum.value = 1
      }
    }
    await nextTick()
    scrollMessagePaneToBottom()
  } catch (error) {
    if (!silent) {
      showErrorMessage(error)
    }
  } finally {
    messageLoading.value = false
  }
}

/**
 * 打开会话，自动标记已读并刷新会话列表与未读汇总。
 *
 * @param {number} conversationId 会话 ID
 */
async function openConversation(conversationId) {
  selectedConversationId.value = conversationId
  await loadConversationMessages(conversationId)
  await loadConversations(true)
  await loadUnreadCount(true)
}

/**
 * 切换会话消息分页，并按当前页码重新加载。
 */
async function handleConversationPageChange() {
  if (!selectedConversationId.value) {
    return
  }
  await loadConversationMessages(selectedConversationId.value, true, true)
}

/**
 * 判断消息是否由当前管理员发送，用于左右气泡区分。
 *
 * @param {Object} message 会话消息
 * @returns {boolean} true 表示本人发送
 */
function isMine(message) {
  return authStore.accountInfo?.accountId === message.senderAccountId
}

/**
 * 把消息滚动区域定位到底部，便于查看最新会话内容。
 */
function scrollMessagePaneToBottom() {
  const pane = messagePaneRef.value
  if (pane) {
    pane.scrollTop = pane.scrollHeight
  }
}

/**
 * 发送文本消息，成功后跳转到最新一页并刷新会话列表。
 */
async function handleSend() {
  const content = sendContent.value.trim()
  if (!content || !selectedConversationId.value) {
    return
  }
  sending.value = true
  try {
    const result = await sendConversationMessage(selectedConversationId.value, { content })
    if (result.status === false) {
      throw result
    }
    sendContent.value = ''
    showSuccessMessage('消息已发送')
    await loadConversationMessages(selectedConversationId.value, false, true)
    await loadConversations(true)
  } catch (error) {
    showErrorMessage(error)
  } finally {
    sending.value = false
  }
}

/**
 * 打开发起会话弹窗，并预加载可选的启用账号。
 */
function openNewConversation() {
  targetAccountId.value = null
  newOrderId.value = ''
  newConversationVisible.value = true
  searchTargets('')
}

/**
 * 远程搜索可发起会话的启用账号。
 *
 * @param {string} keyword 手机号或展示名称关键词
 */
async function searchTargets(keyword) {
  targetSearching.value = true
  try {
    const result = await pageAccounts({
      keyword: keyword?.trim() || null,
      status: 'ENABLED',
      pageNum: 1,
      pageSize: 20
    })
    if (result.status === false) {
      throw result
    }
    targetOptions.value = (result.data.records || []).filter((item) => item.role !== 'ADMIN')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    targetSearching.value = false
  }
}

/**
 * 创建或复用与目标账号的会话，并立即打开该会话。
 */
async function handleCreateConversation() {
  if (!targetAccountId.value) {
    showErrorMessage('请选择目标账号')
    return
  }
  creating.value = true
  try {
    const orderId = newOrderId.value ? Number(newOrderId.value) : null
    if (newOrderId.value && Number.isNaN(orderId)) {
      showErrorMessage('关联订单 ID 必须为数字')
      return
    }
    const result = await createConversation({
      targetAccountId: targetAccountId.value,
      orderId
    })
    if (result.status === false) {
      throw result
    }
    const conversationId = result.data
    newConversationVisible.value = false
    activeTab.value = 'conversations'
    showSuccessMessage('会话已创建')
    await loadConversations(true)
    await openConversation(conversationId)
  } catch (error) {
    showErrorMessage(error)
  } finally {
    creating.value = false
  }
}

/**
 * 30 秒轮询未读数与当前可见消息列表，不引入 WebSocket。
 */
async function refreshByPolling() {
  if (activeTab.value === 'notifications') {
    await loadNotifications(true)
    await loadUnreadCount(true)
    return
  }
  await loadConversations(true)
  if (selectedConversationId.value) {
    await loadConversationMessages(selectedConversationId.value, true, true)
  }
  await loadUnreadCount(true)
}

onMounted(async () => {
  await loadUnreadCount(true)
  await loadNotifications()
  await loadConversations()
  pollingTimer = window.setInterval(refreshByPolling, 30000)
})

onUnmounted(() => {
  if (pollingTimer) {
    window.clearInterval(pollingTimer)
    pollingTimer = null
  }
})
</script>
