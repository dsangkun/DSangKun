<script setup lang="ts">
import type { ProductMetricBlock } from '../types/workbench'

defineProps<{
  productName: string
  metric: ProductMetricBlock
}>()
</script>

<template>
  <div class="chart-card">
    <div class="chart-title">{{ metric.title }}</div>
    <div class="product-name">产品：{{ productName }}</div>
    <div class="bars">
      <div v-for="item in metric.compareList" :key="item.label" class="bar-group">
        <div class="bar" :class="`bar-${item.type}`" :style="{ height: `${item.height}px` }">
          {{ item.value }}
        </div>
        <div class="bar-label">{{ item.label }}</div>
      </div>
    </div>
  </div>
</template>
