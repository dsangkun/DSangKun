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
import type { CompetitorChangeItem, NewArrivalActionType, NewArrivalItem, ProductOperationItem, WorkbenchOverview } from '../types/workbench'

const newArrivalItems = ref<NewArrivalItem[]>([...newArrivalMock])
const competitorItems = ref<CompetitorChangeItem[]>(competitorChangeMock)
const operationItems = ref<ProductOperationItem[]>(operationDataMock)
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

const mergeListById = <T extends { id: string }>(mockList: T[], incoming: T[]) => {
  if (!Array.isArray(incoming) || incoming.length === 0) {
    return mockList
  }

  const merged = mockList.map((item) => incoming.find((candidate) => candidate.id === item.id) ?? item)
  const extras = incoming.filter((item) => !mockList.some((mockItem) => mockItem.id === item.id))
  return [...merged, ...extras]
}

const mergeOperationData = (incoming: ProductOperationItem[]) => {
  if (!Array.isArray(incoming) || incoming.length === 0) {
    return operationDataMock
  }

  const merged = operationDataMock.map((mockItem) => {
    const matched = incoming.find((item) => item.id === mockItem.id || item.productName === mockItem.productName)

    if (!matched) {
      return mockItem
    }

    return {
      ...mockItem,
      ...matched,
      sales: matched.sales ?? mockItem.sales,
      traffic: matched.traffic ?? mockItem.traffic,
      spAds: matched.spAds ?? mockItem.spAds,
      sbvAds: matched.sbvAds ?? mockItem.sbvAds,
      review: matched.review ?? mockItem.review
    }
  })

  const extras = incoming.filter(
    (item) => !operationDataMock.some((mockItem) => mockItem.id === item.id || mockItem.productName === item.productName)
  )

  return [...merged, ...extras]
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
    newArrivalItems.value = mergeListById(newArrivalMock, newArrivals)
    competitorItems.value = mergeListById(competitorChangeMock, competitorChanges)
    operationItems.value = mergeOperationData(operationData)
    usingMockData.value = false
    syncOverview()
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
    <main class="main-layout full-width-layout">
      <header class="topbar compact-topbar">
        <div>
          <h1 class="page-title">运营数据展示模块</h1>
          <div class="subtext">当前阶段：先完成 Boss 预期的展示页{{ usingMockData ? '（页面以 Mock 展示数据为主）' : '（已连接后端接口）' }}</div>
        </div>
        <div class="badge">今日待办 {{ totalTodoCount }} 项</div>
      </header>

      <div class="content-area wide-content-area">
        <div v-if="loading" class="empty-tip margin-bottom-16">正在加载工作台数据...</div>
        <div v-if="loadError" class="empty-tip margin-bottom-16">{{ loadError }}</div>

        <WorkbenchSection title="板块1：竞品上新信息" desc="每条信息处理后立即从页面消失" :badge="newArrivalCountText">
          <NewArrivalList :items="newArrivalItems" @handle="(id, action) => handleNewArrival(id, action)" />
        </WorkbenchSection>

        <WorkbenchSection title="板块2：竞品监控" desc="仅展示当日有变化的已追踪竞品" :badge="`今日变化 ${competitorItems.length} 项`">
          <CompetitorMonitorList :items="competitorItems" />
        </WorkbenchSection>

        <WorkbenchSection title="板块3：运营数据展示模块" desc="每个产品以卡片方式展示基础信息、评价、销售、流量、SP广告、SBV广告六类信息" badge="产品卡片展示">
          <OperationDataPanel :items="operationItems" />
          <div class="empty-tip margin-top-16">
            {{ usingMockData ? '当前优先完成展示效果，模块3使用 Mock 数据进行页面验证。' : '当前页面已接入后端接口，正在进行联调验证。' }}
          </div>
        </WorkbenchSection>
      </div>
    </main>
  </div>
</template>
