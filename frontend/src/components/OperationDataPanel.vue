<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ProductMetricBlock, ProductOperationItem } from '../types/workbench'

type HealthLevel = 'good' | 'medium' | 'risk'

const props = defineProps<{
  items: ProductOperationItem[]
}>()

const activeReviewItem = ref<ProductOperationItem | null>(null)
const activeMetricDetail = ref<{
  productName: string
  section: ProductMetricBlock
} | null>(null)

const hasItems = computed(() => props.items.length > 0)

const getLevelWeight = (level: HealthLevel) => {
  if (level === 'risk') return 3
  if (level === 'medium') return 2
  return 1
}

const getCardLevel = (item: ProductOperationItem): HealthLevel => {
  const highlightStatuses = [...item.sales.highlights, ...item.traffic.highlights, ...item.ads.highlights].map(
    (highlight) => highlight.status ?? 'neutral'
  )

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

const openReviewDetail = (item: ProductOperationItem) => {
  activeReviewItem.value = item
}

const closeReviewDetail = () => {
  activeReviewItem.value = null
}

const openMetricDetail = (item: ProductOperationItem, section: ProductMetricBlock) => {
  activeMetricDetail.value = {
    productName: item.productName,
    section
  }
}

const closeMetricDetail = () => {
  activeMetricDetail.value = null
}
</script>

<template>
  <div v-if="hasItems" class="operation-showcase">
    <div class="status-guide">
      <span class="status-guide-item good"><i></i> 好：当前表现稳定，可常规关注</span>
      <span class="status-guide-item medium"><i></i> 中：有波动，建议优先看详情</span>
      <span class="status-guide-item risk"><i></i> 风险：存在明显异常，建议优先处理</span>
    </div>

    <div class="product-showcase-grid ultra-compact-grid">
      <article
        v-for="item in sortedItems"
        :key="item.id"
        class="operation-card ultra-compact-card"
        :class="`card-level-${item.healthLevel}`"
      >
        <div class="card-level-strip" :class="`strip-${item.healthLevel}`"></div>

        <section class="operation-card-basic image-first-card">
          <div class="product-top-tags top-tags-floating">
            <div class="health-badge" :class="`health-badge-${item.healthLevel}`">
              {{ item.healthLevel === 'risk' ? '风险' : item.healthLevel === 'medium' ? '中' : '好' }}
            </div>
            <div class="product-tag tiny-tag">{{ item.productTag }}</div>
          </div>

          <div class="product-image-frame">
            <div class="product-cover product-cover-large" :class="`product-cover-${item.coverTone}`">
              <span>{{ item.coverText }}</span>
            </div>
          </div>

          <div class="product-basic-content product-basic-centered">
            <div class="product-card-title tiny-title centered-title">{{ item.productName }}</div>
            <div class="product-card-subtitle tiny-subtitle centered-subtitle">{{ item.shopName }} · {{ item.siteName }}</div>
            <div class="product-basic-meta tiny-meta centered-meta">
              <span class="info-chip tiny-chip">子ASIN {{ item.childAsin }}</span>
            </div>
          </div>
        </section>

        <section class="info-card compact-section tiny-section">
          <div class="info-card-header compact-header tiny-header">
            <div>
              <div class="info-card-title tiny-section-title">评价信息</div>
              <div class="info-card-desc tiny-desc">卡片仅保留摘要，详细数据进入详情</div>
            </div>
            <button class="link-btn tiny-link" @click="openReviewDetail(item)">详情</button>
          </div>
          <div class="review-inline">
            <span class="review-pill">评分 {{ item.review.score }}</span>
            <span class="review-pill">评论 {{ item.review.reviewCount }}</span>
            <span class="review-pill risk">差评 {{ item.review.badReviewCount }}</span>
          </div>
          <div class="review-one-line">{{ item.review.latestTitle }}</div>
        </section>

        <section class="info-card compact-section tiny-section">
          <div class="info-card-header compact-header tiny-header">
            <div>
              <div class="info-card-title tiny-section-title">{{ item.sales.title }}</div>
              <div class="info-card-desc tiny-desc">卡片只保留对比柱图，详细字段点开查看</div>
            </div>
            <button class="link-btn tiny-link" @click="openMetricDetail(item, item.sales)">详情</button>
          </div>
          <div class="micro-legend">
            <span><i class="legend-dot today"></i>今</span>
            <span><i class="legend-dot avg"></i>均</span>
            <span><i class="legend-dot lastweek"></i>周</span>
            <span><i class="legend-dot target"></i>目</span>
          </div>
          <div class="micro-chart-wrap">
            <div class="micro-guide-line"></div>
            <div class="mini-bars micro-bars">
              <div v-for="bar in item.sales.compareList" :key="bar.label" class="mini-bar-group micro-bar-group">
                <div class="mini-bar micro-bar" :class="`bar-${bar.type}`" :style="{ height: `${bar.height}px` }"></div>
                <div class="mini-bar-label micro-bar-label">{{ bar.label }}</div>
              </div>
            </div>
          </div>
        </section>

        <section class="info-card compact-section tiny-section">
          <div class="info-card-header compact-header tiny-header">
            <div>
              <div class="info-card-title tiny-section-title">{{ item.traffic.title }}</div>
              <div class="info-card-desc tiny-desc">卡片只保留对比柱图，详细字段点开查看</div>
            </div>
            <button class="link-btn tiny-link" @click="openMetricDetail(item, item.traffic)">详情</button>
          </div>
          <div class="micro-legend">
            <span><i class="legend-dot today"></i>今</span>
            <span><i class="legend-dot avg"></i>均</span>
            <span><i class="legend-dot lastweek"></i>周</span>
            <span><i class="legend-dot target"></i>目</span>
          </div>
          <div class="micro-chart-wrap">
            <div class="micro-guide-line"></div>
            <div class="mini-bars micro-bars">
              <div v-for="bar in item.traffic.compareList" :key="bar.label" class="mini-bar-group micro-bar-group">
                <div class="mini-bar micro-bar" :class="`bar-${bar.type}`" :style="{ height: `${bar.height}px` }"></div>
                <div class="mini-bar-label micro-bar-label">{{ bar.label }}</div>
              </div>
            </div>
          </div>
        </section>

        <section class="info-card compact-section tiny-section">
          <div class="info-card-header compact-header tiny-header">
            <div>
              <div class="info-card-title tiny-section-title">{{ item.ads.title }}</div>
              <div class="info-card-desc tiny-desc">卡片只保留对比柱图，详细字段点开查看</div>
            </div>
            <button class="link-btn tiny-link" @click="openMetricDetail(item, item.ads)">详情</button>
          </div>
          <div class="micro-legend">
            <span><i class="legend-dot today"></i>今</span>
            <span><i class="legend-dot avg"></i>均</span>
            <span><i class="legend-dot lastweek"></i>周</span>
            <span><i class="legend-dot target"></i>目</span>
          </div>
          <div class="micro-chart-wrap">
            <div class="micro-guide-line"></div>
            <div class="mini-bars micro-bars">
              <div v-for="bar in item.ads.compareList" :key="bar.label" class="mini-bar-group micro-bar-group">
                <div class="mini-bar micro-bar" :class="`bar-${bar.type}`" :style="{ height: `${bar.height}px` }"></div>
                <div class="mini-bar-label micro-bar-label">{{ bar.label }}</div>
              </div>
            </div>
          </div>
        </section>
      </article>
    </div>

    <div v-if="activeReviewItem" class="overlay modal-overlay" @click.self="closeReviewDetail">
      <section class="metric-modal review-modal">
        <div class="drawer-header">
          <div>
            <div class="drawer-title">{{ activeReviewItem.productName }} · 评价详情</div>
            <div class="drawer-desc">卡片页只保留轻摘要，所有评价字段集中在详情弹窗查看。</div>
          </div>
          <button class="icon-btn" @click="closeReviewDetail">×</button>
        </div>
        <div class="metric-modal-content">
          <div class="detail-metrics-row review-detail-grid">
            <div class="summary-stat compact-stat">
              <div class="summary-label">评分</div>
              <div class="summary-value compact-value">{{ activeReviewItem.review.score }}</div>
            </div>
            <div class="summary-stat compact-stat">
              <div class="summary-label">评论总数</div>
              <div class="summary-value compact-value">{{ activeReviewItem.review.reviewCount }}</div>
            </div>
            <div class="summary-stat compact-stat">
              <div class="summary-label">新增评论</div>
              <div class="summary-value compact-value">{{ activeReviewItem.review.newReviewCount }}</div>
            </div>
            <div class="summary-stat compact-stat risk">
              <div class="summary-label">新增差评</div>
              <div class="summary-value compact-value">{{ activeReviewItem.review.badReviewCount }}</div>
            </div>
          </div>
          <div class="review-detail-card">
            <div class="review-preview-title">{{ activeReviewItem.review.latestTitle }}</div>
            <div class="review-preview-content">{{ activeReviewItem.review.latestContent }}</div>
            <div class="review-preview-meta">{{ activeReviewItem.review.latestAuthor }} · {{ activeReviewItem.review.latestDate }}</div>
          </div>
        </div>
      </section>
    </div>

    <div v-if="activeMetricDetail" class="overlay modal-overlay" @click.self="closeMetricDetail">
      <section class="metric-modal">
        <div class="drawer-header">
          <div>
            <div class="drawer-title">{{ activeMetricDetail.productName }} · {{ activeMetricDetail.section.title }}</div>
            <div class="drawer-desc">卡片页只保留图形，详细字段、目标值和备注都放在这里。</div>
          </div>
          <button class="icon-btn" @click="closeMetricDetail">×</button>
        </div>
        <div class="metric-modal-content">
          <div class="target-strip target-strip-large">目标：{{ activeMetricDetail.section.targetValue }} · {{ activeMetricDetail.section.targetNote }}</div>
          <div class="metric-summary-grid metric-summary-grid-modal">
            <div v-for="highlight in activeMetricDetail.section.highlights" :key="highlight.label" class="metric-summary-card" :class="highlight.status">
              <div class="summary-label">{{ highlight.label }}</div>
              <div class="summary-value">{{ highlight.value }}</div>
              <div class="summary-note">{{ highlight.note }}</div>
            </div>
          </div>
          <div class="detail-chart-card">
            <div class="detail-chart-title">当日 / 7日均值 / 上周同日 / 目标值</div>
            <div class="mini-bars large-bars">
              <div v-for="bar in activeMetricDetail.section.compareList" :key="bar.label" class="mini-bar-group">
                <div class="mini-bar" :class="`bar-${bar.type}`" :style="{ height: `${Math.max(bar.height + 18, 70)}px` }">{{ bar.value }}</div>
                <div class="mini-bar-label">{{ bar.label }}</div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>

  <div v-else class="empty-tip">暂无运营数据。</div>
</template>
