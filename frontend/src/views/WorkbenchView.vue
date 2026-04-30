<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import CompetitorMonitorList from '../components/CompetitorMonitorList.vue'
import NewArrivalList from '../components/NewArrivalList.vue'
import OperationDataPanel from '../components/OperationDataPanel.vue'
import WorkbenchSection from '../components/WorkbenchSection.vue'
import { currentUser } from '../auth/session'
import {
  fetchCompetitorChanges,
  fetchNewArrivals,
  fetchOperationData,
  fetchOperationDates,
  fetchWorkbenchOverview,
  postNewArrivalAction
} from '../api/workbench'
import { asinMapping } from '../constants/asinMapping'
import { competitorChangeMock, newArrivalMock, operationDataMock } from '../mock/workbench'
import type {
  CompetitorChangeItem,
  NewArrivalActionType,
  NewArrivalItem,
  ParentOperationCard,
  ProductOperationItem,
  WorkbenchOverview
} from '../types/workbench'

type OperatorOption = {
  id: string
  name: string
  focus: string
  parentAsins: string[]
}

const mappingOwnerNames = [...new Set(asinMapping.map((item) => item.ownerName).filter(Boolean))]

const mockOperatorOptions: OperatorOption[] = mappingOwnerNames.map((name) => ({
  id: `owner:${name}`,
  name,
  focus: `优先处理你负责父ASIN下由规则触发的异常任务，子ASIN明细后续进入数据页。`,
  parentAsins: [...new Set(asinMapping.filter((item) => item.ownerName === name).map((item) => item.parentAsin))]
}))

const selectedOperationDate = ref('2026-03-17')
const operationDateOptions = ref(['2026-03-17', '2026-03-18', '2026-03-19'])

const fallbackCoverTones: Array<'blue' | 'green' | 'orange' | 'purple'> = ['blue', 'green', 'orange', 'purple']

const currentOperatorId = computed(() => {
  if (!currentUser.value) return 'owner:all'
  if (currentUser.value.role === 'admin') return 'owner:all'
  return `owner:${currentUser.value.ownerName}`
})

const selectedOperator = computed<OperatorOption>(() => {
  if (currentUser.value?.role === 'admin') {
    return {
      id: 'owner:all',
      name: currentUser.value.displayName,
      focus: '管理员视角：查看全部父ASIN卡片、异常任务与数据页入口。',
      parentAsins: []
    }
  }

  const matched = mockOperatorOptions.find((item) => item.id === currentOperatorId.value)
  return matched ?? {
    id: currentOperatorId.value,
    name: currentUser.value?.displayName ?? '未登录',
    focus: '当前账号暂无绑定的父ASIN，请先检查映射表归属人与登录账号配置。',
    parentAsins: []
  }
})

const operatorName = computed(() => selectedOperator.value?.name ?? '未登录')
const todayFocus = computed(() => selectedOperator.value?.focus ?? '当前展示模块3父ASIN聚合数据。')

const newArrivalOwnerMap: Record<string, string> = {
  A001: 'owner:黄京梅',
  B014: 'owner:王冀婉',
  C102: 'owner:程韦嘉',
  D208: 'owner:黄京梅',
  E315: 'owner:左金晶',
  F420: 'owner:褚润佳',
  G517: 'owner:陈姝航',
  H633: 'owner:王冀婉',
  J744: 'owner:程韦嘉'
}

const competitorOwnerMap: Record<string, string> = {
  CX9: 'owner:黄京梅',
  ACS: 'owner:王冀婉',
  PMX: 'owner:程韦嘉',
  TSO: 'owner:黄京梅',
  BPF: 'owner:左金晶',
  KSR: 'owner:褚润佳',
  WBO: 'owner:陈姝航',
  NBS: 'owner:王冀婉',
  MGC: 'owner:程韦嘉'
}

const allNewArrivalItems = ref<NewArrivalItem[]>([...newArrivalMock])
const allCompetitorItems = ref<CompetitorChangeItem[]>(competitorChangeMock)
const allOperationItems = ref<ProductOperationItem[]>(operationDataMock)
const overview = ref<WorkbenchOverview>({
  totalTodoCount: newArrivalMock.length + competitorChangeMock.length + operationDataMock.length,
  newArrivalCount: newArrivalMock.length,
  competitorChangeCount: competitorChangeMock.length,
  operationProductCount: operationDataMock.length
})
const loading = ref(false)
const loadError = ref('')
const usingMockData = ref(true)

