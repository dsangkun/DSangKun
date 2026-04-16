<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type { MetricHighlightItem, ProductAdsBlock, ProductMetricBlock, ProductOperationItem } from '../types/workbench'

type HealthLevel = 'good' | 'medium' | 'risk'
type ExpandSectionKey = 'review' | 'sales' | 'traffic' | 'spAds' | 'sbvAds'

const props = defineProps<{
  items: ProductOperationItem[]
}>()

const emit = defineEmits<{
  progressChange: [payload: { total: number, read: number }]
}>()

type FocusMetricRow = {
  key: 'today' | 'avg' | 'lastweek' | 'target'
  label: string
  value: string
  barHeight: number
  pointX: number
  pointY: number
}

type FocusMetricCard = {
  key: string
  title: string
  value: string
  rows: FocusMetricRow[]
}

const expandedSections = reactive<Record<string, Partial<Record<ExpandSectionKey, boolean>>>>({})
const readSections = reactive<Record<string, Partial<Record<ExpandSectionKey, boolean>>>>({})

const hasItems = computed(() => props.items.length > 0)

const getLevelWeight = (level: HealthLevel) => {
  if (level === 'risk') return 3
  if (level === 'medium') return 2
  return 1
}

const getCardLevel = (item: ProductOperationItem): HealthLevel => {
  const highlightStatuses = [
    ...item.sales.highlights,
    ...item.traffic.highlights,
    ...item.spAds.highlights,
    ...item.sbvAds.highlights
  ].map((highlight) => highlight.status ?? 'neutral')

  if (highlightStatuses.includes('risk') || Number(item.review.badReviewCount) >= 3) {
    return 'risk'
  }

  if (highlightStatuses.includes('warn') || Number(item.review.badReviewCount) > 0) {
    return 'medium'
  }

  return 'good'
}

const sortedItems = computed(() => {
  return [...props.items]
    .map((item) => ({
      ...item,
      healthLevel: getCardLevel(item) as HealthLevel
    }))
    .sort((a, b) => getLevelWeight(b.healthLevel) - getLevelWeight(a.healthLevel))
})

const sectionProgressKeys: ExpandSectionKey[] = ['review', 'sales', 'traffic', 'spAds', 'sbvAds']

const emitProgressChange = () => {
  const total = props.items.length * sectionProgressKeys.length
  const read = props.items.reduce((count, item) => {
    return count + sectionProgressKeys.filter((section) => Boolean(readSections[item.id]?.[section])).length
  }, 0)

  emit('progressChange', { total, read })
}

const isExpanded = (itemId: string, section: ExpandSectionKey) => {
  return Boolean(expandedSections[itemId]?.[section])
}

const toggleSection = (itemId: string, section: ExpandSectionKey) => {
  if (!expandedSections[itemId]) {
    expandedSections[itemId] = {}
  }

  expandedSections[itemId][section] = !expandedSections[itemId][section]
}

const isRead = (itemId: string, section: ExpandSectionKey) => {
  return Boolean(readSections[itemId]?.[section])
}

const markAsRead = (itemId: string, section: ExpandSectionKey) => {
  if (!readSections[itemId]) {
    readSections[itemId] = {}
  }

  readSections[itemId][section] = true
  emitProgressChange()
}

watch(
  () => props.items,
  () => {
    emitProgressChange()
  },
  { immediate: true, deep: true }
)

const getSectionCount = (itemId: string) => {
  const state = expandedSections[itemId]
  if (!state) return 0
  return Object.values(state).filter(Boolean).length
}

const getSectionTitle = (section: ExpandSectionKey) => {
  const titleMap: Record<ExpandSectionKey, string> = {
    review: '评价信息',
    sales: '销售数据',
    traffic: '流量数据',
    spAds: 'SP广告',
    sbvAds: 'SBV广告'
  }

  return titleMap[section]
}

const getRankLabel = (index: number) => {
  return `#${index + 1}`
}

const getHealthLabel = (level: HealthLevel) => {
  if (level === 'risk') return '风险'
  if (level === 'medium') return '中'
  return '好'
}

