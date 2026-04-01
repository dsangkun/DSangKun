<script setup lang="ts">
import { computed, ref } from 'vue'
import CompetitorMonitorList from '../components/CompetitorMonitorList.vue'
import NewArrivalList from '../components/NewArrivalList.vue'
import OperationDataPanel from '../components/OperationDataPanel.vue'
import WorkbenchSection from '../components/WorkbenchSection.vue'
import { competitorChangeMock, newArrivalMock, operationDataMock } from '../mock/workbench'

const newArrivalItems = ref([...newArrivalMock])
const competitorItems = ref(competitorChangeMock)
const operationItems = ref(operationDataMock)

const totalTodoCount = computed(() => newArrivalItems.value.length + competitorItems.value.length + operationItems.value.length)
const newArrivalCountText = computed(() => `${newArrivalItems.value.length} 条待处理`)

const handleNewArrival = (id: string) => {
  newArrivalItems.value = newArrivalItems.value.filter((item) => item.id !== id)
}
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="logo">电商运营管理系统</div>
      <div class="menu-title">导航</div>
      <div class="menu-item">系统主页</div>
      <div class="menu-item active">工作台</div>
      <div class="menu-item">商品中心</div>
      <div class="menu-item">竞品中心</div>
      <div class="menu-item">数据分析</div>
    </aside>

    <main class="main-layout">
      <header class="topbar">
        <div>
          <h1 class="page-title">工作台</h1>
          <div class="subtext">当前阶段：Vue 工程化页面 + Mock 数据演示</div>
        </div>
        <div class="badge">今日待办 {{ totalTodoCount }} 项</div>
      </header>

      <div class="content-area">
        <WorkbenchSection title="板块1：竞品上新信息" desc="每条信息处理后立即从页面消失" :badge="newArrivalCountText">
          <NewArrivalList :items="newArrivalItems" @handle="(id) => handleNewArrival(id)" />
        </WorkbenchSection>

        <WorkbenchSection title="板块2：竞品监控" desc="仅展示当日有变化的已追踪竞品" :badge="`今日变化 ${competitorItems.length} 项`">
          <CompetitorMonitorList :items="competitorItems" />
        </WorkbenchSection>

        <WorkbenchSection title="板块3：运营数据" desc="按产品展示销售、流量、广告三类对比数据" badge="柱状图对比">
          <OperationDataPanel :items="operationItems" />
          <div class="empty-tip margin-top-16">
            当前阶段仍为前端演示数据。下一步可直接接入后端 API 与数据库真实数据。
          </div>
        </WorkbenchSection>
      </div>
    </main>
  </div>
</template>
