<template>
  <div class="min-h-screen bg-slate-50">
    <HeaderNav />
    <main class="mx-auto min-h-[calc(100vh-144px)] w-[1120px] py-8">
      <router-view />
    </main>
    <FooterBar />
  </div>
</template>

<script setup>
import FooterBar from './components/FooterBar.vue'
import HeaderNav from './components/HeaderNav.vue'
</script>
