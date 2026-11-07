<template>
  <view class="conversation">
    <view class="conversation__header">
      <text class="conversation__name">{{ targetName || '会话' }}</text>
      <text class="conversation__tip">仅支持文本消息，打开会话即自动标记已读</text>
    </view>

    <scroll-view
      class="conversation__messages"
      scroll-y
      :scroll-into-view="scrollAnchor"
      scroll-with-animation
    >
      <view v-if="messages.length < total" class="conversation__more" @click="loadMoreHistory">
        加载更早的消息
      </view>

      <view
        v-for="item in messages"
        :id="`msg-${item.id}`"
        :key="item.id"
        class="bubble-row"
        :class="{ 'bubble-row--mine': isMine(item) }"
      >
        <view class="bubble" :class="{ 'bubble--mine': isMine(item) }">
          <text class="bubble__content">{{ item.content }}</text>
          <text class="bubble__time">{{ formatDateTime(item.createTime) }}</text>
        </view>
      </view>

      <view v-if="messages.length === 0 && loaded" class="conversation__empty">
        <text class="conversation__empty-text">还没有消息，发送第一条消息开始沟通</text>
      </view>
    </scroll-view>

    <view class="conversation__input-bar app-safe-area-bottom">
      <input
        v-model="content"
        class="conversation__input"
        maxlength="1000"
        confirm-type="send"
        placeholder="输入消息内容"
        @confirm="handleSend"
      />
      <button class="conversation__send" type="primary" size="mini" :loading="sending" @click="handleSend">
        发送
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { getConversationMessages, sendConversationMessage } from '@/api/message'
import { useAuthStore } from '@/stores/auth'
import { ensureLogin } from '@/utils/auth'
import { formatDateTime } from '@/utils/format'
import { MESSAGE_POLL_INTERVAL } from '@/constants/driver'

const authStore = useAuthStore()

const conversationId = ref(null)
const targetName = ref('')
const messages = ref([])
const pageNum = ref(1)
const pageSize = 100
const total = ref(0)
const loaded = ref(false)
const content = ref('')
const sending = ref(false)
const scrollAnchor = ref('')

/**
 * 消息轮询定时器。
 */
let pollingTimer = null

/**
 * 判断消息是否为当前账号发送。
 *
 * @param {Object} item 消息
 * @returns {boolean} 是否本人发送
 */
function isMine(item) {
  return item.senderAccountId === authStore.accountInfo?.accountId
}

/**
 * 加载会话消息，打开会话会自动标记已读。
 *
 * @param {boolean} reset 是否重置到第一页
 */
async function loadMessages(reset = true) {
  if (reset) {
    pageNum.value = 1
  }
  try {
    const result = await getConversationMessages(conversationId.value, {
      pageNum: pageNum.value,
      pageSize
    })
    if (result.status === false) {
      throw result
    }
    const records = result.data?.records || []
    messages.value = reset ? records : records.concat(messages.value)
    total.value = result.data?.total || 0
    loaded.value = true
    scrollToBottom()
  } catch (error) {
    uni.showToast({ title: error?.message || '会话消息加载失败', icon: 'none', duration: 1500 })
  }
}

/**
 * 滚动到最新消息。
 */
function scrollToBottom() {
  const last = messages.value[messages.value.length - 1]
  if (last) {
    scrollAnchor.value = `msg-${last.id}`
  }
}

/**
 * 加载更早的消息。
 */
function loadMoreHistory() {
  pageNum.value += 1
  loadMessages(false)
}

/**
 * 页面加载时读取会话参数并拉取消息。
 *
 * @param {Object} options 页面参数 { id, name }
 */
onLoad((options) => {
  if (!ensureLogin()) {
    return
  }
  conversationId.value = Number(options?.id || 0)
  targetName.value = options?.name ? decodeURIComponent(options.name) : ''
  loadMessages(true)
  pollingTimer = setInterval(() => loadMessages(true), MESSAGE_POLL_INTERVAL)
})

/**
 * 发送文本消息。
 */
async function handleSend() {
  if (sending.value) {
    return
  }
  const text = content.value.trim()
  if (!text) {
    uni.showToast({ title: '请输入消息内容', icon: 'none', duration: 1000 })
    return
  }
  sending.value = true
  try {
    const result = await sendConversationMessage(conversationId.value, text)
    if (result.status === false) {
      throw result
    }
    content.value = ''
    await loadMessages(true)
  } catch (error) {
    uni.showToast({ title: error?.message || '消息发送失败', icon: 'none', duration: 1500 })
  } finally {
    sending.value = false
  }
}

onUnload(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
})
</script>

<style lang="scss" scoped>
.conversation {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: $app-bg-color;

  &__header {
    padding: $app-spacing-base $app-spacing-lg;
    background-color: #ffffff;
    border-bottom: 1rpx solid $app-border-color;
  }

  &__name {
    font-size: 30rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__tip {
    display: block;
    margin-top: 4rpx;
    font-size: 22rpx;
    color: $app-text-color-placeholder;
  }

  &__messages {
    flex: 1;
    padding: $app-spacing-base $app-spacing-base 140rpx;
    box-sizing: border-box;
  }

  &__more {
    padding: $app-spacing-sm 0;
    font-size: 22rpx;
    color: $app-color-primary;
    text-align: center;
  }

  &__empty {
    padding: 120rpx 0;
  }

  &__empty-text {
    display: block;
    font-size: 26rpx;
    color: $app-text-color-placeholder;
    text-align: center;
  }

  &__input-bar {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    display: flex;
    align-items: center;
    gap: $app-spacing-sm;
    padding: $app-spacing-sm $app-spacing-lg;
    background-color: #ffffff;
    border-top: 1rpx solid $app-border-color;
  }

  &__input {
    flex: 1;
    height: 72rpx;
    padding: 0 $app-spacing-base;
    font-size: 26rpx;
    background-color: $app-bg-color;
    border-radius: $app-border-radius-sm;
  }

  &__send {
    margin: 0;
    font-size: 26rpx;
  }
}

.bubble-row {
  display: flex;
  justify-content: flex-start;
  margin-bottom: $app-spacing-base;

  &--mine {
    justify-content: flex-end;
  }
}

.bubble {
  max-width: 70%;
  padding: $app-spacing-sm $app-spacing-base;
  background-color: #ffffff;
  border-radius: $app-border-radius-base;

  &--mine {
    background-color: rgba(43, 124, 255, 0.12);
  }

  &__content {
    display: block;
    font-size: 26rpx;
    line-height: 1.5;
    color: $app-text-color;
    word-break: break-all;
  }

  &__time {
    display: block;
    margin-top: 6rpx;
    font-size: 20rpx;
    color: $app-text-color-placeholder;
  }
}
</style>