const parentMappingMap = computed(() => {
  const map = new Map<string, typeof asinMapping>()

  for (const row of asinMapping) {
    const list = map.get(row.parentAsin) ?? []
    list.push(row)
    map.set(row.parentAsin, list)
  }

  return map
})

const childToParentMap = computed(() => {
  const map = new Map<string, (typeof asinMapping)[number]>()
  asinMapping.forEach((row) => {
    map.set(row.childAsin, row)
  })
  return map
})

const buildEmptyMetricBlock = (title: string) => ({
  title,
  compareList: [
    { label: '今', value: '--', height: 18, type: 'today' as const },
    { label: '均', value: '--', height: 18, type: 'avg' as const },
    { label: '周', value: '--', height: 18, type: 'lastweek' as const },
    { label: '目', value: '--', height: 18, type: 'target' as const }
  ],
  highlights: [],
  targetValue: '--',
  targetNote: '当前父ASIN下暂无接入数据'
})

const buildEmptyAdsBlock = (title: string) => ({
  title,
  highlights: [],
  activityList: [],
  sourceNote: '当前父ASIN下暂无接入数据'
})

const buildEmptyParentCard = (parentAsin: string, index: number): ParentOperationCard => {
  const mappingRows = parentMappingMap.value.get(parentAsin) ?? []
  const parentProductName = mappingRows[0]?.parentProductName || `父ASIN ${parentAsin}`
  const ownerName = mappingRows.find((row) => row.ownerName)?.ownerName || '未分配'
  const childAsins = mappingRows.map((row) => row.childAsin)
  const childProductNames = mappingRows.map((row) => row.childProductName || row.childAsin)
  const hasSbv = mappingRows.some((row) => row.hasSbv)
  const coverText = parentProductName.replace(/\s+/g, '').slice(0, 4) || `父${index + 1}`

  return {
    id: `parent-${parentAsin}`,
    productName: parentProductName,
    productCode: parentAsin,
    shopName: 'EXPERLAM',
    siteName: '美国站',
    productTag: `${childAsins.length}个子ASIN`,
    ownerName,
    coverText,
    coverTone: fallbackCoverTones[index % fallbackCoverTones.length],
    listingTitle: `${parentProductName}（父ASIN对象）`,
    listingPrice: '--',
    productImageUrl: '',
    childAsin: parentAsin,
    childSku: '--',
    review: {
      score: '--',
      reviewCount: '--',
      newReviewCount: '0',
      badReviewCount: '0',
      latestTitle: '暂无数据',
      latestContent: '当前父ASIN下暂无接入的子ASIN运营数据。',
      latestDate: '--',
      latestAuthor: '--',
      recentComments: []
    },
    sales: buildEmptyMetricBlock('销售数据'),
    traffic: buildEmptyMetricBlock('流量数据'),
    spAds: buildEmptyAdsBlock('SP广告'),
    sbvAds: buildEmptyAdsBlock('SBV广告'),
    parentAsin,
    parentProductName,
    childItems: [],
    childAsins,
    childProductNames,
    hasSbv
  }
}

const buildParentCards = (items: ProductOperationItem[]): ParentOperationCard[] => {
  const grouped = new Map<string, ProductOperationItem[]>()

  for (const item of items) {
    const mapping = childToParentMap.value.get(item.childAsin)
    const parentAsin = mapping?.parentAsin ?? item.childAsin
    const list = grouped.get(parentAsin) ?? []
    list.push({
      ...item,
      ownerName: mapping?.ownerName || item.ownerName || '未分配'
    })
    grouped.set(parentAsin, list)
  }

  const allParentAsins = [...new Set([...parentMappingMap.value.keys(), ...grouped.keys()])]

  return allParentAsins.map((parentAsin, index) => {
    const childItems = grouped.get(parentAsin) ?? []

    if (!childItems.length) {
      return buildEmptyParentCard(parentAsin, index)
    }

    const mappingRows = parentMappingMap.value.get(parentAsin) ?? []
    const primary = childItems[0]
    const riskChild = childItems.find((item) => {
      const statuses = [...item.sales.highlights, ...item.traffic.highlights, ...item.spAds.highlights, ...item.sbvAds.highlights].map((h) => h.status)
      return Number(item.review.badReviewCount) > 0 || statuses.includes('warn') || statuses.includes('risk')
    }) ?? primary
    const ownerName = mappingRows.find((row) => row.ownerName)?.ownerName || riskChild.ownerName || '未分配'
    const parentProductName = mappingRows[0]?.parentProductName || riskChild.productName
    const childAsins = mappingRows.length ? mappingRows.map((row) => row.childAsin) : childItems.map((item) => item.childAsin)
    const childProductNames = mappingRows.length ? mappingRows.map((row) => row.childProductName || row.childAsin) : childItems.map((item) => item.productName)
    const hasSbv = mappingRows.some((row) => row.hasSbv)

    return {
      ...riskChild,
      id: `parent-${parentAsin}`,
      productName: parentProductName,
      productTag: `${childAsins.length}个子ASIN`,
      ownerName,
      coverText: riskChild.coverText || `父${index + 1}`,
      parentAsin,
      parentProductName,
      childItems,
      childAsins,
      childProductNames,
      hasSbv,
      childAsin: parentAsin,
      childSku: riskChild.childSku,
      coverTone: riskChild.coverTone || fallbackCoverTones[index % fallbackCoverTones.length]
    }
  })
}

