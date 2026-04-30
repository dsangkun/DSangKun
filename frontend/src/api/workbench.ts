import type {
  ApiResponse,
  CompetitorChangeItem,
  NewArrivalActionType,
  NewArrivalItem,
  ProductOperationItem,
  WorkbenchOverview
} from '../types/workbench'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${url}`, {
    headers: {
      'Content-Type': 'application/json'
    },
    ...init
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }

  const result = (await response.json()) as ApiResponse<T>

  if (!result.success) {
    throw new Error(result.message || '接口请求失败')
  }

  return result.data
}

export function fetchWorkbenchOverview() {
  return request<WorkbenchOverview>('/api/workbench/overview')
}

export function fetchNewArrivals() {
  return request<NewArrivalItem[]>('/api/workbench/new-arrivals')
}

export function postNewArrivalAction(id: string, action: NewArrivalActionType) {
  return request<{ id: string; action: string; status: string }>(`/api/workbench/new-arrivals/${id}/action`, {
    method: 'POST',
    body: JSON.stringify({ action })
  })
}

export function fetchCompetitorChanges() {
  return request<CompetitorChangeItem[]>('/api/workbench/competitor-changes')
}

export function fetchOperationData(date?: string) {
  const query = date ? `?date=${encodeURIComponent(date)}` : ''
  return request<ProductOperationItem[]>(`/api/workbench/operation-data${query}`)
}

export function fetchOperationDates() {
  return request<string[]>('/api/workbench/operation-dates')
}

export type DailyReportSheet = {
  sheetName: string
  sourceFile: string
  reportDate: string
  rows: string[][]
}

export type DailyReportSheetCandidate = {
  sheetName: string
  score: number
  reasons: string[]
}

export type DailyReportProductSheetResponse = {
  parentAsin: string
  parentProductName: string
  reportDate: string
  matchedBy: string
  confidence: 'high' | 'medium' | 'low' | 'none'
  aliases: string[]
  sheet: DailyReportSheet | null
  candidates: DailyReportSheetCandidate[]
}

export function fetchLatestDailyReportSheet(params: {
  unionId: string
  parentAsin: string
  parentProductName: string
  childProductNames?: string[]
  reportDate?: string
}) {
  const query = new URLSearchParams({
    unionId: params.unionId,
    parentAsin: params.parentAsin,
    parentProductName: params.parentProductName
  })
  if (String(params.reportDate ?? '').trim()) {
    query.set('reportDate', String(params.reportDate).trim())
  }
  ;(params.childProductNames ?? []).forEach((name) => {
    if (String(name ?? '').trim()) {
      query.append('childProductNames', name)
    }
  })
  return request<DailyReportProductSheetResponse>(`/api/workbench/daily-report/latest-sheet?${query.toString()}`)
}

export function fetchDailyReportDates(unionId: string) {
  const query = new URLSearchParams({ unionId })
  return request<string[]>(`/api/workbench/daily-report/dates?${query.toString()}`)
}
