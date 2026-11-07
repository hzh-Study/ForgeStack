<template>
  <section class="grid gap-5">
    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="m-0 text-xl font-semibold text-slate-950">消息中心</h2>
          <p class="mt-2 text-sm text-slate-500">系统通知逐条标记已读，打开会话即把该会话消息标记已读。</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建会话</el-button>
      </div>

      <el-tabs v-model="activeTab" class="mt-4">
        <el-tab-pane label="站内消息" name="conversation">
          <div class="flex min-h-[520px] gap-4">
            <div class="w-[280px] shrink-0 rounded-md border border-slate-200">
              <div class="border-b border-slate-200 px-4 py-3 text-sm font-medium text-slate-700">
                会话列表
              </div>
              <div v-if="!conversations.length" class="p-6 text-center text-sm text-slate-400">
                暂无会话，可从业务详情页联系相关方
              </div>
              <button
                v-for="conversation in conversations"
                :key="conversation.conversationId"
                class="block w-full border-b border-slate-100 px-4 py-3 text-left hover:bg-slate-50"
                :class="selectedConversationId === conversation.conversationId ? 'bg-blue-50' : ''"
                @click="openConversation(conversation.conversationId)"
              >
                <div class="flex items-center justify-between gap-2">
                  <span class="truncate text-sm font-medium text-slate-800">{{ conversation.targetName || `账号 ${conversation.targetAccountId}` }}</span>
                  <el-badge v-if="conversation.unreadCount" :value="conversation.unreadCount" :max="99" />
                </div>
                <p class="mt-1 line-clamp-1 text-xs text-slate-500">{{ conversation.lastMessageContent || '暂无消息' }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ formatDateTime(conversation.lastMessageTime) }}</p>
              </button>
            </div>

            <div class="flex min-w-0 flex-1 flex-col rounded-md border border-slate-200">
              <div class="flex items-center justify-between border-b border-slate-200 px-4 py-3">
                <span class="text-sm font-medium text-slate-800">
                  {{ currentConversation?.targetName || '请选择会话' }}
                </span>
                <span v-if="currentConversation?.relatedOrderId" class="text-xs text-slate-400">
                  关联订单 ID：{{ currentConversation.relatedOrderId }}
                </span>
              </div>

              <div v-if="!selectedConversationId" class="flex flex-1 items-center justify-center text-sm text-slate-400">
                选择左侧会话查看消息
              </div>
              <template v-else>
                <div ref="messageListRef" class="flex-1 space-y-3 overflow-y-auto p-4">
                  <div v-if="!messages.length" class="py-10 text-center text-sm text-slate-400">暂无消息</div>
                  <div
                    v-for="message in messages"
                    :key="message.id"
                    class="flex"
                    :class="message.senderAccountId === authStore.accountInfo?.accountId ? 'justify-end' : 'justify-start'"
                  >
                    <div class="max-w-[70%] rounded-md px-3 py-2 text-sm"
                      :class="message.senderAccountId === authStore.accountInfo?.accountId
                        ? 'bg-blue-600 text-white'
                        : 'bg-slate-100 text-slate-700'">
                      <p class="m-0 whitespace-pre-wrap break-words">{{ message.content }}</p>
                      <p class="mt-1 text-xs"
                        :class="message.senderAccountId === authStore.accountInfo?.accountId ? 'text-blue-100' : 'text-slate-400'">
                        {{ formatDateTime(message.createTime) }}
                      </p>
                    </div>
                  </div>
                </div>
                <div class="border-t border-slate-200 p-3">
                  <el-input
                    v-model="messageContent"
                    type="textarea"
                    :rows="2"
                    maxlength="1000"
                    show-word-limit
                    placeholder="请输入文本消息，回车发送"
                    @keyup.ctrl.enter="handleSend"
                  />
                  <div class="mt-2 flex justify-end">
                    <el-button type="primary" :loading="sending" :disabled="!messageContent.trim()" @click="handleSend">
                      发送
                    </el-button>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="系统通知" name="notification">
          <div v-if="!notifications.length" class="py-12 text-center text-sm text-slate-400">暂无系统通知</div>
          <div v-for="notification in notifications" :key="notification.id"
            class="cursor-pointer border-b border-slate-100 px-4 py-4 hover:bg-slate-50"
            @click="handleNotificationClick(notification)">
            <div class="flex items-center justify-between gap-3">
              <div class="flex items-center gap-2">
                <span v-if="notification.isRead === 0" class="h-2 w-2 rounded-full bg-blue-500"></span>
                <span class="text-sm font-medium text-slate-800">{{ notification.title }}</span>
                <el-tag size="small" type="info">{{ notification.type }}</el-tag>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-xs text-slate-400">{{ formatDateTime(notification.createTime) }}</span>
                <el-button v-if="notification.isRead === 0" link type="primary" @click.stop="handleReadNotification(notification)">
                  标记已读
                </el-button>
              </div>
            </div>
            <p class="mt-2 whitespace-pre-wrap text-sm text-slate-600">{{ notification.content }}</p>
          </div>
          <div class="mt-4 flex justify-end">
            <el-pagination
              v-model:current-page="notificationQuery.pageNum"
              :page-size="notificationQuery.pageSize"
              :total="notificationTotal"
              layout="prev, pager, next, total"
              @current-change="loadNotifications"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="createVisible" title="新建会话" width="460px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px">
        <el-form-item label="对方账号 ID" prop="targetAccountId">
          <el-input-number v-model="createForm.targetAccountId" :min="1" :controls="false" class="w-full" placeholder="请输入对方账号 ID" />
        </el-form-item>
        <el-form-item label="关联订单 ID" prop="orderId">
          <el-input-number v-model="createForm.orderId" :min="1" :controls="false" class="w-full" placeholder="可选，用于关联业务订单" />
        </el-form-item>
      </el-form>
      <p class="text-xs text-slate-500">
        会话关系受后端校验：司机只能联系所属物流公司，货主只能联系接单物流公司。
        可从运单详情或任务详情点击“联系”按钮自动打开会话。
      </p>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateConversation">创建并打开</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import {
  createConversation,
  listConversations,
  pageConversationMessages,
  pageNotifications,
  readNotification,
  sendConversationMessage
} from '@/api/messages'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const activeTab = ref('conversation')
const conversations = ref([])
const selectedConversationId = ref(null)
const messages = ref([])
const messageContent = ref('')
const sending = ref(false)
const messageListRef = ref()
const notifications = ref([])
const notificationTotal = ref(0)
const notificationQuery = reactive({ pageNum: 1, pageSize: 10 })
const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref()
const createForm = reactive({ targetAccountId: null, orderId: null })
const createRules = {
  targetAccountId: [{ required: true, message: '请输入对方账号 ID', trigger: 'blur' }]
}

