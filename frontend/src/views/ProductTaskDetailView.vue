<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { dailyReportSheets } from '../constants/dailyReportSheets'

const route = useRoute()
const router = useRouter()

const productId = computed(() => String(route.params.id ?? ''))
const productName = computed(() => String(route.query.name ?? '未命名产品'))
const parentAsin = computed(() => String(route.query.asin ?? '--'))
const ownerName = computed(() => String(route.query.owner ?? '未分配'))
const childCount = computed(() => String(route.query.childCount ?? '--'))

const matchedSheet = computed(() => {
  return dailyReportSheets[productName.value] ?? null
})

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
</script>

<template>
  <div class="product-task-detail-page">
    <div class="placeholder-card product-task-detail-card data-sheet-page-card">
      <div class="placeholder-tag">数据页</div>
      <h2 class="placeholder-title">{{ productName }}</h2>
      <p class="placeholder-desc">
        当前数据页仅直接展示该父ASIN产品名对应的日报 Sheet，其他区域暂时留白。
      </p>

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
        <div v-if="matchedSheet" class="task-detail-meta-item">
          <span>Sheet 名</span>
          <strong>{{ matchedSheet.sheetName }}</strong>
        </div>
        <div v-if="matchedSheet" class="task-detail-meta-item">
          <span>来源文件</span>
          <strong>{{ matchedSheet.sourceFile }}</strong>
        </div>
      </div>

      <section class="daily-sheet-section">
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
          当前未找到与父ASIN产品名“{{ productName }}”完全匹配的 Sheet，数据页暂无法展示该日报内容。
        </div>
      </section>

      <div class="placeholder-actions">
        <button class="placeholder-back-btn" @click="router.back()">返回上一页</button>
      </div>
    </div>
  </div>
</template>
