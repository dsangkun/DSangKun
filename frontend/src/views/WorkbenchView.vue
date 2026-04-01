<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import CompetitorMonitorList from '../components/CompetitorMonitorList.vue'
import NewArrivalList from '../components/NewArrivalList.vue'
import OperationDataPanel from '../components/OperationDataPanel.vue'
import WorkbenchSection from '../components/WorkbenchSection.vue'
import {
  fetchCompetitorChanges,
  fetchNewArrivals,
  fetchOperationData,
  fetchWorkbenchOverview,
  postNewArrivalAction
} from '../api/workbench'
import { competitorChangeMock, newArrivalMock, operationDataMock } from '../mock/workbench'
import type { NewArrivalActionType, WorkbenchOverview } from '../types/workbench'

const newArrivalItems = ref([...newArrivalMock])
const competitorItems = ref(competitorChangeMock)
const operationItems = ref(operationDataMock)
const overview = ref<WorkbenchOverview>({
  totalTodoCount: newArrivalMock.length + competitorChangeMock.length + operationDataMock.length,
  newArrivalCount: newArrivalMock.length,
  competitorChangeCount: competitorChangeMock.length,
  operationProductCount: operationDataMock.length
})
const loading = ref(false)
const loadError = ref('')
const usingMockData = ref(true)

const totalTodoCount = computed(() => overview.value.totalTodoCount)
const newArrivalCountText = computed(() => `${newArrivalItems.value.length} 条待处理`)

const syncOverview = () => {
  overview.value = {
    totalTodoCount: newArrivalItems.value.length + competitorItems.value.length + operationItems.value.length,
    newArrivalCount: newArrivalItems.value.length,
    competitorChangeCount: competitorItems.value.length,
    operationProductCount: operationItems.value.length
  }
}

const loadWorkbenchData = async () => {
  loading.value = true
  loadError.value = ''

  try {
    const [overviewData, newArrivals, competitorChanges, operationData] = await Promise.all([
      fetchWorkbenchOverview(),
      fetchNewArrivals(),
      fetchCompetitorChanges(),
      fetchOperationData()
    ])

    overview.value = overviewData
    newArrivalItems.value = newArrivals
    competitorItems.value = competitorChanges
    operationItems.value = operationData
    usingMockData.value = false
  } catch (error) {
    console.error('加载工作台接口失败，已回退到 Mock 数据：', error)
    loadError.value = '后端接口暂不可用，当前已自动回退到 Mock 数据。'
    newArrivalItems.value = [...newArrivalMock]
    competitorItems.value = competitorChangeMock
    operationItems.value = operationDataMock
    usingMockData.value = true
    syncOverview()
  } finally {
    loading.value = false
  }
}

const handleNewArrival = async (id: string, action: 'push' | 'track' | 'ignore') => {
  try {
    const actionMap: Record<'push' | 'track' | 'ignore', NewArrivalActionType> = {
      push: 'PUSH',
      track: 'TRACK',
      ignore: 'IGNORE'
    }

    if (!usingMockData.value) {
      await postNewArrivalAction(id, actionMap[action])
    }

    newArrivalItems.value = newArrivalItems.value.filter((item) => item.id !== id)
    syncOverview()
  } catch (error) {
    console.error('处理竞品上新事项失败：', error)
    loadError.value = '处理失败，请稍后重试。'
  }
}

onMounted(() => {
  void loadWorkbenchData()
})
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
          <div class="subtext">当前阶段：前后端联调中{{ usingMockData ? '（Mock 兜底启用）' : '（已连接后端接口）' }}</div>
        </div>
        <div class="badge">今日待办 {{ totalTodoCount }} 项</div>
      </header>

      <div class="content-area">
        <div v-if="loading" class="empty-tip margin-bottom-16">正在加载工作台数据...</div>
        <div v-if="loadError" class="empty-tip margin-bottom-16">{{ loadError }}</div>

        <WorkbenchSection title="板块1：竞品上新信息" desc="每条信息处理后立即从页面消失" :badge="newArrivalCountText">
          <NewArrivalList :items="newArrivalItems" @handle="(id, action) => handleNewArrival(id, action)" />
        </WorkbenchSection>

        <WorkbenchSection title="板块2：竞品监控" desc="仅展示当日有变化的已追踪竞品" :badge="`今日变化 ${competitorItems.length} 项`">
          <CompetitorMonitorList :items="competitorItems" />
        </WorkbenchSection>

        <WorkbenchSection title="板块3：运营数据" desc="按产品展示销售、流量、广告三类对比数据" badge="柱状图对比">
          <OperationDataPanel :items="operationItems" />
          <div class="empty-tip margin-top-16">
            {{ usingMockData ? '当前使用 Mock 数据兜底展示，后端接口恢复后会自动切换联调模式。' : '当前页面已接入后端接口，正在进行联调验证。' }}
          </div>
        </WorkbenchSection>
      </div>
    </main>
  </div>
</template>
