<template>
  <div class="flex min-h-screen flex-col bg-slate-50">
    <HeaderNav />
    <div class="flex flex-1">
      <UserSideMenu v-if="authStore.hasAccountInfo" />
      <main class="min-w-0 flex-1 px-6 py-6">
        <router-view />
      </main>
    </div>
    <FooterBar />
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import FooterBar from './components/FooterBar.vue'
import HeaderNav from './components/HeaderNav.vue'
import UserSideMenu from './components/UserSideMenu.vue'

const authStore = useAuthStore()
</script>