const getMetricPreview = (section: ProductMetricBlock) => {
  return section.compareList.map((item) => `${item.label}${item.value}`).join(' / ')
}

const getAdsSummaryRows = (highlight: MetricHighlightItem) => {
  const source = `${highlight.label}-${highlight.value}-${highlight.status ?? 'neutral'}`
  let seed = 0

  for (const char of source) {
    seed = (seed * 31 + char.charCodeAt(0)) % 9973
  }

  const labels = ['今日', '平均', '上周', '目标']
  const baseMap: Record<string, number[]> = {
    good: [88, 72, 80, 76],
    warn: [74, 82, 68, 70],
    risk: [62, 78, 58, 66],
    neutral: [80, 76, 72, 74]
  }

  const base = baseMap[highlight.status ?? 'neutral'] ?? baseMap.neutral
  return labels.map((label, index) => {
    const width = Math.max(34, Math.min(100, base[index] - ((seed >> (index * 2)) % 10)))
    const numericText = String(highlight.value).replace(/^\$/, '')
    return {
      label,
      width,
      value: index === 0 ? highlight.value : `${numericText}`
    }
  })
}

const getTemplateChartBarHeight = (metricWidth: number) => {
  return Math.max(16, Math.round(metricWidth * 0.42))
}

const getTemplateChartNodes = (rows: Array<{ width: number }>) => {
  const startX = 16
  const stepX = 34
  const baseY = 64

  return rows.map((row, index) => {
    const barHeight = getTemplateChartBarHeight(row.width)
    const lift = Math.max(8, Math.round(row.width * 0.34))
    return {
      x: startX + index * stepX,
      y: baseY - lift,
      barHeight
    }
  })
}

const getTemplateChartPoints = (rows: Array<{ width: number }>) => {
  return getTemplateChartNodes(rows)
    .map((node) => `${node.x},${node.y}`)
    .join(' ')
}

const focusMetricTypeOrder: Array<'today' | 'avg' | 'lastweek' | 'target'> = ['today', 'avg', 'lastweek', 'target']
const focusMetricLabelMap: Record<'today' | 'avg' | 'lastweek' | 'target', string> = {
  today: '今日',
  avg: '平均',
  lastweek: '上周',
  target: '目标'
}
const focusMetricFallbackRatio: Record<'today' | 'avg' | 'lastweek' | 'target', number> = {
  today: 1,
  avg: 0.88,
  lastweek: 0.82,
  target: 0.93
}
const focusMetricPointXList = [20.5, 44.5, 68.5, 92.5]

const getFocusMetricPointY = (barHeight: number) => {
  const svgHeight = 78
  const chartBoxHeight = 64
  const baselineBottom = 9
  const baselineY = svgHeight - (baselineBottom / chartBoxHeight) * svgHeight
  const scaledBarHeight = (barHeight / chartBoxHeight) * svgHeight
  return Number((baselineY - scaledBarHeight).toFixed(1))
}

const getFocusMetricRows = (section: ProductMetricBlock, highlight: MetricHighlightItem): FocusMetricRow[] => {
  const compareMap = new Map(section.compareList.map((item) => [item.type, item]))
  const rawHeights = focusMetricTypeOrder.map((type) => compareMap.get(type)?.height ?? Math.round(56 * focusMetricFallbackRatio[type]))
  const minHeight = Math.min(...rawHeights)
  const maxHeight = Math.max(...rawHeights)
  const heightRange = Math.max(1, maxHeight - minHeight)
  const parsedHighlight = parseMetricNumber(highlight.value)

  return focusMetricTypeOrder.map((type, index) => {
    const compareItem = compareMap.get(type)
    const rawBarHeight = compareItem ? compareItem.height : Math.round(56 * focusMetricFallbackRatio[type])
    const normalizedHeight = (rawBarHeight - minHeight) / heightRange
    const ratio = compareItem ? compareItem.height / Math.max(1, rawHeights[0]) : focusMetricFallbackRatio[type]
    const barHeight = Math.round(11 + normalizedHeight * 19)
    const value = type === 'today'
      ? highlight.value
      : parsedHighlight === null
        ? compareItem?.value ?? highlight.value
        : scaleMetricValue(highlight.value, ratio)

    return {
      key: type,
      label: focusMetricLabelMap[type],
      value,
      barHeight,
      pointX: focusMetricPointXList[index] ?? focusMetricPointXList[focusMetricPointXList.length - 1],
      pointY: getFocusMetricPointY(barHeight)
    }
  })
}

