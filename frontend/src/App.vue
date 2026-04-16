<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  { name: '俄罗斯WB运营', path: '/wb' },
  { name: '项目管理进度', path: '/project-progress' },
  { name: 'Amazon运营推进器', path: '/' }
]

const currentSectionName = computed(() => {
  return navItems.find((item) => item.path === route.path)?.name ?? 'Amazon运营推进器'
})
</script>

<template>
  <div class="app-shell">
    <aside class="shell-sidebar">
      <div class="brand-block">
        <div class="brand-mark">A</div>
        <div>
          <div class="brand-title">Amazon运营推进器</div>
          <div class="brand-subtitle">前端改版执行基线</div>
        </div>
      </div>

      <nav class="side-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="side-nav-item"
          :class="{ active: route.path === item.path }"
        >
          {{ item.name }}
        </RouterLink>
      </nav>
    </aside>

    <main class="shell-main">
      <header class="shell-topbar">
        <div>
          <div class="shell-topbar-label">当前系统</div>
          <h1 class="shell-topbar-title">{{ currentSectionName }}</h1>
        </div>
      </header>

      <div class="shell-content">
        <RouterView />
      </div>
    </main>
  </div>
</template>
