import type {
  CompetitorChangeItem,
  MetricHighlightItem,
  NewArrivalItem,
  ProductAdActivityItem,
  ProductAdsBlock,
  ProductMetricBlock,
  ProductOperationItem
} from '../types/workbench'

const buildMetricBlock = (
  title: string,
  compareList: ProductMetricBlock['compareList'],
  highlights: MetricHighlightItem[],
  targetValue: string,
  targetNote: string
): ProductMetricBlock => ({
  title,
  compareList,
  highlights,
  targetValue,
  targetNote
})

const buildAdsBlock = (
  highlights: MetricHighlightItem[],
  activityList: ProductAdActivityItem[],
  sourceNote: string
): ProductAdsBlock => ({
  title: '广告数据',
  highlights,
  activityList,
  sourceNote
})

export const newArrivalMock: NewArrivalItem[] = [
  { id: 'A001', title: '竞品 A 新上架：便携咖啡机 Pro', time: '09:12', category: '厨房小家电', shop: '竞品旗舰店A', snapshotUrl: 'https://snapshot.example.com/product/A001' },
  { id: 'B014', title: '竞品 B 新上架：无叶挂脖风扇 Lite', time: '10:03', category: '季节电器', shop: '竞品旗舰店B', snapshotUrl: 'https://snapshot.example.com/product/B014' },
  { id: 'C102', title: '竞品 C 新上架：桌面空气循环扇 Mini', time: '10:48', category: '家居电器', shop: '竞品旗舰店C', snapshotUrl: 'https://snapshot.example.com/product/C102' },
  { id: 'D208', title: '竞品 D 新上架：儿童收纳架 Plus', time: '11:05', category: '儿童家具', shop: '竞品旗舰店D', snapshotUrl: 'https://snapshot.example.com/product/D208' },
  { id: 'E315', title: '竞品 E 新上架：可折叠游戏围栏', time: '11:17', category: '母婴用品', shop: '竞品旗舰店E', snapshotUrl: 'https://snapshot.example.com/product/E315' },
  { id: 'F420', title: '竞品 F 新上架：玩具分类收纳柜 Max', time: '11:43', category: '儿童家具', shop: '竞品旗舰店F', snapshotUrl: 'https://snapshot.example.com/product/F420' },
  { id: 'G517', title: '竞品 G 新上架：带书架组合式收纳柜', time: '12:08', category: '儿童家具', shop: '竞品旗舰店G', snapshotUrl: 'https://snapshot.example.com/product/G517' },
  { id: 'H633', title: '竞品 H 新上架：木质九格玩具柜', time: '12:26', category: '家居收纳', shop: '竞品旗舰店H', snapshotUrl: 'https://snapshot.example.com/product/H633' },
  { id: 'J744', title: '竞品 J 新上架：多功能儿童书架收纳一体柜', time: '12:42', category: '儿童家具', shop: '竞品旗舰店J', snapshotUrl: 'https://snapshot.example.com/product/J744' }
]

export const competitorChangeMock: CompetitorChangeItem[] = [
  { id: 'CX9', name: '咖啡机旗舰款 X9', shop: '竞品旗舰店A', rank: '12', changes: ['价格：¥399 → ¥369', '活动：新增满300减30', '销量：日增 +126'] },
  { id: 'ACS', name: '挂脖风扇 AirCool S', shop: '竞品旗舰店B', rank: '28', changes: ['主图：已更新', '文案：卖点描述调整', '销量：日增 +84'] },
  { id: 'PMX', name: '空气循环扇 Pro Max', shop: '竞品旗舰店C', rank: '7', changes: ['价格：¥259 → ¥279', '活动：取消店铺券', '排名：11 → 7'] },
  { id: 'TSO', name: 'Toy Storage Organizer Plus', shop: '竞品旗舰店D', rank: '16', changes: ['价格：$89.99 → $84.99', '评价：新增 2 条差评', '广告：加大 SP 投放'] },
  { id: 'BPF', name: 'Baby Play Fence Lite', shop: '竞品旗舰店E', rank: '23', changes: ['排名：31 → 23', '主图：替换为场景图', '活动：新增 coupon'] },
  { id: 'KSR', name: 'Kids Shelf Rack 3-Tier', shop: '竞品旗舰店F', rank: '14', changes: ['价格：$72.99 → $76.99', '文案：新增 Montessori 关键词', '销量：日增 +57'] },
  { id: 'WBO', name: 'Wooden Book Organizer', shop: '竞品旗舰店G', rank: '19', changes: ['活动：取消 10% OFF', '流量：广告点击上升', '排名：17 → 19'] },
  { id: 'NBS', name: '9-Bin Storage Cabinet', shop: '竞品旗舰店H', rank: '11', changes: ['价格：$119.99 → $109.99', '评价：新增 11 条评论', '销量：日增 +92'] },
  { id: 'MGC', name: 'Multi-Grid Toy Cabinet', shop: '竞品旗舰店J', rank: '26', changes: ['主图：增加细节图', '广告：新增视频广告', '价格：保持不变'] }
]