const getFocusMetricCards = (section: ProductMetricBlock): FocusMetricCard[] => {
  return section.highlights.map((highlight) => ({
    key: highlight.label,
    title: highlight.label,
    value: highlight.value,
    rows: getFocusMetricRows(section, highlight)
  }))
}

const getFocusMetricRowsFromAds = (highlight: MetricHighlightItem): FocusMetricRow[] => {
  const rows = getAdsSummaryRows(highlight)
  const rawHeights = rows.map((row) => row.width)
  const minHeight = Math.min(...rawHeights)
  const maxHeight = Math.max(...rawHeights)
  const heightRange = Math.max(1, maxHeight - minHeight)
  return rows.map((row, index) => {
    const normalizedHeight = (row.width - minHeight) / heightRange
    const barHeight = Math.round(11 + normalizedHeight * 19)

    return {
      key: focusMetricTypeOrder[index],
      label: row.label,
      value: row.value,
      barHeight,
      pointX: focusMetricPointXList[index] ?? focusMetricPointXList[focusMetricPointXList.length - 1],
      pointY: getFocusMetricPointY(barHeight)
    }
  })
}

const getFocusMetricCardsFromAds = (section: ProductAdsBlock): FocusMetricCard[] => {
  return section.highlights.map((highlight) => ({
    key: highlight.label,
    title: highlight.label,
    value: highlight.value,
    rows: getFocusMetricRowsFromAds(highlight)
  }))
}

const getFocusMetricChartPoints = (rows: FocusMetricRow[]) => {
  return rows.map((row) => `${row.pointX},${row.pointY}`).join(' ')
}

const getFocusMetricChartPath = (rows: FocusMetricRow[]) => {
  if (!rows.length) return ''
  if (rows.length === 1) return `M ${rows[0].pointX} ${rows[0].pointY}`

  let path = `M ${rows[0].pointX} ${rows[0].pointY}`

  for (let index = 0; index < rows.length - 1; index += 1) {
    const current = rows[index]
    const next = rows[index + 1]
    const controlX = (current.pointX + next.pointX) / 2
    path += ` C ${controlX} ${current.pointY}, ${controlX} ${next.pointY}, ${next.pointX} ${next.pointY}`
  }

  return path
}

const parseMetricNumber = (value: string) => {
  const cleaned = String(value).replace(/[$,% ,]/g, '')
  const parsed = Number(cleaned)
  return Number.isFinite(parsed) ? parsed : null
}

const formatMetricLike = (sourceValue: string, nextNumber: number) => {
  const raw = String(sourceValue)
  const hasDollar = raw.includes('$')
  const hasPercent = raw.includes('%')
  const decimalDigits = raw.includes('.') ? (raw.split('.')[1]?.replace(/[^0-9].*$/, '').length ?? 0) : 0
  const formatted = nextNumber.toLocaleString('en-US', {
    minimumFractionDigits: decimalDigits,
    maximumFractionDigits: decimalDigits
  })

  if (hasDollar) return `$${formatted}`
  if (hasPercent) return `${formatted}%`
  return formatted
}

const scaleMetricValue = (sourceValue: string, factor: number) => {
  const parsed = parseMetricNumber(sourceValue)
  if (parsed === null) return sourceValue
  return formatMetricLike(sourceValue, parsed * factor)
}

const getSalesModules = (item: ProductOperationItem) => {
  const totalHighlights = item.sales.highlights
  const factorMap: Record<string, number> = {
    销量: 0.72,
    销售额: 0.71,
    订单数: 0.72,
    转化率: 0.88
  }

  const naturalHighlights = item.sales.highlights.map((highlight) => ({
    ...highlight,
    value: scaleMetricValue(highlight.value, factorMap[highlight.label] ?? 0.75),
    note: highlight.label === '订单数' ? '自然单为主' : highlight.note
  }))

  return [
    { key: 'total', title: '总数据', highlights: totalHighlights },
    { key: 'natural', title: '自然数据', highlights: naturalHighlights }
  ]
}

