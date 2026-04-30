<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCurrentUser } from '../auth/session'
import {
  fetchDailyReportDates,
  fetchLatestDailyReportSheet,
  type DailyReportProductSheetResponse
} from '../api/workbench'
import { asinMapping } from '../constants/asinMapping'
import {
  formatDailyReportMatchConfidence,
  resolveDailyReportSheetForParent
} from '../utils/dailyReportSheetMatch'

const route = useRoute()
const router = useRouter()

const productId = computed(() => String(route.params.id ?? ''))
const productName = computed(() => String(route.query.name ?? '未命名产品'))
const parentAsin = computed(() => String(route.query.asin ?? '--'))
const ownerName = computed(() => String(route.query.owner ?? '未分配'))
const childCount = computed(() => String(route.query.childCount ?? '--'))

const mappingRows = computed(() => {
  return asinMapping.filter((item) => item.parentAsin === parentAsin.value)
})

const childProductNames = computed(() => {
  return mappingRows.value
    .map((item) => item.childProductName)
    .filter((item) => String(item ?? '').trim().length > 0)
})

const remoteSheetResponse = ref<DailyReportProductSheetResponse | null>(null)
const remoteError = ref('')
const loading = ref(false)
const reportDates = ref<string[]>([])
const selectedReportDate = ref('latest')
const datesLoading = ref(false)

const staticSheetMatchResult = computed(() => {
  return resolveDailyReportSheetForParent({
    parentProductName: productName.value,
    childProductNames: childProductNames.value
  })
})

const sheetMatchResult = computed(() => {
  if (remoteSheetResponse.value) {
    return {
      sheet: remoteSheetResponse.value.sheet,
      matchedBy: remoteSheetResponse.value.matchedBy,
      confidence: remoteSheetResponse.value.confidence,
      aliases: remoteSheetResponse.value.aliases,
      candidates: remoteSheetResponse.value.candidates
    }
  }
  return staticSheetMatchResult.value
})

const matchedSheet = computed(() => sheetMatchResult.value.sheet)
const matchConfidenceLabel = computed(() => formatDailyReportMatchConfidence(sheetMatchResult.value.confidence))
const currentUnionId = computed(() => String((getCurrentUser() as any)?.unionId ?? (getCurrentUser() as any)?.unionid ?? '').trim())
const effectiveReportDate = computed(() => selectedReportDate.value === 'latest' ? '' : selectedReportDate.value)

const normalizedRows = computed(() => {
  const rows = matchedSheet.value?.rows ?? []
  const maxCols = rows.reduce((max, row) => Math.max(max, row.length), 0)
  return rows.map((row) => {
    const filled = [...row]
    while (filled.length < maxCols) {
      filled.push('')
    }
    return filled
  })
})

const mergedFirstColumnRows = computed(() => {
  const rows = normalizedRows.value
  const hiddenFirstColumnKeys = new Set<string>()
  const rowspanMap = new Map<string, number>()

  let rowIndex = 0
  while (rowIndex < rows.length) {
    const firstCell = rows[rowIndex]?.[0] ?? ''
    if (!firstCell) {
      rowIndex += 1
      continue
    }

    let endIndex = rowIndex + 1
    while (endIndex < rows.length && (rows[endIndex]?.[0] ?? '') === firstCell) {
      hiddenFirstColumnKeys.add(`${endIndex}-0`)
      endIndex += 1
    }

    rowspanMap.set(`${rowIndex}-0`, endIndex - rowIndex)
    rowIndex = endIndex
  }

  return rows.map((row, rowIndex) => ({
    row,
    rowIndex,
    cells: row.map((cell, cellIndex) => ({
      cell,
      cellIndex,
      hidden: cellIndex === 0 ? hiddenFirstColumnKeys.has(`${rowIndex}-0`) : false,
      rowspan: cellIndex === 0 ? (rowspanMap.get(`${rowIndex}-0`) ?? 1) : 1
    }))
  }))
})

async function loadReportDates() {
  if (!currentUnionId.value) {
    return
  }
  datesLoading.value = true
  try {
    reportDates.value = await fetchDailyReportDates(currentUnionId.value)
  } catch {
    reportDates.value = []
  } finally {
    datesLoading.value = false
  }
}

async function loadRemoteSheet() {
  if (!currentUnionId.value || !parentAsin.value || !productName.value) {
    return
  }

  loading.value = true
  remoteError.value = ''
  try {
    remoteSheetResponse.value = await fetchLatestDailyReportSheet({
      unionId: currentUnionId.value,
      parentAsin: parentAsin.value,
      parentProductName: productName.value,
      childProductNames: childProductNames.value,
      reportDate: effectiveReportDate.value || undefined
    })
  } catch (error) {
    remoteError.value = error instanceof Error ? error.message : String(error)
  } finally {
    loading.value = false
  }
}

watch(selectedReportDate, async () => {
  await loadRemoteSheet()
})