const whiteBasinAdsActivity: ProductAdActivityItem[] = [
  { source: 'SP', campaignName: '广toy storage organizer拾贰', impressions: '6,137', clicks: '60', ctr: '0.98%', cpc: '$0.98', cost: '$58.96', sales: '$469.94', acos: '12.55%', orders: '6', cvr: '10.00%' },
  { source: 'SP', campaignName: '广toy storage拾贰', impressions: '4,005', clicks: '26', ctr: '0.65%', cpc: '$0.79', cost: '$20.60', sales: '$184.98', acos: '11.14%', orders: '2', cvr: '7.69%' },
  { source: 'SP', campaignName: '广toy organizer拾贰', impressions: '4,600', clicks: '25', ctr: '0.54%', cpc: '$0.83', cost: '$20.77', sales: '$74.99', acos: '27.70%', orders: '1', cvr: '4.00%' },
  { source: 'SP', campaignName: '自动广告拾贰', impressions: '1,042', clicks: '20', ctr: '1.92%', cpc: '$0.95', cost: '$18.98', sales: '$74.99', acos: '25.31%', orders: '1', cvr: '5.00%' },
  { source: 'SP', campaignName: '精kids toy storage organizer拾贰', impressions: '5,251', clicks: '28', ctr: '0.53%', cpc: '$1.07', cost: '$29.87', sales: '$229.97', acos: '12.99%', orders: '3', cvr: '10.71%' },
  { source: 'SP', campaignName: '广kids toy storage organizer拾贰', impressions: '2,360', clicks: '30', ctr: '1.27%', cpc: '$1.04', cost: '$31.13', sales: '$74.99', acos: '41.51%', orders: '1', cvr: '3.33%' },
  { source: 'SP', campaignName: 'ASIN广告拾贰', impressions: '5,380', clicks: '37', ctr: '0.69%', cpc: '$0.85', cost: '$31.46', sales: '$464.94', acos: '6.77%', orders: '6', cvr: '16.22%' },
  { source: 'SP', campaignName: '广 toy organizers and storage 拾贰', impressions: '5,150', clicks: '36', ctr: '0.70%', cpc: '$0.84', cost: '$30.35', sales: '$154.98', acos: '19.58%', orders: '2', cvr: '5.56%' },
  { source: 'SP', campaignName: '精kids toy organizer拾贰', impressions: '5,161', clicks: '18', ctr: '0.35%', cpc: '$1.07', cost: '$19.20', sales: '$0.00', acos: '-', orders: '0', cvr: '0.00%' },
  { source: 'SBV', campaignName: 'SBV-toy organizer-新款', impressions: '3,099', clicks: '44', ctr: '1.42%', cpc: '$0.77', cost: '$33.88', sales: '$244.97', acos: '13.83%', orders: '3', cvr: '6.82%' },
  { source: 'SBV', campaignName: 'SBV-toy storage-新款', impressions: '1,717', clicks: '24', ctr: '1.40%', cpc: '$0.78', cost: '$18.76', sales: '$0.00', acos: '有花费无销售额', orders: '0', cvr: '0.00%' },
  { source: 'SBV', campaignName: 'SBV-toy storage organizer-新款', impressions: '4,625', clicks: '58', ctr: '1.25%', cpc: '$0.86', cost: '$50.06', sales: '$334.96', acos: '14.95%', orders: '4', cvr: '6.90%' }
]

