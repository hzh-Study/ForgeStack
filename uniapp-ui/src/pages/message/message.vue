<template>
  <view class="message">
    <view class="message__header">
      <view class="message__header-left">
        <text class="message__title">消息中心</text>
        <text class="message__summary">
          未读 {{ unread.total }} 条 · 通知 {{ unread.notificationUnread }} · 会话 {{ unread.conversationUnread }}
        </text>
      </view>
      <view class="message__refresh" @click="refreshAll">
        <uni-icons type="refresh" size="22" color="#2b7cff" />
      </view>
    </view>

    <view class="message__tabs">
      <view
        class="message__tab"
        :class="{ 'message__tab--active': activeTab === 'notification' }"
        @click="handleTabChange('notification')"
      >
        <text class="message__tab-text">系统通知</text>
      </view>
      <view
        class="message__tab"
        :class="{ 'message__tab--active': activeTab === 'conversation' }"
        @click="handleTabChange('conversation')"
      >
        <text class="message__tab-text">会话消息</text>
      </view>
    </view>

    <view v-if="activeTab === 'notification'" class="message__list">
      <view
        v-for="item in notifications"
        :key="item.id"
        class="notice-card"
        :class="{ 'notice-card--unread': item.isRead === 0 }"
        @click="handleNotificationTap(item)"
      >
        <view class="notice-card__head">
          <text class="notice-card__title">{{ item.title }}</text>
          <text v-if="item.isRead === 0" class="notice-card__dot">未读</text>
        </view>
        <text class="notice-card__content">{{ item.content }}</text>
        <view class="notice-card__footer">
          <text class="notice-card__time">{{ formatDateTime(item.createTime) }}</text>
          <text v-if="item.isRead === 0" class="notice-card__read" @click.stop="markNotificationRead(item)">
            标为已读
          </text>
        </view>
      </view>

      <BaseEmpty v-if="!notificationLoading && notifications.length === 0" text="暂无系统通知" />
      <uni-load-more v-if="notifications.length > 0" :status="loadMoreStatus" />
    </view>

    <view v-else class="message__list">
      <view
        v-for="item in conversations"
        :key="item.conversationId"
        class="conversation-card"
        @click="openConversation(item)"
      >
        <view class="conversation-card__avatar">
          <text class="conversation-card__avatar-text">{{ firstChar(item.targetName) }}</text>
        </view>
        <view class="conversation-card__body">
          <view class="conversation-card__head">
            <text class="conversation-card__name">{{ item.targetName || '对方账号' }}</text>
            <text class="conversation-card__time">{{ formatDateTime(item.lastMessageTime) }}</text>
          </view>
          <view class="conversation-card__foot">
            <text class="conversation-card__last">{{ item.lastMessageContent || '暂无消息' }}</text>
            <text v-if="Number(item.unreadCount) > 0" class="conversation-card__badge">
              {{ item.unreadCount > 99 ? '99+' : item.unreadCount }}
            </text>
          </view>
        </view>
      </view>

      <BaseEmpty v-if="showConversationEmpty" text="暂无可会话对象，可在任务详情联系所属物流公司" />
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onHide, onPullDownRefresh, onReachBottom, onShow, onUnload } from '@dcloudio/uni-app'
import {
  getUnreadCount,
  listConversations,
  pageNotifications,
  readNotification
} from '@/api/message'
import { useAuthStore } from '@/stores/auth'
import { ensureLogin } from '@/utils/auth'
import { formatDateTime } from '@/utils/format'
import { MESSAGE_POLL_INTERVAL } from '@/constants/driver'
import BaseEmpty from '@/components/BaseEmpty.vue'

const authStore = useAuthStore()

const activeTab = ref('notification')
const unread = ref({
  total: 0,
  notificationUnread: 0,
  conversationUnread: 0
})
const notifications = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const notificationLoading = ref(false)
const conversations = ref([])
const conversationLoading = ref(false)
const conversationLoaded = ref(false)