onMounted(async () => {
  if (!currentUnionId.value || !parentAsin.value || !productName.value) {
    return
  }
  await loadReportDates()
  await loadRemoteSheet()
})
</script>

<template>
  <div class="product-task-detail-page">
    <div class="placeholder-card product-task-detail-card data-sheet-page-card">
      <div class="placeholder-tag">数据页</div>
      <h2 class="placeholder-title">{{ productName }}</h2>
      <p class="placeholder-desc">
        当前数据页支持按日报日期筛选，默认展示钉盘中的最新日报；若后端未命中，则回退到本地静态日报 Sheet。
      </p>

      <div class="task-detail-toolbar">
        <div class="task-detail-toolbar-item">
          <span>日报日期</span>
          <select v-model="selectedReportDate" class="task-detail-select" :disabled="datesLoading || loading">
            <option value="latest">最新日报</option>
            <option v-for="date in reportDates" :key="date" :value="date">{{ date }}</option>
          </select>
        </div>
        <div class="task-detail-toolbar-tip">
          <span v-if="datesLoading">正在读取可选日期...</span>
          <span v-else>可选日期 {{ reportDates.length }} 个</span>
        </div>
      </div>

      <div class="task-detail-meta-grid compact">
        <div class="task-detail-meta-item">
          <span>父卡片ID</span>
          <strong>{{ productId }}</strong>
        </div>
        <div class="task-detail-meta-item">
          <span>父ASIN</span>
          <strong>{{ parentAsin }}</strong>
        </div>
        <div class="task-detail-meta-item">
          <span>归属人</span>
          <strong>{{ ownerName }}</strong>
        </div>
        <div class="task-detail-meta-item">
          <span>子ASIN数量</span>
          <strong>{{ childCount }}</strong>
        </div>
        <div class="task-detail-meta-item">
          <span>匹配置信度</span>
          <strong>{{ matchConfidenceLabel }}</strong>
        </div>
        <div class="task-detail-meta-item">
          <span>匹配方式</span>
          <strong>{{ sheetMatchResult.matchedBy }}</strong>
        </div>
        <div v-if="matchedSheet" class="task-detail-meta-item">
          <span>Sheet 名</span>
          <strong>{{ matchedSheet.sheetName }}</strong>
        </div>
        <div v-if="matchedSheet" class="task-detail-meta-item">
          <span>来源文件</span>
          <strong>{{ matchedSheet.sourceFile }}</strong>
        </div>
        <div v-if="matchedSheet && 'reportDate' in matchedSheet" class="task-detail-meta-item">
          <span>日报日期</span>
          <strong>{{ (matchedSheet as any).reportDate || '--' }}</strong>
        </div>
      </div>

      <div v-if="loading" class="task-empty-state">
        <div>正在读取钉盘最新日报...</div>
      </div>
      <div v-else-if="remoteError" class="task-empty-state">
        <div>钉盘最新日报读取失败，已回退为本地静态数据。</div>
        <div class="sheet-match-debug-title">错误信息：{{ remoteError }}</div>
      </div>

      <section class="daily-sheet-section">
        <div v-if="sheetMatchResult.aliases.length" class="sheet-match-helper">
          <div class="sheet-match-helper-title">当前匹配别名</div>
          <div class="sheet-match-alias-list">
            <span v-for="alias in sheetMatchResult.aliases" :key="alias" class="sheet-match-chip">{{ alias }}</span>
          </div>
        </div>

        <div v-if="matchedSheet" class="daily-sheet-table-wrap">
          <table class="daily-sheet-table">
            <tbody>
              <tr v-for="item in mergedFirstColumnRows" :key="`row-${item.rowIndex}`">
                <template v-for="cell in item.cells" :key="`cell-${item.rowIndex}-${cell.cellIndex}`">
                  <td
                    v-if="!cell.hidden"
                    :rowspan="cell.rowspan"
                    :class="[
                      'daily-sheet-cell',
                      cell.cellIndex === 0 ? 'col-subject' : '',
                      cell.cellIndex === 1 ? 'col-metric' : '',
                      cell.cellIndex >= 2 ? 'col-value' : '',
                      item.rowIndex === 0 ? 'first-row' : ''
                    ]"
                  >
                    {{ cell.cell || ' ' }}
                  </td>
                </template>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else class="task-empty-state">
          <div>当前未匹配到可用 Sheet，数据页暂无法展示该日报内容。</div>
          <div class="sheet-match-debug-list" v-if="sheetMatchResult.candidates.length">
            <div class="sheet-match-debug-title">候选 Sheet（前 5 个）</div>
            <ul>
              <li v-for="candidate in sheetMatchResult.candidates" :key="candidate.sheetName">
                {{ candidate.sheetName }}（score={{ candidate.score }}；{{ candidate.reasons.join(' / ') || '无原因' }}）
              </li>
            </ul>
          </div>
        </div>
      </section>

      <div class="placeholder-actions">
        <button class="placeholder-back-btn" @click="router.back()">返回上一页</button>
      </div>
    </div>
  </div>
</template>
