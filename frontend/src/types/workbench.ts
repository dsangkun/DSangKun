export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface WorkbenchOverview {
  totalTodoCount: number
  newArrivalCount: number
  competitorChangeCount: number
  operationProductCount: number
}

export type NewArrivalActionType = 'PUSH' | 'TRACK' | 'IGNORE'

export interface NewArrivalItem {
  id: string
  title: string
  time: string
  category: string
  shop: string
  snapshotUrl: string
}

export interface CompetitorChangeItem {
  id: string
  name: string
  shop: string
  rank: string
  changes: string[]
}

export interface MetricCompareItem {
  label: string
  value: string
  height: number
  type: 'today' | 'avg' | 'lastweek' | 'target'
}

export interface MetricHighlightItem {
  label: string
  value: string
  note?: string
  status?: 'good' | 'warn' | 'risk' | 'neutral'
}

export interface ProductMetricBlock {
  title: string
  compareList: MetricCompareItem[]
  highlights: MetricHighlightItem[]
  targetValue: string
  targetNote: string
}

export interface ProductReviewInfo {
  score: string
  reviewCount: string
  newReviewCount: string
  badReviewCount: string
  latestTitle: string
  latestContent: string
  latestDate: string
  latestAuthor: string
}

export interface ProductOperationItem {
  id: string
  productName: string
  productCode: string
  shopName: string
  siteName: string
  productTag: string
  coverText: string
  coverTone: 'blue' | 'green' | 'orange' | 'purple'
  childAsin: string
  childSku: string
  review: ProductReviewInfo
  sales: ProductMetricBlock
  traffic: ProductMetricBlock
  ads: ProductMetricBlock
}
