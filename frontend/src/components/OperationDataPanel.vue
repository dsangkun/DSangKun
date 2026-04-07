<script setup lang="ts">
import { computed, reactive } from 'vue'
import type { MetricHighlightItem, ProductMetricBlock, ProductOperationItem } from '../types/workbench'

type HealthLevel = 'good' | 'medium' | 'risk'
type ExpandSectionKey = 'review' | 'sales' | 'traffic' | 'spAds' | 'sbvAds'

const props = defineProps<{
  items: ProductOperationItem[]
}>()

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
}

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

const getTrafficModules = (item: ProductOperationItem) => {
  const totalHighlights = item.traffic.highlights
  const factorMap: Record<string, number> = {
    总流量: 0.74,
    自然流量: 0.78,
    广告流量: 0.69,
    Listing转化: 0.9
  }

  const naturalHighlights = item.traffic.highlights.map((highlight) => ({
    ...highlight,
    value: scaleMetricValue(highlight.value, factorMap[highlight.label] ?? 0.76),
    note: highlight.label === '广告流量' ? '以自然承接为主' : highlight.note
  }))

  return [
    { key: 'total', title: '总数据', highlights: totalHighlights },
    { key: 'natural', title: '自然数据', highlights: naturalHighlights }
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

    <div class="product-showcase-grid single-column-grid">
      <article
        v-for="item in sortedItems"
        :key="item.id"
        class="operation-card expanded-operation-card vertical-flow-card"
        :class="`card-level-${item.healthLevel}`"
      >
        <div class="card-level-strip" :class="`strip-${item.healthLevel}`"></div>

        <section class="operation-card-basic top-down-basic-layout">
          <div class="product-top-tags top-tags-left wrapped-tags">
            <div class="health-badge" :class="`health-badge-${item.healthLevel}`">
              {{ item.healthLevel === 'risk' ? '风险' : item.healthLevel === 'medium' ? '中' : '好' }}
            </div>
            <div class="product-tag">{{ item.productTag }}</div>
            <div class="info-chip">子ASIN {{ item.childAsin }}</div>
            <div class="info-chip">SKU {{ item.childSku }}</div>
          </div>

          <div class="product-head-card listing-style-head-card vertical-listing-head-card">
            <div class="listing-image-wrap vertical-listing-image-wrap">
              <div v-if="item.productImageUrl" class="listing-image-real-wrap amazon-image-real-wrap">
                <img :src="item.productImageUrl" :alt="item.productName" class="listing-image" />
              </div>
              <div v-else class="product-cover listing-style-cover amazon-listing-style-cover" :class="`product-cover-${item.coverTone}`">
                <span>{{ item.coverText }}</span>
              </div>
            </div>

            <div class="product-basic-content stacked-main-content listing-meta-content vertical-listing-meta-content">
              <div class="stacked-title-block listing-title-block vertical-listing-title-block">
                <div class="product-card-title stacked-name listing-main-title amazon-listing-title">{{ item.listingTitle || item.productName }}</div>
                <div class="listing-price amazon-listing-price">{{ item.listingPrice || '--' }}</div>
                <div class="product-card-subtitle stacked-subtitle amazon-listing-subtitle">{{ item.productName }} · {{ item.shopName }} · {{ item.siteName }}</div>
              </div>

              <div class="expand-summary-chip">已展开 {{ getSectionCount(item.id) }} 项</div>
            </div>
          </div>

          <div class="section-toggle-list compact-section-list">
              <section class="inline-section-card">
                <button class="section-toggle-btn compact-toggle-btn" @click="toggleSection(item.id, 'review')">
                  <div class="section-toggle-main">
                    <div class="section-toggle-title">{{ getSectionTitle('review') }}</div>
                  </div>
                  <div class="section-toggle-right compact-toggle-right">
                    <span class="section-read-badge" :class="isRead(item.id, 'review') ? 'read' : 'unread'">{{ isRead(item.id, 'review') ? '已读' : '未读' }}</span>
                    <span class="section-toggle-arrow" :class="{ open: isExpanded(item.id, 'review') }">⌄</span>
                  </div>
                </button>

                <div v-if="isExpanded(item.id, 'review')" class="section-expand-panel review-section-expand-panel">
                  <div class="review-thread-panel">
                    <div class="review-thread-header">
                      <div class="review-thread-title">最新评论</div>
                    </div>

                    <div class="review-thread-list">
                      <article v-for="(comment, index) in (item.review.recentComments?.length ? item.review.recentComments : [{ author: item.review.latestAuthor, content: item.review.latestContent, date: item.review.latestDate }])" :key="`${item.id}-comment-${index}`" class="review-comment-card">
                        <div class="review-comment-top">
                          <div class="review-comment-author">{{ comment.author }}</div>
                          <div class="review-comment-date">{{ comment.date || item.review.latestDate }}</div>
                        </div>
                        <div class="review-comment-content">{{ comment.content }}</div>
                      </article>
                    </div>

                    <div class="section-read-action-row">
                      <button class="section-read-btn" :class="{ done: isRead(item.id, 'review') }" @click="markAsRead(item.id, 'review')">{{ isRead(item.id, 'review') ? '已读' : '标记为已读' }}</button>
                    </div>
                  </div>
                </div>
              </section>

              <section class="inline-section-card metric-section-card">
                <button class="section-toggle-btn compact-toggle-btn" @click="toggleSection(item.id, 'sales')">
                  <div class="section-toggle-main">
                    <div class="section-toggle-title">{{ item.sales.title }}</div>
                  </div>
                  <div class="section-toggle-right compact-toggle-right">
                    <span class="section-read-badge" :class="isRead(item.id, 'sales') ? 'read' : 'unread'">{{ isRead(item.id, 'sales') ? '已读' : '未读' }}</span>
                    <span class="section-toggle-arrow" :class="{ open: isExpanded(item.id, 'sales') }">⌄</span>
                  </div>
                </button>
                <div v-if="isExpanded(item.id, 'sales')" class="section-expand-panel">
                  <div class="sales-summary-panel expanded-metric-summary-panel vertical-sales-summary-panel">
                    <div v-for="module in getSalesModules(item)" :key="`${item.id}-${module.key}`" class="sales-module-block">
                      <div class="sales-module-title">{{ module.title }}</div>
                      <div class="ads-summary-grid sales-summary-grid vertical-sales-summary-grid">
                        <div v-for="highlight in module.highlights" :key="`${module.key}-${highlight.label}`" class="ads-summary-item ads-visual-summary-item sales-visual-summary-item" :class="highlight.status">
                          <div class="ads-summary-head">
                            <div class="ads-summary-label">{{ highlight.label }}</div>
                            <div class="ads-summary-value">{{ highlight.value }}</div>
                          </div>
                          <div class="ads-mini-chart refined-ads-mini-chart">
                            <div class="ads-mini-chart-bars refined-ads-mini-chart-bars">
                              <div v-for="row in getAdsSummaryRows(highlight)" :key="`${module.key}-${highlight.label}-${row.label}`" class="ads-mini-chart-row refined-ads-mini-chart-row">
                                <div class="ads-mini-chart-row-label">{{ row.label }}</div>
                                <div class="ads-mini-chart-track">
                                  <span class="ads-mini-chart-bar colorful-ads-mini-chart-bar" :style="{ width: `${row.width}%` }"></span>
                                </div>
                                <div class="ads-mini-chart-row-value">{{ row.value }}</div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="section-read-action-row metric-read-action-row">
                    <button class="section-read-btn" :class="{ done: isRead(item.id, 'sales') }" @click="markAsRead(item.id, 'sales')">{{ isRead(item.id, 'sales') ? '已读' : '标记为已读' }}</button>
                  </div>
                </div>
              </section>

              <section class="inline-section-card metric-section-card">
                <button class="section-toggle-btn compact-toggle-btn" @click="toggleSection(item.id, 'traffic')">
                  <div class="section-toggle-main">
                    <div class="section-toggle-title">{{ item.traffic.title }}</div>
                  </div>
                  <div class="section-toggle-right compact-toggle-right">
                    <span class="section-read-badge" :class="isRead(item.id, 'traffic') ? 'read' : 'unread'">{{ isRead(item.id, 'traffic') ? '已读' : '未读' }}</span>
                    <span class="section-toggle-arrow" :class="{ open: isExpanded(item.id, 'traffic') }">⌄</span>
                  </div>
                </button>
                <div v-if="isExpanded(item.id, 'traffic')" class="section-expand-panel">
                  <div class="sales-summary-panel expanded-metric-summary-panel vertical-sales-summary-panel">
                    <div v-for="module in getTrafficModules(item)" :key="`${item.id}-${module.key}`" class="sales-module-block">
                      <div class="sales-module-title">{{ module.title }}</div>
                      <div class="ads-summary-grid sales-summary-grid vertical-sales-summary-grid">
                        <div v-for="highlight in module.highlights" :key="`${module.key}-${highlight.label}`" class="ads-summary-item ads-visual-summary-item sales-visual-summary-item" :class="highlight.status">
                          <div class="ads-summary-head">
                            <div class="ads-summary-label">{{ highlight.label }}</div>
                            <div class="ads-summary-value">{{ highlight.value }}</div>
                          </div>
                          <div class="ads-mini-chart refined-ads-mini-chart">
                            <div class="ads-mini-chart-bars refined-ads-mini-chart-bars">
                              <div v-for="row in getAdsSummaryRows(highlight)" :key="`${module.key}-${highlight.label}-${row.label}`" class="ads-mini-chart-row refined-ads-mini-chart-row">
                                <div class="ads-mini-chart-row-label">{{ row.label }}</div>
                                <div class="ads-mini-chart-track">
                                  <span class="ads-mini-chart-bar colorful-ads-mini-chart-bar" :style="{ width: `${row.width}%` }"></span>
                                </div>
                                <div class="ads-mini-chart-row-value">{{ row.value }}</div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="section-read-action-row metric-read-action-row">
                    <button class="section-read-btn" :class="{ done: isRead(item.id, 'traffic') }" @click="markAsRead(item.id, 'traffic')">{{ isRead(item.id, 'traffic') ? '已读' : '标记为已读' }}</button>
                  </div>
                </div>
              </section>

              <section class="inline-section-card ads-section-card">
                <button class="section-toggle-btn compact-toggle-btn" @click="toggleSection(item.id, 'spAds')">
                  <div class="section-toggle-main">
                    <div class="section-toggle-title">{{ getSectionTitle('spAds') }}</div>
                    <div class="section-toggle-desc horizontal-desc">{{ item.spAds.sourceNote }}</div>
                  </div>
                  <div class="section-toggle-right compact-toggle-right">
                    <span class="section-read-badge" :class="isRead(item.id, 'spAds') ? 'read' : 'unread'">{{ isRead(item.id, 'spAds') ? '已读' : '未读' }}</span>
                    <span class="section-toggle-arrow" :class="{ open: isExpanded(item.id, 'spAds') }">⌄</span>
                  </div>
                </button>
                <div class="ads-summary-panel">
                  <div class="ads-summary-grid">
                    <div v-for="highlight in item.spAds.highlights" :key="highlight.label" class="ads-summary-item ads-visual-summary-item" :class="highlight.status">
                      <div class="ads-summary-head">
                        <div class="ads-summary-label">{{ highlight.label }}</div>
                        <div class="ads-summary-value">{{ highlight.value }}</div>
                      </div>
                      <div class="ads-mini-chart refined-ads-mini-chart">
                        <div class="ads-mini-chart-bars refined-ads-mini-chart-bars">
                          <div v-for="row in getAdsSummaryRows(highlight)" :key="`${highlight.label}-${row.label}`" class="ads-mini-chart-row refined-ads-mini-chart-row">
                            <div class="ads-mini-chart-row-label">{{ row.label }}</div>
                            <div class="ads-mini-chart-track">
                              <span class="ads-mini-chart-bar colorful-ads-mini-chart-bar" :style="{ width: `${row.width}%` }"></span>
                            </div>
                            <div class="ads-mini-chart-row-value">{{ row.value }}</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="isExpanded(item.id, 'spAds')" class="section-expand-panel">
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
                  <div class="section-read-action-row">
                    <button class="section-read-btn" :class="{ done: isRead(item.id, 'spAds') }" @click="markAsRead(item.id, 'spAds')">{{ isRead(item.id, 'spAds') ? '已读' : '标记为已读' }}</button>
                  </div>
                </div>
              </section>

              <section class="inline-section-card ads-section-card">
                <button class="section-toggle-btn compact-toggle-btn" @click="toggleSection(item.id, 'sbvAds')">
                  <div class="section-toggle-main">
                    <div class="section-toggle-title">{{ getSectionTitle('sbvAds') }}</div>
                    <div class="section-toggle-desc horizontal-desc">{{ item.sbvAds.sourceNote }}</div>
                  </div>
                  <div class="section-toggle-right compact-toggle-right">
                    <span class="section-read-badge" :class="isRead(item.id, 'sbvAds') ? 'read' : 'unread'">{{ isRead(item.id, 'sbvAds') ? '已读' : '未读' }}</span>
                    <span class="section-toggle-arrow" :class="{ open: isExpanded(item.id, 'sbvAds') }">⌄</span>
                  </div>
                </button>
                <div class="ads-summary-panel">
                  <div class="ads-summary-grid">
                    <div v-for="highlight in item.sbvAds.highlights" :key="highlight.label" class="ads-summary-item ads-visual-summary-item" :class="highlight.status">
                      <div class="ads-summary-head">
                        <div class="ads-summary-label">{{ highlight.label }}</div>
                        <div class="ads-summary-value">{{ highlight.value }}</div>
                      </div>
                      <div class="ads-mini-chart refined-ads-mini-chart">
                        <div class="ads-mini-chart-bars refined-ads-mini-chart-bars">
                          <div v-for="row in getAdsSummaryRows(highlight)" :key="`${highlight.label}-${row.label}`" class="ads-mini-chart-row refined-ads-mini-chart-row">
                            <div class="ads-mini-chart-row-label">{{ row.label }}</div>
                            <div class="ads-mini-chart-track">
                              <span class="ads-mini-chart-bar colorful-ads-mini-chart-bar" :style="{ width: `${row.width}%` }"></span>
                            </div>
                            <div class="ads-mini-chart-row-value">{{ row.value }}</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="isExpanded(item.id, 'sbvAds')" class="section-expand-panel">
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
                  <div class="section-read-action-row">
                    <button class="section-read-btn" :class="{ done: isRead(item.id, 'sbvAds') }" @click="markAsRead(item.id, 'sbvAds')">{{ isRead(item.id, 'sbvAds') ? '已读' : '标记为已读' }}</button>
                  </div>
                </div>
              </section>
            </div>
        </section>
      </article>
    </div>
  </div>

  <div v-else class="empty-tip">暂无运营数据。</div>
</template>