/**
 * 通知列表加载更多状态。
 */
const loadMoreStatus = computed(() => {
  if (notificationLoading.value) {
    return 'loading'
  }
  return notifications.value.length >= total.value ? 'noMore' : 'more'
})

/**
 * 是否展示会话空状态。
 */
const showConversationEmpty = computed(
  () => !conversationLoading.value && conversationLoaded.value && conversations.value.length === 0
)

/**
 * 轮询定时器。
 */
let pollingTimer = null

/**
 * 取名称首字符用于会话头像占位。
 *
 * @param {string} name 对方名称
 * @returns {string} 首字符
 */
function firstChar(name) {
  return (name || '对').slice(0, 1)
}

/**
 * 刷新未读数。
 */
async function loadUnread() {
  try {
    const result = await getUnreadCount()
    if (result.status === false) {
      return
    }
    unread.value = result.data || { total: 0, notificationUnread: 0, conversationUnread: 0 }
  } catch (error) {
    // 轮询失败不打扰用户
  }
}

/**
 * 加载系统通知。
 *
 * @param {boolean} reset 是否重置分页
 */
async function loadNotifications(reset = false) {
  if (notificationLoading.value) {
    return
  }
  if (reset) {
    pageNum.value = 1
  }
  notificationLoading.value = true
  try {
    const result = await pageNotifications({
      pageNum: pageNum.value,
      pageSize
    })
    if (result.status === false) {
      throw result
    }
    const records = result.data?.records || []
    notifications.value = reset ? records : notifications.value.concat(records)
    total.value = result.data?.total || 0
  } catch (error) {
    uni.showToast({ title: error?.message || '通知加载失败', icon: 'none', duration: 1500 })
  } finally {
    notificationLoading.value = false
    uni.stopPullDownRefresh()
  }
}

/**
 * 加载会话列表。
 */
async function loadConversations() {
  conversationLoading.value = true
  try {
    const result = await listConversations()
    if (result.status === false) {
      throw result
    }
    conversations.value = result.data || []
    conversationLoaded.value = true
  } catch (error) {
    uni.showToast({ title: error?.message || '会话加载失败', icon: 'none', duration: 1500 })
  } finally {
    conversationLoading.value = false
  }
}

/**
 * 刷新未读数、通知与会话。
 */
function refreshAll() {
  loadUnread()
  loadNotifications(true)
  loadConversations()
}

/**
 * 启动 30 秒轮询。
 */
function startPolling() {
  stopPolling()
  pollingTimer = setInterval(() => {
    loadUnread()
    if (activeTab.value === 'notification') {
      loadNotifications(true)
    } else {
      loadConversations()
    }
  }, MESSAGE_POLL_INTERVAL)
}

/**
 * 停止轮询。
 */
function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

/**
 * 页面显示时校验登录并刷新数据。
 */
onShow(() => {
  if (!ensureLogin()) {
    return
  }
  authStore.fetchProfile().catch(() => {})
  refreshAll()
  startPolling()
})

onHide(() => stopPolling())
onUnload(() => stopPolling())

/**
 * 下拉刷新消息数据。
 */
onPullDownRefresh(() => {
  refreshAll()
})

/**
 * 触底加载更多通知。
 */
onReachBottom(() => {
  if (activeTab.value !== 'notification' || notifications.value.length >= total.value || notificationLoading.value) {
    return
  }
  pageNum.value += 1
  loadNotifications(false)
})

/**
 * 切换消息标签。
 *
 * @param {string} tab 标签
 */
function handleTabChange(tab) {
  if (activeTab.value === tab) {
    return
  }
  activeTab.value = tab
  if (tab === 'notification') {
    loadNotifications(true)
  } else {
    loadConversations()
  }
  loadUnread()
}

/**
 * 点击通知查看内容并标记已读。
 *
 * @param {Object} item 通知
 */
