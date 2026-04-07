<script setup lang="ts">
import { computed, reactive } from 'vue'
import type { ProductMetricBlock, ProductOperationItem } from '../types/workbench'

type HealthLevel = 'good' | 'medium' | 'risk'
type ExpandSectionKey = 'review' | 'sales' | 'traffic' | 'spAds' | 'sbvAds'

const props = defineProps<{
  items: ProductOperationItem[]
}>()

const expandedSections = reactive<Record<string, Partial<Record<ExpandSectionKey, boolean>>>>({})

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
          </div>

          <div class="product-image-frame stacked-image-frame">
            <div class="product-cover product-cover-large" :class="`product-cover-${item.coverTone}`">
              <span>{{ item.coverText }}</span>
            </div>
          </div>

          <div class="product-basic-content stacked-main-content">
            <div class="stacked-title-block">
              <div class="product-card-title stacked-name">{{ item.productName }}</div>
              <div class="product-card-subtitle stacked-subtitle">{{ item.shopName }} · {{ item.siteName }}</div>
              <div class="product-card-subtitle stacked-subtitle">SKU {{ item.childSku }}</div>
            </div>

            <div class="expand-summary-chip">已展开 {{ getSectionCount(item.id) }} 项</div>

            <div class="section-toggle-list compact-section-list">
              <section class="inline-section-card">
                <button class="section-toggle-btn compact-toggle-btn" @click="toggleSection(item.id, 'review')">
                  <div class="section-toggle-main">
                    <div class="section-toggle-title">{{ getSectionTitle('review') }}</div>
                    <div class="section-toggle-desc horizontal-desc">
                      评分 {{ item.review.score }} / 评论 {{ item.review.reviewCount }} / 差评 {{ item.review.badReviewCount }}
                    </div>
                  </div>
                  <div class="section-toggle-right compact-toggle-right">
                    <span class="section-toggle-arrow" :class="{ open: isExpanded(item.id, 'review') }">⌄</span>
                  </div>
                </button>

                <div v-if="isExpanded(item.id, 'review')" class="section-expand-panel">
                  <div class="detail-data-table review-data-table">
                    <div class="detail-table-header">评价明细</div>
                    <div class="detail-table-row header-row">
                      <div>字段</div>
                      <div>数值</div>
                    </div>
                    <div class="detail-table-row"><div>评分</div><div>{{ item.review.score }}</div></div>
                    <div class="detail-table-row"><div>评论总数</div><div>{{ item.review.reviewCount }}</div></div>
                    <div class="detail-table-row"><div>新增评论</div><div>{{ item.review.newReviewCount }}</div></div>
                    <div class="detail-table-row"><div>新增差评</div><div>{{ item.review.badReviewCount }}</div></div>
                    <div class="detail-table-row multi-line-row"><div>最新评论标题</div><div>{{ item.review.latestTitle }}</div></div>
                    <div class="detail-table-row multi-line-row"><div>最新评论内容</div><div>{{ item.review.latestContent }}</div></div>
                    <div class="detail-table-row"><div>评论时间</div><div>{{ item.review.latestDate }}</div></div>
                    <div class="detail-table-row"><div>评论作者</div><div>{{ item.review.latestAuthor }}</div></div>
                  </div>
                </div>
              </section>

              <section class="inline-section-card metric-section-card">
                <button class="section-toggle-btn compact-toggle-btn" @click="toggleSection(item.id, 'sales')">
                  <div class="section-toggle-main">
                    <div class="section-toggle-title">{{ item.sales.title }}</div>
                    <div class="section-toggle-desc horizontal-desc">{{ getMetricPreview(item.sales) }}</div>
                  </div>
                  <div class="section-toggle-right compact-toggle-right">
                    <span class="section-toggle-arrow" :class="{ open: isExpanded(item.id, 'sales') }">⌄</span>
                  </div>
                </button>
                <div class="outer-chart-panel">
                  <div class="outer-chart-title">柱状图展示</div>
                  <div class="outer-chart-subtitle">当日 / 7日均值 / 上周同日 / 目标值</div>
                  <div class="mini-bars dense-bars outer-dense-bars">
                    <div v-for="bar in item.sales.compareList" :key="bar.label" class="mini-bar-group dense-bar-group">
                      <div class="mini-bar dense-bar" :class="`bar-${bar.type}`" :style="{ height: `${Math.max(bar.height + 34, 92)}px` }">
                        <span class="dense-bar-value">{{ bar.value }}</span>
                      </div>
                      <div class="mini-bar-label dense-bar-label">{{ bar.label }}</div>
                    </div>
                  </div>
                </div>
                <div v-if="isExpanded(item.id, 'sales')" class="section-expand-panel">
                  <div class="detail-data-table metric-data-table">
                    <div class="detail-table-header">销售数据明细</div>
                    <div class="detail-table-row header-row metric-header-row"><div>指标</div><div>数值</div><div>说明</div></div>
                    <div v-for="highlight in item.sales.highlights" :key="highlight.label" class="detail-table-row metric-row">
                      <div>{{ highlight.label }}</div><div>{{ highlight.value }}</div><div>{{ highlight.note || '-' }}</div>
                    </div>
                    <div class="detail-table-row metric-row target-row"><div>目标值</div><div>{{ item.sales.targetValue }}</div><div>{{ item.sales.targetNote }}</div></div>
                  </div>
                </div>
              </section>

              <section class="inline-section-card metric-section-card">
                <button class="section-toggle-btn compact-toggle-btn" @click="toggleSection(item.id, 'traffic')">
                  <div class="section-toggle-main">
                    <div class="section-toggle-title">{{ item.traffic.title }}</div>
                    <div class="section-toggle-desc horizontal-desc">{{ getMetricPreview(item.traffic) }}</div>
                  </div>
                  <div class="section-toggle-right compact-toggle-right">
                    <span class="section-toggle-arrow" :class="{ open: isExpanded(item.id, 'traffic') }">⌄</span>
                  </div>
                </button>
                <div class="outer-chart-panel">
                  <div class="outer-chart-title">柱状图展示</div>
                  <div class="outer-chart-subtitle">当日 / 7日均值 / 上周同日 / 目标值</div>
                  <div class="mini-bars dense-bars outer-dense-bars">
                    <div v-for="bar in item.traffic.compareList" :key="bar.label" class="mini-bar-group dense-bar-group">
                      <div class="mini-bar dense-bar" :class="`bar-${bar.type}`" :style="{ height: `${Math.max(bar.height + 34, 92)}px` }">
                        <span class="dense-bar-value">{{ bar.value }}</span>
                      </div>
                      <div class="mini-bar-label dense-bar-label">{{ bar.label }}</div>
                    </div>
                  </div>
                </div>
                <div v-if="isExpanded(item.id, 'traffic')" class="section-expand-panel">
                  <div class="detail-data-table metric-data-table">
                    <div class="detail-table-header">流量数据明细</div>
                    <div class="detail-table-row header-row metric-header-row"><div>指标</div><div>数值</div><div>说明</div></div>
                    <div v-for="highlight in item.traffic.highlights" :key="highlight.label" class="detail-table-row metric-row">
                      <div>{{ highlight.label }}</div><div>{{ highlight.value }}</div><div>{{ highlight.note || '-' }}</div>
                    </div>
                    <div class="detail-table-row metric-row target-row"><div>目标值</div><div>{{ item.traffic.targetValue }}</div><div>{{ item.traffic.targetNote }}</div></div>
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
                    <span class="section-toggle-arrow" :class="{ open: isExpanded(item.id, 'spAds') }">⌄</span>
                  </div>
                </button>
                <div class="ads-summary-panel">
                  <div class="ads-summary-grid">
                    <div v-for="highlight in item.spAds.highlights" :key="highlight.label" class="ads-summary-item" :class="highlight.status">
                      <div class="ads-summary-label">{{ highlight.label }}</div>
                      <div class="ads-summary-value">{{ highlight.value }}</div>
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
                            <td>{{ activity.campaignName }}</td>
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

              <section class="inline-section-card ads-section-card">
                <button class="section-toggle-btn compact-toggle-btn" @click="toggleSection(item.id, 'sbvAds')">
                  <div class="section-toggle-main">
                    <div class="section-toggle-title">{{ getSectionTitle('sbvAds') }}</div>
                    <div class="section-toggle-desc horizontal-desc">{{ item.sbvAds.sourceNote }}</div>
                  </div>
                  <div class="section-toggle-right compact-toggle-right">
                    <span class="section-toggle-arrow" :class="{ open: isExpanded(item.id, 'sbvAds') }">⌄</span>
                  </div>
                </button>
                <div class="ads-summary-panel">
                  <div class="ads-summary-grid">
                    <div v-for="highlight in item.sbvAds.highlights" :key="highlight.label" class="ads-summary-item" :class="highlight.status">
                      <div class="ads-summary-label">{{ highlight.label }}</div>
                      <div class="ads-summary-value">{{ highlight.value }}</div>
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
                            <td>{{ activity.campaignName }}</td>
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
        </section>
      </article>
    </div>
  </div>

  <div v-else class="empty-tip">暂无运营数据。</div>
</template>