const defaultAdsActivity = (asin: string): ProductAdActivityItem[] => [
  { source: 'SBV', campaignName: `${asin}-SBV-品牌词`, impressions: '3,280', clicks: '34', ctr: '1.04%', cpc: '$0.86', cost: '$29.24', sales: '$229.97', acos: '12.72%', orders: '3', cvr: '8.82%' },
  { source: 'SP', campaignName: `${asin}-SP-核心词`, impressions: '5,460', clicks: '48', ctr: '0.88%', cpc: '$0.79', cost: '$37.92', sales: '$304.96', acos: '12.43%', orders: '4', cvr: '8.33%' },
  { source: 'SP', campaignName: `${asin}-SP-自动`, impressions: '2,140', clicks: '17', ctr: '0.79%', cpc: '$0.68', cost: '$11.56', sales: '$74.99', acos: '15.42%', orders: '1', cvr: '5.88%' }
]

const defaultAdsHighlights: MetricHighlightItem[] = [
  { label: '曝光量', value: '10,880', note: 'SBV + SP 汇总曝光', status: 'neutral' },
  { label: '点击量', value: '99', note: 'SBV + SP 汇总点击', status: 'neutral' },
  { label: 'CTR', value: '0.91%', note: '总点击 / 总曝光', status: 'neutral' },
  { label: 'CPC', value: '$0.80', note: '总花费 / 总点击', status: 'neutral' },
  { label: '花费', value: '$78.72', note: 'SBV + SP 合并后总花费', status: 'neutral' },
  { label: '总销售额', value: '$609.92', note: '汇总口径统一为总销售额', status: 'good' },
  { label: 'ACOS', value: '12.91%', note: '合并口径展示', status: 'good' },
  { label: '总订单数', value: '8', note: 'SBV + SP 合并订单', status: 'neutral' },
  { label: 'CVR', value: '8.10%', note: '点击到订单转化', status: 'good' }
]

