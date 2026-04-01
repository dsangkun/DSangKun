<script setup lang="ts">
import type { NewArrivalItem } from '../types/workbench'

const props = defineProps<{
  items: NewArrivalItem[]
}>()

const emit = defineEmits<{
  handle: [id: string, action: 'push' | 'track' | 'ignore']
}>()

const actionMap = {
  push: '信息推送',
  track: '追踪',
  ignore: '忽略'
} as const
</script>

<template>
  <div v-if="props.items.length" class="todo-list">
    <div v-for="item in props.items" :key="item.id" class="todo-card">
      <div class="todo-top">
        <div>
          <div class="todo-title">{{ item.title }}</div>
          <div class="todo-meta">上新时间：{{ item.time }} ｜ 类目：{{ item.category }} ｜ 店铺：{{ item.shop }}</div>
          <div class="todo-link">
            快照主页链接：
            <a :href="item.snapshotUrl" target="_blank" rel="noreferrer">{{ item.snapshotUrl }}</a>
          </div>
        </div>
        <div class="badge badge-light">新品</div>
      </div>
      <div class="actions">
        <button class="btn btn-primary" @click="emit('handle', item.id, 'push')">{{ actionMap.push }}</button>
        <button class="btn btn-warning" @click="emit('handle', item.id, 'track')">{{ actionMap.track }}</button>
        <button class="btn btn-muted" @click="emit('handle', item.id, 'ignore')">{{ actionMap.ignore }}</button>
      </div>
    </div>
  </div>
  <div v-else class="empty-tip">当前没有待处理的竞品上新信息。</div>
</template>