const shortenText = (text: string, max = 26) => {
  const source = String(text ?? '').trim()
  if (!source) return '--'
  return source.length > max ? `${source.slice(0, max)}...` : source
}

const getTemplateReviewChips = (item: ProductOperationItem) => {
  const comments = item.review.recentComments?.map((comment) => shortenText(comment.content, 24)) ?? []
  const fallback = [shortenText(item.review.latestTitle, 24), shortenText(item.review.latestContent, 24)].filter(Boolean)
  return [...comments, ...fallback].slice(0, 4)
}

const getSequenceNumber = (itemId: string) => {
  const parsed = Number(String(itemId).replace(/\D/g, ''))
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1
}

const getCategoryRows = (item: ProductOperationItem) => {
  const seq = getSequenceNumber(item.id)
  return [
    { rank: `#${2700 + seq * 19}`, text: 'in Pet Supplies' },
    { rank: `#${Math.max(1, seq)}`, text: `in ${shortenText(item.productName, 26)}` }
  ]
}

const getPreviewCards = (item: ProductOperationItem) => {
  const buildCompareRows = (compareList: ProductMetricBlock['compareList']) =>
    compareList.map((bar) => ({
      label: bar.label,
      width: Math.max(26, Math.min(100, bar.height * 1.6)),
      value: bar.value
    }))

  return [
    {
      key: 'sales',
      title: item.sales.title,
      value: item.sales.highlights[0]?.value ?? '--',
      rows: buildCompareRows(item.sales.compareList)
    },
    {
      key: 'traffic',
      title: item.traffic.title,
      value: item.traffic.highlights[0]?.value ?? '--',
      rows: buildCompareRows(item.traffic.compareList)
    },
    {
      key: 'sp',
      title: 'SP广告',
      value: item.spAds.highlights[0]?.value ?? '--',
      rows: item.spAds.highlights[0] ? getAdsSummaryRows(item.spAds.highlights[0]) : []
    },
    {
      key: 'sbv',
      title: 'SBV广告',
      value: item.sbvAds.highlights[0]?.value ?? '--',
      rows: item.sbvAds.highlights[0] ? getAdsSummaryRows(item.sbvAds.highlights[0]) : []
    }
  ]
}

</script>

