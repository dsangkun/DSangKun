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

const operatorName = ref('桑坤')
const todayFocus = ref('优先核对目标数据、处理未读工作进度，并继续推进模块3前端改版。')

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

const topSummaryTrendBars = computed(() => [44, 42, 47, 47, 61, 50, 46, 54, 58, 52, 51, 53])

const topSummaryTrendLinePoints = computed(() => {
  return topSummaryTrendBars.value
    .map((height, index) => `${12 + index * 20},${56 - height * 0.45}`)
    .join(' ')
})

const topSummaryGoalLeft = computed(() => `${overview.value.totalTodoCount.toLocaleString('en-US')}`)
const topSummaryGoalRight = computed(() => {
  const composite = newArrivalItems.value.length * 12000 + competitorItems.value.length * 8600 + operationItems.value.length * 5400
  return `$${composite.toLocaleString('en-US')}`
})

const progressRingStyle = computed(() => {
  const completeDeg = Math.max(24, Math.min(300, progressPercent.value * 3.2))
  const secondDeg = Math.min(340, completeDeg + 88)
  return {
    background: `conic-gradient(#ffac2f 0deg ${completeDeg}deg, #7d8dff ${completeDeg}deg ${secondDeg}deg, #60a5fa ${secondDeg}deg 360deg)`
  }
})

const projectPreviewPercent = computed(() => `${(progressPercent.value * 0.76).toFixed(1)}%`)
const projectPreviewLines = [1, 2, 3, 4]

const moduleThreeProgress = ref({ total: 0, read: 0 })

const progressPercent = computed(() => {
  if (!moduleThreeProgress.value.total) return 0
  return Math.round((moduleThreeProgress.value.read / moduleThreeProgress.value.total) * 100)
})

const unreadProgressCount = computed(() => Math.max(0, moduleThreeProgress.value.total - moduleThreeProgress.value.read))
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

const handleModuleThreeProgressChange = (payload: { total: number, read: number }) => {
  moduleThreeProgress.value = payload
}

onMounted(() => {
  void loadWorkbenchData()
})
</script>

<template>
  <div class="amazon-workbench-page">
    <section class="hero-panel">
      <div>
        <div class="hero-greeting">{{ operatorName }}，今日工作</div>
        <div class="hero-desc">{{ todayFocus }}</div>
      </div>
      <div class="hero-status-card">
        <div class="hero-status-label">当前数据状态</div>
        <div class="hero-status-value">{{ usingMockData ? 'Mock展示中' : '接口联调中' }}</div>
        <div class="hero-status-note">模块3优先保功能，再做视觉精修</div>
      </div>
    </section>

    <div class="content-area workbench-shell-content">
      <div v-if="loading" class="empty-tip margin-bottom-16">正在加载工作台数据...</div>
      <div v-if="loadError" class="empty-tip margin-bottom-16">{{ loadError }}</div>

      <section class="template-top-summary-grid">
        <article class="template-top-summary-card template-top-summary-card-wide">
          <div class="template-top-summary-head">
            <span class="template-top-summary-label">目标销量</span>
            <span class="template-top-summary-label right">达成销量</span>
          </div>
          <div class="template-top-summary-values dual">
            <div class="template-top-summary-value">{{ topSummaryGoalLeft }}</div>
            <div class="template-top-summary-value right">{{ topSummaryGoalRight }}</div>
          </div>
          <div class="template-top-summary-chart">
            <div class="template-top-summary-grid-lines">
              <span v-for="line in 4" :key="`goal-line-${line}`"></span>
            </div>
            <div class="template-top-summary-bars">
              <div v-for="(height, index) in topSummaryTrendBars" :key="`goal-bar-${index}`" class="template-top-summary-bar-item">
                <span class="template-top-summary-bar" :style="{ height: `${height}px` }"></span>
              </div>
            </div>
            <svg class="template-top-summary-line-svg" viewBox="0 0 244 64" preserveAspectRatio="none" aria-hidden="true">
              <polyline class="template-top-summary-line" :points="topSummaryTrendLinePoints" />
              <circle v-for="(height, index) in topSummaryTrendBars" :key="`goal-point-${index}`" class="template-top-summary-point" :cx="12 + index * 20" :cy="56 - height * 0.45" r="2.4" />
            </svg>
          </div>
        </article>

        <article class="template-top-summary-card template-top-summary-card-progress">
          <div class="template-top-summary-head single">
            <span class="template-top-summary-label">进入工作进度</span>
          </div>
          <div class="template-top-summary-progress-text">
            <strong>{{ unreadProgressCount }} 项未读</strong>
            <span>{{ progressPercent.toFixed(2) }}%</span>
          </div>
          <div class="template-top-summary-donut-wrap">
            <div class="template-top-summary-donut" :style="progressRingStyle">
              <span class="template-top-summary-donut-inner"></span>
            </div>
          </div>
        </article>

        <article class="template-top-summary-card template-top-summary-card-project">
          <div class="template-top-summary-head single">
            <span class="template-top-summary-label">今日项目</span>
          </div>
          <div class="template-top-summary-project-value">{{ projectPreviewPercent }}</div>
          <div class="template-top-summary-project-lines">
            <span v-for="line in projectPreviewLines" :key="`project-line-${line}`"></span>
          </div>
        </article>
      </section>

      <WorkbenchSection title="竞品上新信息" desc="每条信息处理后立即从页面消失" :badge="newArrivalCountText">
        <NewArrivalList :items="newArrivalItems" @handle="(id, action) => handleNewArrival(id, action)" />
      </WorkbenchSection>

      <WorkbenchSection title="竞品监控" desc="仅展示当日有变化的已追踪竞品" :badge="`今日变化 ${competitorItems.length} 项`">
        <CompetitorMonitorList :items="competitorItems" />
      </WorkbenchSection>

      <WorkbenchSection title="Amazon运营推进器" desc="模块3重点区：保留产品卡片、评价、销售、流量、SP广告、SBV广告等原有业务语义与交互" badge="模块3重点区">
        <OperationDataPanel :items="operationItems" @progress-change="handleModuleThreeProgressChange" />
        <div class="empty-tip margin-top-16">
          {{ usingMockData ? '当前优先完成前端骨架与展示验证，模块3继续使用 Mock 数据承接功能。' : '当前页面已接入后端接口，模块3正在联调验证。' }}
        </div>
      </WorkbenchSection>
    </div>
  </div>
</template>