export const operationDataMock: ProductOperationItem[] = [
  {
    id: 'P001',
    productName: '新款书架白盆',
    productCode: 'EXPERLAM-C001',
    shopName: 'EXPERLAM',
    siteName: '美国站',
    productTag: '重点观察',
    coverText: '白盆',
    coverTone: 'blue',
    childAsin: 'B0D45HSHDF',
    childSku: '230519W-SNSJ1-2_XM',
    review: {
      score: '4.7',
      reviewCount: '1,286',
      newReviewCount: '+12',
      badReviewCount: '2',
      latestTitle: '安装比预期简单',
      latestContent: '整体质感不错，孩子收纳玩具很方便，但希望抽屉再顺滑一点。',
      latestDate: '今天 10:26',
      latestAuthor: 'Amazon User'
    },
    sales: buildMetricBlock('销售数据', [
      { label: '今', value: '71', height: 54, type: 'today' },
      { label: '均', value: '58', height: 42, type: 'avg' },
      { label: '周', value: '61', height: 46, type: 'lastweek' },
      { label: '目', value: '65', height: 50, type: 'target' }
    ], [
      { label: '销量', value: '71', note: '较目标 +9.2%', status: 'good' },
      { label: '销售额', value: '$5,339', note: '稳定高于目标', status: 'good' },
      { label: '订单数', value: '71', note: '广告单 22', status: 'neutral' },
      { label: '转化率', value: '4.59%', note: '略低于目标 0.2pp', status: 'warn' }
    ], '65单', '销售目标：65单 / $4,900'),
    traffic: buildMetricBlock('流量数据', [
      { label: '今', value: '1548', height: 52, type: 'today' },
      { label: '均', value: '1380', height: 44, type: 'avg' },
      { label: '周', value: '1462', height: 48, type: 'lastweek' },
      { label: '目', value: '1500', height: 50, type: 'target' }
    ], [
      { label: '总流量', value: '1,548', note: '略高于目标', status: 'good' },
      { label: '自然流量', value: '1,142', note: '占比 73.8%', status: 'neutral' },
      { label: '广告流量', value: '406', note: 'SP 280 / SBV 126', status: 'neutral' },
      { label: 'Listing转化', value: '4.59%', note: '承接仍可优化', status: 'warn' }
    ], '1,500', '流量目标：1,500 UV / 转化率 4.8%'),
    ads: buildAdsBlock([
      { label: '曝光量', value: '48,527', note: 'SBV + SP 汇总曝光', status: 'neutral' },
      { label: '点击量', value: '406', note: 'SBV + SP 汇总点击', status: 'neutral' },
      { label: 'CTR', value: '0.84%', note: '总点击 / 总曝光', status: 'neutral' },
      { label: 'CPC', value: '$0.90', note: '总花费 / 总点击', status: 'neutral' },
      { label: '花费', value: '$364.02', note: 'SBV + SP 合并后当日总花费', status: 'warn' },
      { label: '总销售额', value: '$2,309.71', note: '与案例合并口径一致', status: 'good' },
      { label: 'ACOS', value: '15.76%', note: '合并后整体投产可控', status: 'good' },
      { label: '总订单数', value: '29', note: 'SBV 与 SP 合并订单', status: 'neutral' },
      { label: 'CVR', value: '7.14%', note: '点击到订单转化', status: 'good' }
    ], whiteBasinAdsActivity, '数据来源：SBV 单产品广告表 + SP 广告报告按广告ASIN筛选合并')
  },
  ...['P002', 'P003', 'P004', 'P005', 'P006', 'P007', 'P008'].map((id, index) => {
    const meta = [
      ['加宽新款白盆', 'EXPERLAM-C002', 'green', '加宽', 'B0F37X92BV'],
      ['儿童围栏', 'EXPERLAM-C003', 'orange', '围栏', 'B0F37H5W3F'],
      ['新款书架蓝盆', 'EXPERLAM-C004', 'blue', '蓝盆', 'B0D45FT6MF'],
      ['新款书架彩盆', 'EXPERLAM-C005', 'purple', '彩盆', 'B0DNQ3BDWF'],
      ['加宽新款彩盆', 'EXPERLAM-C006', 'orange', '加彩', 'B0FRF6F82F'],
      ['毛绒玩具收纳柜', 'EXPERLAM-C007', 'green', '收纳', 'B0F9T1L2NZ'],
      ['九格玩具收纳柜', 'EXPERLAM-C008', 'purple', '九格', 'B0FC6HQKCS']
    ][index] as [string, string, 'blue' | 'green' | 'orange' | 'purple', string, string]

    return {
      id,
      productName: meta[0],
      productCode: meta[1],
      shopName: 'EXPERLAM',
      siteName: '美国站',
      productTag: '广告观察',
      coverText: meta[3],
      coverTone: meta[2],
      childAsin: meta[4],
      childSku: '230519W-SNSJ1-2_XM',
      review: {
        score: '4.6',
        reviewCount: '860',
        newReviewCount: '+5',
        badReviewCount: '1',
        latestTitle: '评价摘要待接真实数据',
        latestContent: '当前为页面结构验证数据，后续由真实评论数据替换。',
        latestDate: '今天 11:00',
        latestAuthor: 'Amazon User'
      },
      sales: buildMetricBlock('销售数据', [
        { label: '今', value: '32', height: 36, type: 'today' },
        { label: '均', value: '28', height: 30, type: 'avg' },
        { label: '周', value: '27', height: 28, type: 'lastweek' },
        { label: '目', value: '30', height: 32, type: 'target' }
      ], [
        { label: '销量', value: '32', note: '当前为结构展示数据', status: 'good' },
        { label: '销售额', value: '$3,280', note: '待接真实数据', status: 'neutral' },
        { label: '订单数', value: '32', note: '待接真实数据', status: 'neutral' },
        { label: '转化率', value: '4.85%', note: '待接真实数据', status: 'warn' }
      ], '30单', '销售目标：待接真实数据'),
      traffic: buildMetricBlock('流量数据', [
        { label: '今', value: '1200', height: 42, type: 'today' },
        { label: '均', value: '1100', height: 36, type: 'avg' },
        { label: '周', value: '1080', height: 34, type: 'lastweek' },
        { label: '目', value: '1150', height: 38, type: 'target' }
      ], [
        { label: '总流量', value: '1,200', note: '当前为结构展示数据', status: 'good' },
        { label: '自然流量', value: '860', note: '待接真实数据', status: 'neutral' },
        { label: '广告流量', value: '340', note: '待接真实数据', status: 'neutral' },
        { label: 'Listing转化', value: '4.85%', note: '待接真实数据', status: 'warn' }
      ], '1,150', '流量目标：待接真实数据'),
      ads: buildAdsBlock(defaultAdsHighlights, defaultAdsActivity(meta[4]), '数据来源：SBV 单产品广告表 + SP 广告报告按广告ASIN筛选合并')
    }
  })
]