const allParentCards = computed<ParentOperationCard[]>(() => buildParentCards(allOperationItems.value))

const scopedNewArrivalItems = computed(() => {
  if (!currentUser.value) return []
  if (currentUser.value.role === 'admin') return allNewArrivalItems.value
  if (!usingMockData.value) return allNewArrivalItems.value
  return allNewArrivalItems.value.filter((item) => newArrivalOwnerMap[item.id] === currentOperatorId.value)
})

const scopedCompetitorItems = computed(() => {
  if (!currentUser.value) return []
  if (currentUser.value.role === 'admin') return allCompetitorItems.value
  if (!usingMockData.value) return allCompetitorItems.value
  return allCompetitorItems.value.filter((item) => competitorOwnerMap[item.id] === currentOperatorId.value)
})

const scopedOperationItems = computed<ParentOperationCard[]>(() => {
  if (!currentUser.value) return []

  if (currentUser.value.role === 'admin') {
    return allParentCards.value
  }

  if (usingMockData.value) {
    return allParentCards.value.filter((item) => selectedOperator.value.parentAsins.includes(item.parentAsin))
  }

  const ownerName = currentUser.value.ownerName
  return allParentCards.value.filter((item) => (item.ownerName || '未分配') === ownerName)
})

const scopedOverview = computed<WorkbenchOverview>(() => ({
  totalTodoCount: scopedNewArrivalItems.value.length + scopedCompetitorItems.value.length + scopedOperationItems.value.length,
  newArrivalCount: scopedNewArrivalItems.value.length,
  competitorChangeCount: scopedCompetitorItems.value.length,
  operationProductCount: scopedOperationItems.value.length
}))

const operatorProductNames = computed(() => {
  return scopedOperationItems.value.map((item) => item.parentProductName)
})

const topSummaryTrendBars = computed(() => [44, 42, 47, 47, 61, 50, 46, 54, 58, 52, 51, 53])

const topSummaryTrendLinePoints = computed(() => {
  return topSummaryTrendBars.value
    .map((height, index) => `${12 + index * 20},${56 - height * 0.45}`)
    .join(' ')
})

