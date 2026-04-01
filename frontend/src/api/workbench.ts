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

export function fetchOperationData() {
  return request<ProductOperationItem[]>('/api/workbench/operation-data')
}
