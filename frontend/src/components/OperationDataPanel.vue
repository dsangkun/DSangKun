<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { ParentOperationCard, ProductAdsBlock, ProductMetricBlock } from '../types/workbench'

type HealthLevel = 'good' | 'medium' | 'risk'
type ExpandSectionKey = 'review' | 'sales' | 'traffic' | 'spAds' | 'sbvAds'
type TaskLevel = 'warn' | 'risk'

type RuleTask = {
  id: string
  title: string
  level: TaskLevel
  trigger: string
  summary: string
  evidence: string[]
  action: string
  sourceChildAsin?: string
  sourceChildName?: string
}

const props = defineProps<{
  items: ParentOperationCard[]
}>()

const emit = defineEmits<{
  progressChange: [payload: { total: number, read: number }]
}>()

const router = useRouter()
const expandedSections = reactive<Record<string, Partial<Record<ExpandSectionKey, boolean>>>>({})
const readSections = reactive<Record<string, Partial<Record<ExpandSectionKey, boolean>>>>({})

const hasItems = computed(() => props.items.length > 0)
const sectionProgressKeys: ExpandSectionKey[] = ['review', 'sales', 'traffic', 'spAds', 'sbvAds']

const getLevelWeight = (level: HealthLevel) => {
  if (level === 'risk') return 3
  if (level === 'medium') return 2
  return 1
}

const parseNumber = (value: string | number | undefined | null) => {
  const parsed = Number(String(value ?? '').replace(/[^0-9.-]/g, ''))
  return Number.isFinite(parsed) ? parsed : null
}

const getItemSectionTasks = (item: ParentOperationCard, section: ExpandSectionKey): RuleTask[] => {
  const tasks = item.childItems.flatMap((child) => buildSectionTasksFromChild(item, child, section))
  return tasks.sort((a, b) => (a.level === 'risk' && b.level !== 'risk' ? -1 : a.level !== 'risk' && b.level === 'risk' ? 1 : 0))
}

const hasChildData = (item: ParentOperationCard) => item.childItems.length > 0