const topSummaryGoalLeft = computed(() => `${scopedOverview.value.totalTodoCount.toLocaleString('en-US')}`)
const topSummaryGoalRight = computed(() => {
  const composite = scopedNewArrivalItems.value.length * 12000 + scopedCompetitorItems.value.length * 8600 + scopedOperationItems.value.length * 5400
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
const newArrivalCountText = computed(() => `${scopedNewArrivalItems.value.length} 条待处理`)

const syncOverview = () => {
  overview.value = {
    totalTodoCount: allNewArrivalItems.value.length + allCompetitorItems.value.length + allParentCards.value.length,
    newArrivalCount: allNewArrivalItems.value.length,
    competitorChangeCount: allCompetitorItems.value.length,
    operationProductCount: allParentCards.value.length
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
  if (!Array.isArray(incoming)) {
    return operationDataMock
  }

  if (incoming.length === 0) {
    return []
  }

  if (!usingMockData.value) {
    return incoming.map((item) => {
      const mapping = childToParentMap.value.get(item.childAsin)
      return {
        ...item,
        ownerName: mapping?.ownerName || item.ownerName || '未分配'
      }
    })
  }

  return operationDataMock
    .map((mockItem) => {
      const matched = incoming.find(
        (item) => item.id === mockItem.id || item.productName === mockItem.productName || item.childAsin === mockItem.childAsin
      )

      if (!matched) {
        return null
      }

      const mapping = childToParentMap.value.get(matched.childAsin) ?? childToParentMap.value.get(mockItem.childAsin)

      return {
        ...mockItem,
        ...matched,
        ownerName: mapping?.ownerName || matched.ownerName || mockItem.ownerName || '未分配',
        sales: matched.sales ?? mockItem.sales,
        traffic: matched.traffic ?? mockItem.traffic,
        spAds: matched.spAds ?? mockItem.spAds,
        sbvAds: matched.sbvAds ?? mockItem.sbvAds,
        review: matched.review ?? mockItem.review
      }
    })
    .filter(Boolean) as ProductOperationItem[]
}

const loadWorkbenchData = async () => {
  loading.value = true
  loadError.value = ''

  try {
    const [overviewData, newArrivals, competitorChanges, operationDates, operationData] = await Promise.all([
      fetchWorkbenchOverview(),
      fetchNewArrivals(),
      fetchCompetitorChanges(),
      fetchOperationDates(),
      fetchOperationData(selectedOperationDate.value)
    ])

    if (operationDates.length > 0) {
      operationDateOptions.value = operationDates
      if (!operationDates.includes(selectedOperationDate.value)) {
        selectedOperationDate.value = operationDates[0]
      }
    }

    overview.value = overviewData
    allNewArrivalItems.value = mergeListById(newArrivalMock, newArrivals)
    allCompetitorItems.value = mergeListById(competitorChangeMock, competitorChanges)
    usingMockData.value = false
    allOperationItems.value = mergeOperationData(operationData)
    syncOverview()
  } catch (error) {
    console.error('加载工作台接口失败，已回退到 Mock 数据：', error)
    loadError.value = '后端接口暂不可用，当前已自动回退到 Mock 数据。'
    allNewArrivalItems.value = [...newArrivalMock]
    allCompetitorItems.value = competitorChangeMock
    operationDateOptions.value = ['2026-03-17', '2026-03-18', '2026-03-19']
    allOperationItems.value = selectedOperationDate.value === '2026-03-17' ? operationDataMock : []
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

    allNewArrivalItems.value = allNewArrivalItems.value.filter((item) => item.id !== id)
    syncOverview()
  } catch (error) {
    console.error('处理竞品上新事项失败：', error)
    loadError.value = '处理失败，请稍后重试。'
  }
}

const handleModuleThreeProgressChange = (payload: { total: number, read: number }) => {
  moduleThreeProgress.value = payload
}

watch(selectedOperationDate, () => {
  void loadWorkbenchData()
})

onMounted(() => {
  void loadWorkbenchData()
})
</script>

<template>
  <div class="amazon-workbench-page">
    <section class="hero-panel">
      <div class="hero-main-block">
        <div class="hero-greeting">{{ operatorName }}，今日工作</div>
        <div class="hero-desc">{{ todayFocus }}</div>
        <div class="operator-switch-panel login-scope-panel">
          <div class="operator-switch-head">
            <span class="operator-switch-label">当前登录账号</span>
            <span class="operator-switch-note">已按登录运营自动过滤可见父ASIN</span>
          </div>
          <div class="operator-scope-text">
            当前负责父ASIN：{{ operatorProductNames.length ? operatorProductNames.join('、') : '暂无分配' }}
          </div>
        </div>
      </div>
      <div class="hero-status-card">
        <div class="hero-status-label">当前数据状态</div>
        <div class="hero-status-value">{{ usingMockData ? 'Mock展示中' : '接口联调中' }}</div>
        <div class="hero-status-note">模块3已切到登录态过滤 + 父ASIN卡片 + 子ASIN异常聚合</div>
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
        <NewArrivalList :items="scopedNewArrivalItems" @handle="(id, action) => handleNewArrival(id, action)" />
      </WorkbenchSection>

      <WorkbenchSection title="竞品监控" desc="仅展示当日有变化的已追踪竞品" :badge="`今日变化 ${scopedCompetitorItems.length} 项`">
        <CompetitorMonitorList :items="scopedCompetitorItems" />
      </WorkbenchSection>

      <WorkbenchSection title="Amazon运营推进器" desc="模块3重点区：已切到登录态控制可见范围；卡片为父ASIN对象，任意子ASIN异常都会归集到父卡片" badge="模块3重点区">
        <OperationDataPanel :items="scopedOperationItems" @progress-change="handleModuleThreeProgressChange" />
        <div class="empty-tip margin-top-16">
          {{ scopedOperationItems.length
            ? (usingMockData
              ? `当前为 ${selectedOperationDate} 的父ASIN聚合展示数据；后续数据页会承载子ASIN维度明细。`
              : `当前展示 ${selectedOperationDate} 的父ASIN聚合数据。`)
            : `当前登录账号下暂无可见的模块三运营数据。` }}
        </div>
      </WorkbenchSection>
    </div>
  </div>
</template>