async function handleNotificationTap(item) {
  uni.showModal({
    title: item.title,
    content: item.content,
    showCancel: false
  })
  if (item.isRead === 0) {
    await markNotificationRead(item)
  }
}

/**
 * 标记单条通知已读。
 *
 * @param {Object} item 通知
 */
async function markNotificationRead(item) {
  try {
    const result = await readNotification(item.id)
    if (result.status === false) {
      throw result
    }
    uni.showToast({ title: '已标记为已读', icon: 'none', duration: 1000 })
    await loadUnread()
    await loadNotifications(true)
  } catch (error) {
    uni.showToast({ title: error?.message || '标记已读失败', icon: 'none', duration: 1500 })
  }
}

/**
 * 打开会话页。
 *
 * @param {Object} item 会话列表项
 */
function openConversation(item) {
  uni.navigateTo({
    url: `/pages-sub/message/conversation/conversation?id=${item.conversationId}&name=${encodeURIComponent(item.targetName || '')}`
  })
}
</script>

<style lang="scss" scoped>
.message {
  padding: $app-spacing-base;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $app-spacing-sm 0 $app-spacing-base;
  }

  &__title {
    font-size: 38rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__summary {
    display: block;
    margin-top: 6rpx;
    font-size: 24rpx;
    color: $app-text-color-secondary;
  }

  &__refresh {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 64rpx;
    height: 64rpx;
  }

  &__tabs {
    display: flex;
    padding: 8rpx;
    background-color: #ffffff;
    border-radius: $app-border-radius-base;
  }

  &__tab {
    flex: 1;
    padding: 16rpx 0;
    text-align: center;
    border-radius: $app-border-radius-sm;

    &--active {
      background-color: rgba(43, 124, 255, 0.1);
    }
  }

  &__tab-text {
    font-size: 28rpx;
    color: $app-text-color-secondary;
  }

  &__tab--active &__tab-text {
    color: $app-color-primary;
    font-weight: 600;
  }

  &__list {
    margin-top: $app-spacing-base;
  }
}

.notice-card {
  margin-bottom: $app-spacing-base;
  padding: $app-spacing-lg;
  background-color: #ffffff;
  border-radius: $app-border-radius-base;

  &--unread {
    border-left: 6rpx solid $app-color-primary;
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__title {
    font-size: 28rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__dot {
    padding: 2rpx 10rpx;
    font-size: 20rpx;
    color: $app-color-primary;
    background-color: rgba(43, 124, 255, 0.1);
    border-radius: $app-border-radius-sm;
  }

  &__content {
    display: -webkit-box;
    overflow: hidden;
    margin-top: 8rpx;
    font-size: 24rpx;
    line-height: 1.6;
    color: $app-text-color-secondary;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: $app-spacing-base;
  }

  &__time {
    font-size: 22rpx;
    color: $app-text-color-placeholder;
  }

  &__read {
    font-size: 22rpx;
    color: $app-color-primary;
  }
}

.conversation-card {
  display: flex;
  align-items: center;
  gap: $app-spacing-base;
  margin-bottom: $app-spacing-base;
  padding: $app-spacing-lg;
  background-color: #ffffff;
  border-radius: $app-border-radius-base;

  &__avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 80rpx;
    height: 80rpx;
    background-color: rgba(43, 124, 255, 0.1);
    border-radius: 50%;
  }

  &__avatar-text {
    font-size: 30rpx;
    color: $app-color-primary;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__name {
    font-size: 28rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__time {
    font-size: 22rpx;
    color: $app-text-color-placeholder;
  }

  &__foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 6rpx;
  }

  &__last {
    flex: 1;
    overflow: hidden;
    font-size: 24rpx;
    color: $app-text-color-secondary;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  &__badge {
    min-width: 32rpx;
    height: 32rpx;
    padding: 0 8rpx;
    font-size: 20rpx;
    line-height: 32rpx;
    color: #ffffff;
    text-align: center;
    background-color: $app-color-error;
    border-radius: 999rpx;
  }
}
</style>