const getCardLevel = (item: ParentOperationCard): HealthLevel => {
  const hasRisk = sectionProgressKeys.some((section) => getItemSectionTasks(item, section).some((task) => task.level === 'risk'))
  if (hasRisk) return 'risk'

  const hasWarn = sectionProgressKeys.some((section) => getItemSectionTasks(item, section).length > 0)
  if (hasWarn) return 'medium'

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

const emitProgressChange = () => {
  const total = props.items.length * sectionProgressKeys.length
  const read = props.items.reduce((count, item) => {
    return count + sectionProgressKeys.filter((section) => Boolean(readSections[item.id]?.[section])).length
  }, 0)

  emit('progressChange', { total, read })
}

const isExpanded = (itemId: string, section: ExpandSectionKey) => Boolean(expandedSections[itemId]?.[section])

const toggleSection = (itemId: string, section: ExpandSectionKey) => {
  if (!expandedSections[itemId]) expandedSections[itemId] = {}
  expandedSections[itemId][section] = !expandedSections[itemId][section]
}

const isRead = (itemId: string, section: ExpandSectionKey) => Boolean(readSections[itemId]?.[section])

const markAsRead = (itemId: string, section: ExpandSectionKey) => {
  if (!readSections[itemId]) readSections[itemId] = {}
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

const getRankLabel = (index: number) => `#${index + 1}`

const getSectionTitle = (section: ExpandSectionKey) => {
  const titleMap: Record<ExpandSectionKey, string> = {
    review: '评价异常任务',
    sales: '销售异常任务',
    traffic: '流量异常任务',
    spAds: 'SP广告异常任务',
    sbvAds: 'SBV广告异常任务'
  }
  return titleMap[section]
}

const shortenText = (text: string, max = 26) => {
  const source = String(text ?? '').trim()
  if (!source) return '--'
  return source.length > max ? `${source.slice(0, max)}...` : source
}

const getSequenceNumber = (itemId: string) => {
  const parsed = Number(String(itemId).replace(/\D/g, ''))
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1
}

const getCategoryRows = (item: ParentOperationCard) => {
  const seq = getSequenceNumber(item.id)
  return [
    { rank: `#${2700 + seq * 19}`, text: 'in Parent ASIN Group' },
    { rank: `#${Math.max(1, seq)}`, text: `${item.childItems.length} 个子ASIN` }
  ]
}

const getTaskLevelLabel = (level: TaskLevel) => level === 'risk' ? '高优先级' : '处理中'
const getTaskLevelClass = (level: TaskLevel) => level === 'risk' ? 'risk' : 'warn'

const buildCompareEvidence = (section: ProductMetricBlock) => {
  const compareMap = new Map(section.compareList.map((metric) => [metric.type, metric.value]))
  return `今日 ${compareMap.get('today') ?? '--'} / 近均 ${compareMap.get('avg') ?? '--'} / 上周 ${compareMap.get('lastweek') ?? '--'} / 目标 ${compareMap.get('target') ?? '--'}`
}

const buildMetricTaskAction = (sectionKey: ExpandSectionKey, label: string) => {
  if (sectionKey === 'sales') return `优先检查 ${label} 波动原因，联动流量、广告和竞品变化做归因。`
  if (sectionKey === 'traffic') return `优先确认 ${label} 变化来源，区分自然流量、广告流量和转化承接问题。`
  return `优先检查 ${label} 对整体投放效果的影响，并记录后续处理动作。`
}

const buildMetricTasks = (parent: ParentOperationCard, child: ParentOperationCard['childItems'][number], sectionKey: ExpandSectionKey, section: ProductMetricBlock): RuleTask[] => {
  return section.highlights
    .filter((highlight) => highlight.status === 'warn' || highlight.status === 'risk')
    .map((highlight, index) => ({
      id: `${parent.id}-${child.childAsin}-${sectionKey}-${index}`,
      title: `${highlight.label}异常波动`,
      level: (highlight.status === 'risk' ? 'risk' : 'warn') as TaskLevel,
      trigger: '规则触发',
      summary: `子ASIN「${child.productName}」在${section.title}中「${highlight.label}」当前为 ${highlight.value}，${highlight.note ?? '出现异常变化'}。`,
      evidence: [
        `来源子ASIN：${child.childAsin}`,
        buildCompareEvidence(section),
        highlight.note ? `触发说明：${highlight.note}` : '触发说明：当前指标相对基线出现异常波动。'
      ],
      action: buildMetricTaskAction(sectionKey, highlight.label),
      sourceChildAsin: child.childAsin,
      sourceChildName: child.productName
    }))
}

const buildAdsTasks = (parent: ParentOperationCard, child: ParentOperationCard['childItems'][number], sectionKey: ExpandSectionKey, section: ProductAdsBlock): RuleTask[] => {
  const tasksFromHighlights = section.highlights
    .filter((highlight) => highlight.status === 'warn' || highlight.status === 'risk')
    .map((highlight, index) => ({
      id: `${parent.id}-${child.childAsin}-${sectionKey}-highlight-${index}`,
      title: `${highlight.label}投放异常`,
      level: (highlight.status === 'risk' ? 'risk' : 'warn') as TaskLevel,
      trigger: '规则触发',
      summary: `子ASIN「${child.productName}」在${section.title}中「${highlight.label}」当前为 ${highlight.value}，${highlight.note ?? '出现异常变化'}。`,
      evidence: [
        `来源子ASIN：${child.childAsin}`,
        `来源说明：${section.sourceNote}`,
        highlight.note ? `触发说明：${highlight.note}` : '触发说明：当前广告指标相对基线出现异常波动。'
      ],
      action: buildMetricTaskAction(sectionKey, highlight.label),
      sourceChildAsin: child.childAsin,
      sourceChildName: child.productName
    }))

  const zeroSalesActivities = section.activityList
    .filter((activity) => {
      const cost = parseNumber(activity.cost)
      const sales = parseNumber(activity.sales)
      return (cost ?? 0) > 0 && (sales ?? 0) <= 0
    })
    .slice(0, 2)
    .map((activity, index) => ({
      id: `${parent.id}-${child.childAsin}-${sectionKey}-activity-${index}`,
      title: '广告活动无转化',
      level: 'risk' as TaskLevel,
      trigger: '规则触发',
      summary: `子ASIN「${child.productName}」的广告活动「${activity.campaignName}」存在有花费无销售。`,
      evidence: [
        `来源子ASIN：${child.childAsin}`,
        `花费 ${activity.cost} / 销售额 ${activity.sales} / 点击 ${activity.clicks} / CVR ${activity.cvr}`,
        `活动来源：${section.title}`
      ],
      action: '优先核查关键词、落地页承接和出价策略，决定暂停、降价或继续观察。',
      sourceChildAsin: child.childAsin,
      sourceChildName: child.productName
    }))

  return [...tasksFromHighlights, ...zeroSalesActivities]
}

const buildReviewTasks = (parent: ParentOperationCard, child: ParentOperationCard['childItems'][number]): RuleTask[] => {
  const tasks: RuleTask[] = []
  const badReviewCount = parseNumber(child.review.badReviewCount) ?? 0
  const reviewScore = parseNumber(child.review.score) ?? 5

  if (badReviewCount > 0) {
    tasks.push({
      id: `${parent.id}-${child.childAsin}-review-bad`,
      title: '差评风险跟进',
      level: badReviewCount >= 3 ? 'risk' : 'warn',
      trigger: '规则触发',
      summary: `子ASIN「${child.productName}」当前新增/待关注差评 ${badReviewCount} 条，需要分析评价内容与销量、转化是否联动受影响。`,
      evidence: [
        `来源子ASIN：${child.childAsin}`,
        `评分 ${child.review.score} / 评论数 ${child.review.reviewCount} / 差评数 ${child.review.badReviewCount}`,
        `最近评价：${shortenText(child.review.latestTitle, 20)}｜${shortenText(child.review.latestContent, 36)}`
      ],
      action: '优先查看最新差评内容，判断是否涉及质量、安装、物流或描述不符，并记录处理建议。',
      sourceChildAsin: child.childAsin,
      sourceChildName: child.productName
    })
  }

  if (reviewScore < 4.5) {
    tasks.push({
      id: `${parent.id}-${child.childAsin}-review-score`,
      title: '评分下探预警',
      level: 'warn',
      trigger: '规则触发',
      summary: `子ASIN「${child.productName}」当前评分 ${child.review.score}，低于稳定区间，需要观察是否继续下探。`,
      evidence: [
        `来源子ASIN：${child.childAsin}`,
        `评分 ${child.review.score} / 评论数 ${child.review.reviewCount}`,
        `最近评价时间：${child.review.latestDate}`
      ],
      action: '结合近期新增评价与产品变动，确认评分下探是否会影响转化和广告表现。',
      sourceChildAsin: child.childAsin,
      sourceChildName: child.productName
    })
  }

  return tasks
}

const buildSectionTasksFromChild = (
  parent: ParentOperationCard,
  child: ParentOperationCard['childItems'][number],
  section: ExpandSectionKey
): RuleTask[] => {
  if (section === 'review') return buildReviewTasks(parent, child)
  if (section === 'sales') return buildMetricTasks(parent, child, section, child.sales)
  if (section === 'traffic') return buildMetricTasks(parent, child, section, child.traffic)
  if (section === 'spAds') return buildAdsTasks(parent, child, section, child.spAds)
  return buildAdsTasks(parent, child, section, child.sbvAds)
}

const goToDataPage = (item: ParentOperationCard) => {
  void router.push({
    name: 'product-task-detail',
    params: { id: item.id },
    query: {
      name: item.parentProductName,
      asin: item.parentAsin,
      owner: item.ownerName || '未分配',
      childCount: String(item.childItems.length)
    }
  })
}
</script>

<template>
  <div v-if="hasItems" class="operation-showcase">
    <div class="status-guide">
      <span class="status-guide-item good"><i></i> 好：父ASIN下当前未出现明显异常</span>
      <span class="status-guide-item medium"><i></i> 中：父ASIN下已有子ASIN触发异常任务</span>
      <span class="status-guide-item risk"><i></i> 风险：父ASIN下存在高优先级异常任务，建议先处理</span>
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
              <img :src="item.productImageUrl" :alt="item.parentProductName" class="template-product-image" />
            </div>
            <div v-else class="template-image-box template-cover-box" :class="`product-cover-${item.coverTone}`">
              <span>{{ item.coverText }}</span>
            </div>
            <button class="template-detail-link" @click="goToDataPage(item)">进入数据页</button>
          </div>

          <div class="template-title-block">
            <div class="template-product-title">{{ item.parentProductName }}</div>
            <div class="template-rating-line">
              <span class="template-stars">★★★★★</span>
              <span class="template-rating-count">{{ item.childItems.length }} / {{ item.childAsins.length }} 个子ASIN已接入</span>
            </div>
            <div class="template-offer-line">父ASIN：{{ item.parentAsin }}</div>
          </div>

          <div class="template-asin-block">
            <div class="template-asin-line">
              <span>父ASIN: {{ item.parentAsin }}</span>
              <span class="template-icon-line">◔ ◌ ⌁</span>
            </div>
            <div class="template-brand-line">
              <span>品牌: <strong>{{ item.shopName }}</strong></span>
              <span class="template-add-badge">{{ item.hasSbv ? '含SBV' : '无SBV' }}</span>
            </div>
            <div class="template-brand-line">
              <span>归属人: <strong>{{ item.ownerName || '未分配' }}</strong></span>
              <span class="template-owner-tag">父ASIN卡片</span>
            </div>
          </div>

          <div class="template-rank-block">
            <div v-for="row in getCategoryRows(item)" :key="`${item.id}-${row.rank}-${row.text}`" class="template-rank-row">
              <span class="template-rank-no">{{ row.rank }}</span>
              <span class="template-rank-text">{{ row.text }}</span>
            </div>
            <div class="parent-children-preview">
              <span class="parent-children-label">子ASIN：</span>
              <span class="parent-children-text">{{ item.childProductNames.join('、') }}</span>
            </div>
          </div>

          <div class="template-accordion-list">
            <section v-for="section in sectionProgressKeys" :key="`${item.id}-${section}`" class="template-accordion-section">
              <button class="template-accordion-btn" :class="{ done: isRead(item.id, section) }" @click="toggleSection(item.id, section)">
                <span>{{ getSectionTitle(section) }}</span>
                <span class="template-accordion-meta">
                  <em>{{ getItemSectionTasks(item, section).length }} 条</em>
                  <span class="template-accordion-arrow" :class="{ open: isExpanded(item.id, section) }">⌄</span>
                </span>
              </button>
              <div v-if="isExpanded(item.id, section)" class="template-accordion-panel task-accordion-panel">
                <div class="template-inline-action task-panel-toolbar">
                  <div class="task-panel-tip">当前为父ASIN聚合任务区，任意子ASIN命中异常都会汇总到这里。</div>
                  <button class="section-read-btn template-read-btn" :class="{ done: isRead(item.id, section) }" @click="markAsRead(item.id, section)">
                    {{ isRead(item.id, section) ? '已读' : '标记为已读' }}
                  </button>
                </div>

                <div v-if="getItemSectionTasks(item, section).length" class="task-card-list">
                  <article
                    v-for="task in getItemSectionTasks(item, section)"
                    :key="task.id"
                    class="task-info-card"
                    :class="`task-level-${getTaskLevelClass(task.level)}`"
                  >
                    <div class="task-info-head">
                      <div>
                        <div class="task-info-title">{{ task.title }}</div>
                        <div class="task-info-trigger">{{ task.trigger }}</div>
                      </div>
                      <span class="task-info-level" :class="`task-level-${getTaskLevelClass(task.level)}`">{{ getTaskLevelLabel(task.level) }}</span>
                    </div>
                    <div class="task-source-chip">来源子ASIN：{{ task.sourceChildName }} / {{ task.sourceChildAsin }}</div>
                    <div class="task-info-summary">{{ task.summary }}</div>
                    <ul class="task-info-evidence">
                      <li v-for="(row, evidenceIndex) in task.evidence" :key="`${task.id}-${evidenceIndex}`">{{ row }}</li>
                    </ul>
                    <div class="task-info-action">
                      <span class="task-info-action-label">建议动作</span>
                      <span>{{ task.action }}</span>
                    </div>
                  </article>
                </div>

                <div v-else class="task-empty-state">
                  {{ hasChildData(item)
                    ? '当前父ASIN下未触发该类型异常任务，等待规则命中后再展示子ASIN来源的任务信息。'
                    : '当前父ASIN卡片已按映射表生成，但该父ASIN下暂无接入的子ASIN运营数据。' }}
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
