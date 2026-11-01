<template>
  <div class="flex min-h-screen bg-slate-100">
    <Sidebar />
    <div class="min-w-0 flex-1">
      <HeaderBar />
      <main class="p-6">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import HeaderBar from './components/HeaderBar.vue'
import Sidebar from './components/Sidebar.vue'
</script>
