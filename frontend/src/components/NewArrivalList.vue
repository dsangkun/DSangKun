<script setup lang="ts">
import type { NewArrivalItem } from '../types/workbench'

const props = defineProps<{
  items: NewArrivalItem[]
}>()

const emit = defineEmits<{
  handle: [id: string, action: 'push' | 'track' | 'ignore']
}>()
</script>

<template>
  <div v-if="props.items.length" class="uniform-card-grid slim-uniform-grid">
    <article v-for="item in props.items" :key="item.id" class="unified-info-card refined-info-card">
      <div class="uniform-card-top">
        <div class="uniform-card-badge uniform-badge-new">新品</div>
        <div class="uniform-card-id">{{ item.time }}</div>
      </div>

      <div class="uniform-mini-header">
        <div class="uniform-mini-dot uniform-mini-dot-blue"></div>
        <div class="uniform-mini-label">竞品上新</div>
      </div>

      <div class="uniform-card-title clamp-two-lines">{{ item.title }}</div>
      <div class="uniform-card-subtitle clamp-one-line">{{ item.shop }} · {{ item.category }}</div>

      <div class="uniform-chip-row compact-chip-row">
        <span class="uniform-chip">编号 {{ item.id }}</span>
      </div>

      <div class="uniform-summary-text compact-summary-text">
        检测到新上架商品，建议快速判断是否需要推送或进入追踪。
      </div>

      <div class="uniform-action-row compact-action-row">
        <button class="uniform-action-btn primary" @click="emit('handle', item.id, 'push')">推送</button>
        <button class="uniform-action-btn warning" @click="emit('handle', item.id, 'track')">追踪</button>
        <button class="uniform-action-btn muted" @click="emit('handle', item.id, 'ignore')">忽略</button>
      </div>
    </article>
  </div>
  <div v-else class="empty-tip">当前没有待处理的竞品上新信息。</div>
</template>
