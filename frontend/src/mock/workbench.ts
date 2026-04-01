import type { CompetitorChangeItem, NewArrivalItem, ProductOperationItem } from '../types/workbench'

export const newArrivalMock: NewArrivalItem[] = [
  {
    id: 'A001',
    title: '竞品 A 新上架：便携咖啡机 Pro',
    time: '09:12',
    category: '厨房小家电',
    shop: '竞品旗舰店A',
    snapshotUrl: 'https://snapshot.example.com/product/A001'
  },
  {
    id: 'B014',
    title: '竞品 B 新上架：无叶挂脖风扇 Lite',
    time: '10:03',
    category: '季节电器',
    shop: '竞品旗舰店B',
    snapshotUrl: 'https://snapshot.example.com/product/B014'
  },
  {
    id: 'C102',
    title: '竞品 C 新上架：桌面空气循环扇 Mini',
    time: '10:48',
    category: '家居电器',
    shop: '竞品旗舰店C',
    snapshotUrl: 'https://snapshot.example.com/product/C102'
  }
]

export const competitorChangeMock: CompetitorChangeItem[] = [
  {
    id: 'CX9',
    name: '咖啡机旗舰款 X9',
    shop: '竞品旗舰店A',
    rank: '12',
    changes: ['价格：¥399 → ¥369', '活动：新增满300减30', '销量：日增 +126']
  },
  {
    id: 'ACS',
    name: '挂脖风扇 AirCool S',
    shop: '竞品旗舰店B',
    rank: '28',
    changes: ['主图：已更新', '文案：卖点描述调整', '销量：日增 +84']
  },
  {
    id: 'PMX',
    name: '空气循环扇 Pro Max',
    shop: '竞品旗舰店C',
    rank: '7',
    changes: ['价格：¥259 → ¥279', '活动：取消店铺券', '排名：11 → 7']
  }
]

export const operationDataMock: ProductOperationItem[] = [
  {
    id: 'K1',
    productName: '智能咖啡机 K1',
    sales: {
      title: '销售数据',
      compareList: [
        { label: '当日数据', value: '860', height: 138, type: 'today' },
        { label: '七日平均', value: '695', height: 112, type: 'avg' },
        { label: '上周同日', value: '780', height: 124, type: 'lastweek' }
      ]
    },
    traffic: {
      title: '流量数据',
      compareList: [
        { label: '当日数据', value: '12k', height: 146, type: 'today' },
        { label: '七日平均', value: '9.7k', height: 118, type: 'avg' },
        { label: '上周同日', value: '10.8k', height: 130, type: 'lastweek' }
      ]
    },
    ads: {
      title: '广告数据',
      compareList: [
        { label: '当日数据', value: '3.4k', height: 120, type: 'today' },
        { label: '七日平均', value: '2.8k', height: 96, type: 'avg' },
        { label: '上周同日', value: '3.0k', height: 106, type: 'lastweek' }
      ]
    }
  },
  {
    id: 'F2',
    productName: '便携风扇 F2',
    sales: {
      title: '销售数据',
      compareList: [
        { label: '当日数据', value: '430', height: 104, type: 'today' },
        { label: '七日平均', value: '388', height: 94, type: 'avg' },
        { label: '上周同日', value: '462', height: 112, type: 'lastweek' }
      ]
    },
    traffic: {
      title: '流量数据',
      compareList: [
        { label: '当日数据', value: '8.6k', height: 124, type: 'today' },
        { label: '七日平均', value: '7.8k', height: 112, type: 'avg' },
        { label: '上周同日', value: '9.2k', height: 132, type: 'lastweek' }
      ]
    },
    ads: {
      title: '广告数据',
      compareList: [
        { label: '当日数据', value: '2.1k', height: 90, type: 'today' },
        { label: '七日平均', value: '2.4k', height: 102, type: 'avg' },
        { label: '上周同日', value: '1.9k', height: 82, type: 'lastweek' }
      ]
    }
  }
]