<template>
  <div v-if="hasItems" class="operation-showcase">
    <div class="status-guide">
      <span class="status-guide-item good"><i></i> 好：当前表现稳定，可常规关注</span>
      <span class="status-guide-item medium"><i></i> 中：有波动，建议优先看详情</span>
      <span class="status-guide-item risk"><i></i> 风险：存在明显异常，建议优先处理</span>
    </div>

    <div class="product-showcase-grid operation-reference-grid">
      <article
        v-for="(item, index) in sortedItems"
        :key="item.id"
        class="operation-card reference-operation-card template-reference-card"
        :class="`card-level-${item.healthLevel}`"
      >
        <div class="reference-rank-ribbon">{{ getRankLabel(index) }}</div>
        <div class="template-card-shell">
          <div class="template-image-wrap">
            <div v-if="item.productImageUrl" class="template-image-box">
              <img :src="item.productImageUrl" :alt="item.productName" class="template-product-image" />
            </div>
            <div v-else class="template-image-box template-cover-box" :class="`product-cover-${item.coverTone}`">
              <span>{{ item.coverText }}</span>
            </div>
            <div class="template-play-btn">▶</div>
          </div>

          <div class="template-title-block">
            <div class="template-product-title">{{ item.listingTitle || item.productName }}</div>
            <div class="template-rating-line">
              <span class="template-stars">★★★★★</span>
              <span class="template-rating-count">{{ item.review.reviewCount }}</span>
            </div>
            <div class="template-offer-line">4 offers from {{ item.listingPrice || '--' }}</div>
          </div>

          <div class="template-asin-block">
            <div class="template-asin-line">
              <span>ASIN: {{ item.childAsin }}</span>
              <span class="template-icon-line">◔ ◌ ⌁</span>
            </div>
            <div class="template-brand-line">
              <span>品牌: <strong>{{ item.shopName }}</strong></span>
              <span class="template-add-badge">加入产品库</span>
            </div>
          </div>

          <div class="template-rank-block">
            <div v-for="row in getCategoryRows(item)" :key="`${item.id}-${row.rank}-${row.text}`" class="template-rank-row">
              <span class="template-rank-no">{{ row.rank }}</span>
              <span class="template-rank-text">{{ row.text }}</span>
            </div>
          </div>

          <div class="template-accordion-list">
            <section class="template-accordion-section">
              <button class="template-accordion-btn" :class="{ done: isRead(item.id, 'review') }" @click="toggleSection(item.id, 'review')">
                <span>Top highlights <em>（评价信息）</em></span>
                <span class="template-accordion-arrow" :class="{ open: isExpanded(item.id, 'review') }">⌄</span>
              </button>
              <div v-if="isExpanded(item.id, 'review')" class="template-accordion-panel">
                <div class="template-chip-grid">
                  <span v-for="chip in getTemplateReviewChips(item)" :key="`${item.id}-${chip}`" class="template-chip">{{ chip }}</span>
                </div>
                <div class="template-inline-action">
                  <button class="section-read-btn template-read-btn" :class="{ done: isRead(item.id, 'review') }" @click="markAsRead(item.id, 'review')">{{ isRead(item.id, 'review') ? '已读' : '标记为已读' }}</button>
                </div>
              </div>
            </section>

            <section class="template-accordion-section">
              <button class="template-accordion-btn" :class="{ done: isRead(item.id, 'sales') }" @click="toggleSection(item.id, 'sales')">
                <span>Product specifications <em>（销售数据）</em></span>
                <span class="template-accordion-arrow" :class="{ open: isExpanded(item.id, 'sales') }">⌄</span>
              </button>
              <div v-if="isExpanded(item.id, 'sales')" class="template-accordion-panel">
                <div class="template-inline-action">
                  <button class="section-read-btn template-read-btn" :class="{ done: isRead(item.id, 'sales') }" @click="markAsRead(item.id, 'sales')">{{ isRead(item.id, 'sales') ? '已读' : '标记为已读' }}</button>
                </div>
                <div class="template-focus-metric-grid">
                  <div v-for="card in getFocusMetricCards(item.sales)" :key="`${item.id}-sales-${card.key}`" class="template-focus-metric-card">
                    <div class="template-focus-metric-title">{{ card.title }}</div>
                    <div class="template-focus-metric-main">{{ card.value }}</div>
                    <div class="template-focus-metric-body">
                      <div class="template-focus-metric-chart-area">
                        <div class="template-focus-metric-chart-box">
                          <span class="template-focus-metric-chart-baseline"></span>
                          <div class="template-focus-metric-chart-series">
                            <div v-for="row in card.rows" :key="`${item.id}-sales-chart-${card.key}-${row.key}`" class="template-focus-metric-chart-column">
                              <span class="template-focus-metric-chart-bar" :style="{ height: `${row.barHeight}px` }"></span>
                            </div>
                          </div>
                          <svg class="template-focus-metric-chart-svg" viewBox="0 0 108 78" preserveAspectRatio="none" aria-hidden="true">
                            <path class="template-focus-metric-chart-line" :d="getFocusMetricChartPath(card.rows)" />
                          </svg>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <section class="template-accordion-section">
              <button class="template-accordion-btn" :class="{ done: isRead(item.id, 'traffic') }" @click="toggleSection(item.id, 'traffic')">
                <span>About the Brand <em>（流量数据）</em></span>
                <span class="template-accordion-arrow" :class="{ open: isExpanded(item.id, 'traffic') }">⌄</span>
              </button>
              <div v-if="isExpanded(item.id, 'traffic')" class="template-accordion-panel">
                <div class="template-inline-action">
                  <button class="section-read-btn template-read-btn" :class="{ done: isRead(item.id, 'traffic') }" @click="markAsRead(item.id, 'traffic')">{{ isRead(item.id, 'traffic') ? '已读' : '标记为已读' }}</button>
                </div>
                <div class="template-focus-metric-grid">
                  <div v-for="card in getFocusMetricCards(item.traffic)" :key="`${item.id}-traffic-${card.key}`" class="template-focus-metric-card">
                    <div class="template-focus-metric-title">{{ card.title }}</div>
                    <div class="template-focus-metric-main">{{ card.value }}</div>
                    <div class="template-focus-metric-body">
                      <div class="template-focus-metric-chart-area">
                        <div class="template-focus-metric-chart-box">
                          <span class="template-focus-metric-chart-baseline"></span>
                          <div class="template-focus-metric-chart-series">
                            <div v-for="row in card.rows" :key="`${item.id}-traffic-chart-${card.key}-${row.key}`" class="template-focus-metric-chart-column">
                              <span class="template-focus-metric-chart-bar" :style="{ height: `${row.barHeight}px` }"></span>
                            </div>
                          </div>
                          <svg class="template-focus-metric-chart-svg" viewBox="0 0 108 78" preserveAspectRatio="none" aria-hidden="true">
                            <path class="template-focus-metric-chart-line" :d="getFocusMetricChartPath(card.rows)" />
                          </svg>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <section class="template-accordion-section">
              <button class="template-accordion-btn" :class="{ done: isRead(item.id, 'spAds') }" @click="toggleSection(item.id, 'spAds')">
                <span>SP Ads <em>（SP广告）</em></span>
                <span class="template-accordion-arrow" :class="{ open: isExpanded(item.id, 'spAds') }">⌄</span>
              </button>
              <div v-if="isExpanded(item.id, 'spAds')" class="template-accordion-panel">
                <div class="template-inline-action">
                  <button class="section-read-btn template-read-btn" :class="{ done: isRead(item.id, 'spAds') }" @click="markAsRead(item.id, 'spAds')">{{ isRead(item.id, 'spAds') ? '已读' : '标记为已读' }}</button>
                </div>
                <div class="template-focus-metric-grid">
                  <div v-for="card in getFocusMetricCardsFromAds(item.spAds)" :key="`${item.id}-sp-${card.key}`" class="template-focus-metric-card">
                    <div class="template-focus-metric-title">{{ card.title }}</div>
                    <div class="template-focus-metric-main">{{ card.value }}</div>
                    <div class="template-focus-metric-body">
                      <div class="template-focus-metric-chart-area">
                        <div class="template-focus-metric-chart-box">
                          <span class="template-focus-metric-chart-baseline"></span>
                          <div class="template-focus-metric-chart-series">
                            <div v-for="row in card.rows" :key="`${item.id}-sp-chart-${card.key}-${row.key}`" class="template-focus-metric-chart-column">
                              <span class="template-focus-metric-chart-bar" :style="{ height: `${row.barHeight}px` }"></span>
                            </div>
                          </div>
                          <svg class="template-focus-metric-chart-svg" viewBox="0 0 108 78" preserveAspectRatio="none" aria-hidden="true">
                            <path class="template-focus-metric-chart-line" :d="getFocusMetricChartPath(card.rows)" />
                          </svg>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="detail-table-tip">SP 广告：按广告ASIN筛选该产品的 SP 活动数据。</div>
                <div class="detail-table-scroll ads-table-scroll">
                  <div class="detail-data-table ads-activity-table-wrap">
                    <div class="detail-table-header">SP广告活动明细</div>
                    <table class="ads-activity-table sticky-ads-table">
                      <thead>
                        <tr>
                          <th>广告活动名称</th>
                          <th>曝光量</th>
                          <th>点击量</th>
                          <th>点击率 (CTR)</th>
                          <th>单次点击成本 (CPC)</th>
                          <th>花费</th>
                          <th>总销售额</th>
                          <th>广告投入产出比</th>
                          <th>总订单数</th>
                          <th>转化率 (CVR)</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="activity in item.spAds.activityList" :key="`sp-${activity.campaignName}`">
                          <td>
                            <a v-if="activity.campaignUrl" :href="activity.campaignUrl" target="_blank" rel="noopener noreferrer" class="campaign-link">{{ activity.campaignName }}</a>
                            <span v-else>{{ activity.campaignName }}</span>
                          </td>
                          <td>{{ activity.impressions }}</td>
                          <td>{{ activity.clicks }}</td>
                          <td>{{ activity.ctr }}</td>
                          <td>{{ activity.cpc }}</td>
                          <td>{{ activity.cost }}</td>
                          <td>{{ activity.sales }}</td>
                          <td>{{ activity.acos }}</td>
                          <td>{{ activity.orders }}</td>
                          <td>{{ activity.cvr }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </section>

            <section class="template-accordion-section">
              <button class="template-accordion-btn" :class="{ done: isRead(item.id, 'sbvAds') }" @click="toggleSection(item.id, 'sbvAds')">
                <span>SBV Ads <em>（SBV广告）</em></span>
                <span class="template-accordion-arrow" :class="{ open: isExpanded(item.id, 'sbvAds') }">⌄</span>
              </button>
              <div v-if="isExpanded(item.id, 'sbvAds')" class="template-accordion-panel">
                <div class="template-inline-action">
                  <button class="section-read-btn template-read-btn" :class="{ done: isRead(item.id, 'sbvAds') }" @click="markAsRead(item.id, 'sbvAds')">{{ isRead(item.id, 'sbvAds') ? '已读' : '标记为已读' }}</button>
                </div>
                <div class="template-focus-metric-grid">
                  <div v-for="card in getFocusMetricCardsFromAds(item.sbvAds)" :key="`${item.id}-sbv-${card.key}`" class="template-focus-metric-card">
                    <div class="template-focus-metric-title">{{ card.title }}</div>
                    <div class="template-focus-metric-main">{{ card.value }}</div>
                    <div class="template-focus-metric-body">
                      <div class="template-focus-metric-chart-area">
                        <div class="template-focus-metric-chart-box">
                          <span class="template-focus-metric-chart-baseline"></span>
                          <div class="template-focus-metric-chart-series">
                            <div v-for="row in card.rows" :key="`${item.id}-sbv-chart-${card.key}-${row.key}`" class="template-focus-metric-chart-column">
                              <span class="template-focus-metric-chart-bar" :style="{ height: `${row.barHeight}px` }"></span>
                            </div>
                          </div>
                          <svg class="template-focus-metric-chart-svg" viewBox="0 0 108 78" preserveAspectRatio="none" aria-hidden="true">
                            <path class="template-focus-metric-chart-line" :d="getFocusMetricChartPath(card.rows)" />
                          </svg>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="detail-table-tip">SBV 广告：按单产品广告表读取，剔除首行汇总后展示活动明细。</div>
                <div class="detail-table-scroll ads-table-scroll">
                  <div class="detail-data-table ads-activity-table-wrap">
                    <div class="detail-table-header">SBV广告活动明细</div>
                    <table class="ads-activity-table sticky-ads-table">
                      <thead>
                        <tr>
                          <th>广告活动名称</th>
                          <th>曝光量</th>
                          <th>点击量</th>
                          <th>点击率 (CTR)</th>
                          <th>单次点击成本 (CPC)</th>
                          <th>花费</th>
                          <th>总销售额</th>
                          <th>广告投入产出比</th>
                          <th>总订单数</th>
                          <th>转化率 (CVR)</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="activity in item.sbvAds.activityList" :key="`sbv-${activity.campaignName}`">
                          <td>
                            <a v-if="activity.campaignUrl" :href="activity.campaignUrl" target="_blank" rel="noopener noreferrer" class="campaign-link">{{ activity.campaignName }}</a>
                            <span v-else>{{ activity.campaignName }}</span>
                          </td>
                          <td>{{ activity.impressions }}</td>
                          <td>{{ activity.clicks }}</td>
                          <td>{{ activity.ctr }}</td>
                          <td>{{ activity.cpc }}</td>
                          <td>{{ activity.cost }}</td>
                          <td>{{ activity.sales }}</td>
                          <td>{{ activity.acos }}</td>
                          <td>{{ activity.orders }}</td>
                          <td>{{ activity.cvr }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </section>
          </div>

        </div>
      </article>
    </div>
  </div>

  <div v-else class="empty-tip">暂无运营数据。</div>
</template>
