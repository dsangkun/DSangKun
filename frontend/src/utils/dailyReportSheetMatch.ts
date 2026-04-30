import { dailyReportSheets, type DailyReportSheet } from '../constants/dailyReportSheets'

export type DailyReportSheetMatchCandidate = {
  sheetName: string
  score: number
  reasons: string[]
}

export type DailyReportSheetMatchResult = {
  sheet: DailyReportSheet | null
  matchedBy: string
  confidence: 'high' | 'medium' | 'low' | 'none'
  aliases: string[]
  candidates: DailyReportSheetMatchCandidate[]
}

type ResolveDailyReportSheetInput = {
  parentProductName: string
  childProductNames?: string[]
}

const normalizeLabel = (value: string) => {
  return String(value ?? '')
    .trim()
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, '')
}

const dedupeLabels = (values: string[]) => {
  const seen = new Set<string>()
  return values.filter((value) => {
    const normalized = normalizeLabel(value)
    if (!normalized || seen.has(normalized)) return false
    seen.add(normalized)
    return true
  })
}

const getSheetRowSubjects = (sheet: DailyReportSheet) => {
  const rawSet = new Set<string>()
  const normalizedSet = new Set<string>()

  sheet.rows.forEach((row) => {
    const subject = String(row?.[0] ?? '').trim()
    if (!subject) return
    rawSet.add(subject)
    normalizedSet.add(normalizeLabel(subject))
  })

  return {
    rawSet,
    normalizedSet,
    normalizedList: [...normalizedSet]
  }
}

const scoreSheet = (sheet: DailyReportSheet, aliases: string[]) => {
  const reasons = new Set<string>()
  let score = 0

  const sheetName = String(sheet.sheetName ?? '').trim()
  const normalizedSheetName = normalizeLabel(sheetName)
  const rowSubjects = getSheetRowSubjects(sheet)

  aliases.forEach((alias) => {
    const trimmedAlias = String(alias ?? '').trim()
    const normalizedAlias = normalizeLabel(trimmedAlias)
    if (!normalizedAlias) return

    if (trimmedAlias === sheetName) {
      score += 220
      reasons.add(`Sheet 名完全匹配：${trimmedAlias}`)
    }

    if (normalizedAlias === normalizedSheetName) {
      score += 180
      reasons.add(`Sheet 名归一化匹配：${trimmedAlias}`)
    }

    if (rowSubjects.rawSet.has(trimmedAlias)) {
      score += 160
      reasons.add(`Sheet 首列命中：${trimmedAlias}`)
    }

    if (rowSubjects.normalizedSet.has(normalizedAlias)) {
      score += 140
      reasons.add(`Sheet 首列归一化命中：${trimmedAlias}`)
    }

    if (normalizedSheetName && (normalizedSheetName.includes(normalizedAlias) || normalizedAlias.includes(normalizedSheetName))) {
      score += 90
      reasons.add(`Sheet 名包含关系：${trimmedAlias}`)
    }

    if (
      rowSubjects.normalizedList.some(
        (item) => item && (item.includes(normalizedAlias) || normalizedAlias.includes(item))
      )
    ) {
      score += 80
      reasons.add(`Sheet 首列包含关系：${trimmedAlias}`)
    }
  })

  return {
    sheetName,
    score,
    reasons: [...reasons]
  }
}

const getConfidence = (score: number): DailyReportSheetMatchResult['confidence'] => {
  if (score >= 180) return 'high'
  if (score >= 140) return 'medium'
  if (score >= 90) return 'low'
  return 'none'
}

export const resolveDailyReportSheetForParent = (
  input: ResolveDailyReportSheetInput
): DailyReportSheetMatchResult => {
  const aliases = dedupeLabels([input.parentProductName, ...(input.childProductNames ?? [])])

  const candidates = Object.values(dailyReportSheets)
    .map((sheet) => scoreSheet(sheet, aliases))
    .filter((item) => item.score > 0)
    .sort((a, b) => b.score - a.score)

  const bestCandidate = candidates[0]
  const secondCandidate = candidates[1]
  const confidence = getConfidence(bestCandidate?.score ?? 0)

  const isReliableMatch = Boolean(
    bestCandidate && (
      confidence === 'high' ||
      confidence === 'medium' ||
      ((bestCandidate.score ?? 0) >= 90 && ((bestCandidate.score ?? 0) - (secondCandidate?.score ?? 0)) >= 40)
    )
  )

  return {
    sheet: isReliableMatch && bestCandidate ? dailyReportSheets[bestCandidate.sheetName] ?? null : null,
    matchedBy: bestCandidate?.reasons[0] ?? '未匹配到可用 Sheet',
    confidence: isReliableMatch ? confidence : 'none',
    aliases,
    candidates: candidates.slice(0, 5)
  }
}

export const formatDailyReportMatchConfidence = (confidence: DailyReportSheetMatchResult['confidence']) => {
  if (confidence === 'high') return '高'
  if (confidence === 'medium') return '中'
  if (confidence === 'low') return '低'
  return '未匹配'
}
