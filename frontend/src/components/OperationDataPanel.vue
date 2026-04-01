<script setup lang="ts">
import type { ProductOperationItem } from '../types/workbench'
import MetricBarChart from './MetricBarChart.vue'

defineProps<{
  items: ProductOperationItem[]
}>()
</script>

<template>
  <div class="product-panel-list">
    <div v-for="item in items" :key="item.id" class="product-panel">
      <div class="product-panel-header">
        <div>
          <div class="product-panel-title">{{ item.productName }}</div>
          <div class="product-panel-desc">展示该产品当日、七日平均、上周同日的销售/流量/广告数据对比</div>
        </div>
      </div>
      <div class="data-grid">
        <MetricBarChart :product-name="item.productName" :metric="item.sales" />
        <MetricBarChart :product-name="item.productName" :metric="item.traffic" />
        <MetricBarChart :product-name="item.productName" :metric="item.ads" />
      </div>
    </div>
  </div>
</template>
