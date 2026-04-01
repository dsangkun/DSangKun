<script setup lang="ts">
import type { CompetitorChangeItem } from '../types/workbench'

defineProps<{
  items: CompetitorChangeItem[]
}>()
</script>

<template>
  <div class="monitor-grid">
    <div v-for="item in items" :key="item.id" class="todo-card">
      <div class="todo-title">竞品：{{ item.name }}</div>
      <div class="todo-meta">店铺：{{ item.shop }} ｜ 当前销售排名：{{ item.rank }}</div>
      <div class="change-list">
        <span v-for="change in item.changes" :key="change" class="change-tag">
          {{ change }}
        </span>
      </div>
    </div>
  </div>
</template>