/**
 * 当前选中的会话。
 */
const currentConversation = computed(() =>
  conversations.value.find((item) => item.conversationId === selectedConversationId.value)
)

/**
 * 通知顶部铃铛重新拉取未读数。
 */
function notifyUnreadChanged() {
  window.dispatchEvent(new Event('message-unread-refresh'))
}

/**
 * 加载当前账号参与的会话列表。
 */
async function loadConversations() {
  try {
    const result = await listConversations()
    if (result.status === false) {
      throw result
    }
    conversations.value = result.data || []
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 打开会话，后端会把该会话收到的消息标记已读。
 *
 * @param {number} conversationId 会话 ID
 */
async function openConversation(conversationId) {
  selectedConversationId.value = conversationId
  try {
    const result = await pageConversationMessages(conversationId, { pageNum: 1, pageSize: 100 })
    if (result.status === false) {
      throw result
    }
    messages.value = result.data?.records || []
    await loadConversations()
    notifyUnreadChanged()
    await nextTick()
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 发送当前会话文本消息并刷新消息列表。
 */
async function handleSend() {
  const content = messageContent.value.trim()
  if (!content || !selectedConversationId.value) {
    return
  }
  sending.value = true
  try {
    const result = await sendConversationMessage(selectedConversationId.value, content)
    if (result.status === false) {
      throw result
    }
    messageContent.value = ''
    await openConversation(selectedConversationId.value)
  } catch (error) {
    showErrorMessage(error)
  } finally {
    sending.value = false
  }
}

/**
 * 加载系统通知分页数据。
 */
async function loadNotifications() {
  try {
    const result = await pageNotifications(notificationQuery)
    if (result.status === false) {
      throw result
    }
    notifications.value = result.data?.records || []
    notificationTotal.value = Number(result.data?.total || 0)
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 标记单条通知已读并刷新列表与铃铛。
 *
 * @param {Object} notification 通知
 */
async function handleReadNotification(notification) {
  if (notification.isRead !== 0) {
    return
  }
  try {
    const result = await readNotification(notification.id)
    if (result.status === false) {
      throw result
    }
    notification.isRead = 1
    notifyUnreadChanged()
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 点击通知时先标记已读，保持列表当前位置。
 *
 * @param {Object} notification 通知
 */
function handleNotificationClick(notification) {
  handleReadNotification(notification)
}

/**
 * 打开新建会话弹窗并清空上次输入。
 */
function openCreateDialog() {
  createForm.targetAccountId = null
  createForm.orderId = null
  createVisible.value = true
}

/**
 * 创建或复用会话，成功后直接打开该会话。
 */
async function handleCreateConversation() {
  await createFormRef.value.validate()
  creating.value = true
  try {
    const result = await createConversation({
      targetAccountId: createForm.targetAccountId,
      orderId: createForm.orderId || null
    })
    if (result.status === false) {
      throw result
    }
    createVisible.value = false
    showSuccessMessage('会话已创建')
    await loadConversations()
    await openConversation(result.data)
    activeTab.value = 'conversation'
  } catch (error) {
    showErrorMessage(error)
  } finally {
    creating.value = false
  }
}

/**
 * 从业务详情页带 targetAccountId 进入时，自动创建或复用并打开会话。
 */
async function openConversationFromQuery() {
  const targetAccountId = Number(route.query.targetAccountId)
  if (!targetAccountId) {
    return
  }
  try {
    const result = await createConversation({
      targetAccountId,
      orderId: Number(route.query.orderId) || null
    })
    if (result.status === false) {
      throw result
    }
    await loadConversations()
    await openConversation(result.data)
  } catch (error) {
    showErrorMessage(error)
  } finally {
    const query = { ...route.query }
    delete query.targetAccountId
    delete query.orderId
    router.replace({ path: '/messages', query })
  }
}

watch(activeTab, (tab) => {
  if (tab === 'notification') {
    loadNotifications()
  }
})

onMounted(async () => {
  await loadConversations()
  if (conversations.value.length) {
    await openConversation(conversations.value[0].conversationId)
  }
  await openConversationFromQuery()
})
</script>
