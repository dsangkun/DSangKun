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
  type: 'today' | 'avg' | 'lastweek'
}

export interface ProductMetricBlock {
  title: string
  compareList: MetricCompareItem[]
}

export interface ProductOperationItem {
  id: string
  productName: string
  sales: ProductMetricBlock
  traffic: ProductMetricBlock
  ads: ProductMetricBlock
}
