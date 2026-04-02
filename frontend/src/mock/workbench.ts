import type {
  CompetitorChangeItem,
  MetricHighlightItem,
  NewArrivalItem,
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
  },
  {
    id: 'D208',
    title: '竞品 D 新上架：儿童收纳架 Plus',
    time: '11:05',
    category: '儿童家具',
    shop: '竞品旗舰店D',
    snapshotUrl: 'https://snapshot.example.com/product/D208'
  },
  {
    id: 'E315',
    title: '竞品 E 新上架：可折叠游戏围栏',
    time: '11:17',
    category: '母婴用品',
    shop: '竞品旗舰店E',
    snapshotUrl: 'https://snapshot.example.com/product/E315'
  },
  {
    id: 'F420',
    title: '竞品 F 新上架：玩具分类收纳柜 Max',
    time: '11:43',
    category: '儿童家具',
    shop: '竞品旗舰店F',
    snapshotUrl: 'https://snapshot.example.com/product/F420'
  },
  {
    id: 'G517',
    title: '竞品 G 新上架：带书架组合式收纳柜',
    time: '12:08',
    category: '儿童家具',
    shop: '竞品旗舰店G',
    snapshotUrl: 'https://snapshot.example.com/product/G517'
  },
  {
    id: 'H633',
    title: '竞品 H 新上架：木质九格玩具柜',
    time: '12:26',
    category: '家居收纳',
    shop: '竞品旗舰店H',
    snapshotUrl: 'https://snapshot.example.com/product/H633'
  },
  {
    id: 'J744',
    title: '竞品 J 新上架：多功能儿童书架收纳一体柜',
    time: '12:42',
    category: '儿童家具',
    shop: '竞品旗舰店J',
    snapshotUrl: 'https://snapshot.example.com/product/J744'
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
  },
  {
    id: 'TSO',
    name: 'Toy Storage Organizer Plus',
    shop: '竞品旗舰店D',
    rank: '16',
    changes: ['价格：$89.99 → $84.99', '评价：新增 2 条差评', '广告：加大 SP 投放']
  },
  {
    id: 'BPF',
    name: 'Baby Play Fence Lite',
    shop: '竞品旗舰店E',
    rank: '23',
    changes: ['排名：31 → 23', '主图：替换为场景图', '活动：新增 coupon']
  },
  {
    id: 'KSR',
    name: 'Kids Shelf Rack 3-Tier',
    shop: '竞品旗舰店F',
    rank: '14',
    changes: ['价格：$72.99 → $76.99', '文案：新增 Montessori 关键词', '销量：日增 +57']
  },
  {
    id: 'WBO',
    name: 'Wooden Book Organizer',
    shop: '竞品旗舰店G',
    rank: '19',
    changes: ['活动：取消 10% OFF', '流量：广告点击上升', '排名：17 → 19']
  },
  {
    id: 'NBS',
    name: '9-Bin Storage Cabinet',
    shop: '竞品旗舰店H',
    rank: '11',
    changes: ['价格：$119.99 → $109.99', '评价：新增 11 条评论', '销量：日增 +92']
  },
  {
    id: 'MGC',
    name: 'Multi-Grid Toy Cabinet',
    shop: '竞品旗舰店J',
    rank: '26',
    changes: ['主图：增加细节图', '广告：新增视频广告', '价格：保持不变']
  }
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
    ads: buildMetricBlock('广告数据', [
      { label: '今', value: '364', height: 44, type: 'today' },
      { label: '均', value: '332', height: 38, type: 'avg' },
      { label: '周', value: '341', height: 40, type: 'lastweek' },
      { label: '目', value: '350', height: 42, type: 'target' }
    ], [
      { label: '广告花费', value: '$364', note: '略高于预算', status: 'warn' },
      { label: '广告销售额', value: '$682', note: '以 SP 拉动为主', status: 'neutral' },
      { label: 'ACOS', value: '15.11%', note: '控制在目标内', status: 'good' },
      { label: 'CVR', value: '7.86%', note: '表现稳定', status: 'good' }
    ], 'ACOS ≤ 16%', '广告目标：ACOS ≤ 16%，花费 ≤ $350')
  },
  {
    id: 'P002',
    productName: '加宽新款白盆',
    productCode: 'EXPERLAM-C002',
    shopName: 'EXPERLAM',
    siteName: '美国站',
    productTag: '重点拉升',
    coverText: '加宽',
    coverTone: 'green',
    childAsin: 'B0F37X92BV',
    childSku: '230519W-SNSJ1-2_XM',
    review: {
      score: '4.5', reviewCount: '942', newReviewCount: '+8', badReviewCount: '3', latestTitle: '容量很大，但包装需加强', latestContent: '储物空间很好，适合儿童房。收到时外箱略有破损，建议加强运输包装。', latestDate: '今天 09:42', latestAuthor: 'Jamie M.'
    },
    sales: buildMetricBlock('销售数据', [
      { label: '今', value: '36', height: 34, type: 'today' },
      { label: '均', value: '41', height: 42, type: 'avg' },
      { label: '周', value: '39', height: 38, type: 'lastweek' },
      { label: '目', value: '42', height: 44, type: 'target' }
    ], [
      { label: '销量', value: '36', note: '低于目标 14.3%', status: 'risk' },
      { label: '销售额', value: '$3,359', note: '略低于预期', status: 'warn' },
      { label: '订单数', value: '36', note: '广告单 8', status: 'neutral' },
      { label: '转化率', value: '5.81%', note: '转化尚可', status: 'good' }
    ], '42单', '销售目标：42单 / $3,800'),
    traffic: buildMetricBlock('流量数据', [
      { label: '今', value: '620', height: 28, type: 'today' },
      { label: '均', value: '705', height: 38, type: 'avg' },
      { label: '周', value: '668', height: 34, type: 'lastweek' },
      { label: '目', value: '760', height: 42, type: 'target' }
    ], [
      { label: '总流量', value: '620', note: '明显低于目标', status: 'risk' },
      { label: '自然流量', value: '417', note: '占比 67.3%', status: 'neutral' },
      { label: '广告流量', value: '203', note: 'SP 145 / SBV 58', status: 'neutral' },
      { label: 'Listing转化', value: '5.81%', note: '需补流量', status: 'good' }
    ], '760', '流量目标：760 UV / 自然流量占比 70%+'),
    ads: buildMetricBlock('广告数据', [
      { label: '今', value: '225', height: 36, type: 'today' },
      { label: '均', value: '206', height: 30, type: 'avg' },
      { label: '周', value: '214', height: 32, type: 'lastweek' },
      { label: '目', value: '210', height: 31, type: 'target' }
    ], [
      { label: '广告花费', value: '$225', note: '高于预算', status: 'warn' },
      { label: '广告销售额', value: '$275', note: '拉动偏弱', status: 'risk' },
      { label: 'ACOS', value: '22.88%', note: '超目标 4.9pp', status: 'risk' },
      { label: 'CVR', value: '5.52%', note: '中等水平', status: 'neutral' }
    ], 'ACOS ≤ 18%', '广告目标：ACOS ≤ 18%，花费 ≤ $210')
  },
  {
    id: 'P003',
    productName: '儿童围栏',
    productCode: 'EXPERLAM-C003',
    shopName: 'EXPERLAM',
    siteName: '美国站',
    productTag: '稳健款',
    coverText: '围栏',
    coverTone: 'orange',
    childAsin: 'B0F37H5W3F',
    childSku: 'PLAYPEN-58',
    review: {
      score: '4.8', reviewCount: '2,148', newReviewCount: '+5', badReviewCount: '0', latestTitle: '材质扎实，宝宝活动空间足', latestContent: '围栏很稳，安装方便，木质感很好，已经推荐给身边朋友。', latestDate: '今天 08:17', latestAuthor: 'Sophia L.'
    },
    sales: buildMetricBlock('销售数据', [
      { label: '今', value: '26', height: 40, type: 'today' },
      { label: '均', value: '23', height: 34, type: 'avg' },
      { label: '周', value: '22', height: 32, type: 'lastweek' },
      { label: '目', value: '24', height: 36, type: 'target' }
    ], [
      { label: '销量', value: '26', note: '高于目标', status: 'good' },
      { label: '销售额', value: '$3,457', note: '高于目标 +11%', status: 'good' },
      { label: '订单数', value: '26', note: '广告单 12', status: 'neutral' },
      { label: '转化率', value: '5.62%', note: '表现优秀', status: 'good' }
    ], '24单', '销售目标：24单 / $3,100'),
    traffic: buildMetricBlock('流量数据', [
      { label: '今', value: '463', height: 34, type: 'today' },
      { label: '均', value: '428', height: 30, type: 'avg' },
      { label: '周', value: '401', height: 28, type: 'lastweek' },
      { label: '目', value: '430', height: 31, type: 'target' }
    ], [
      { label: '总流量', value: '463', note: '高于目标', status: 'good' },
      { label: '自然流量', value: '260', note: '占比 56.2%', status: 'neutral' },
      { label: '广告流量', value: '203', note: 'SP 122 / SBV 81', status: 'good' },
      { label: 'Listing转化', value: '5.62%', note: '高于目标 1.4pp', status: 'good' }
    ], '430', '流量目标：430 UV / 转化率 ≥ 4.8%'),
    ads: buildMetricBlock('广告数据', [
      { label: '今', value: '132', height: 32, type: 'today' },
      { label: '均', value: '118', height: 26, type: 'avg' },
      { label: '周', value: '121', height: 27, type: 'lastweek' },
      { label: '目', value: '125', height: 28, type: 'target' }
    ], [
      { label: '广告花费', value: '$132', note: '略高于预算', status: 'warn' },
      { label: '广告销售额', value: '$809', note: '投放效率稳定', status: 'good' },
      { label: 'ACOS', value: '16.30%', note: '符合目标区间', status: 'good' },
      { label: 'CVR', value: '7.41%', note: '广告转化强', status: 'good' }
    ], 'ACOS ≤ 17%', '广告目标：ACOS ≤ 17%，花费 ≤ $125')
  },
  {
    id: 'P004',
    productName: '新款书架蓝盆',
    productCode: 'EXPERLAM-C004',
    shopName: 'EXPERLAM',
    siteName: '美国站',
    productTag: '新品',
    coverText: '蓝盆',
    coverTone: 'blue',
    childAsin: 'B0D45FT6MF',
    childSku: 'BOOKCASE-BLUE',
    review: {
      score: '4.6', reviewCount: '518', newReviewCount: '+4', badReviewCount: '1', latestTitle: '颜色更受欢迎', latestContent: '蓝色版本比预期更好看，整体安装也比较顺利。', latestDate: '今天 11:03', latestAuthor: 'Mia T.'
    },
    sales: buildMetricBlock('销售数据', [
      { label: '今', value: '8', height: 18, type: 'today' },
      { label: '均', value: '10', height: 24, type: 'avg' },
      { label: '周', value: '9', height: 21, type: 'lastweek' },
      { label: '目', value: '12', height: 28, type: 'target' }
    ], [
      { label: '销量', value: '8', note: '低于目标', status: 'warn' },
      { label: '销售额', value: '$639', note: '新品爬坡期', status: 'neutral' },
      { label: '订单数', value: '8', note: '广告单较少', status: 'neutral' },
      { label: '转化率', value: '3.31%', note: '还需优化', status: 'warn' }
    ], '12单', '销售目标：12单 / $850'),
    traffic: buildMetricBlock('流量数据', [
      { label: '今', value: '242', height: 20, type: 'today' },
      { label: '均', value: '286', height: 26, type: 'avg' },
      { label: '周', value: '258', height: 22, type: 'lastweek' },
      { label: '目', value: '320', height: 30, type: 'target' }
    ], [
      { label: '总流量', value: '242', note: '低于目标', status: 'warn' },
      { label: '自然流量', value: '198', note: '占比 81.8%', status: 'good' },
      { label: '广告流量', value: '44', note: '投放较轻', status: 'neutral' },
      { label: 'Listing转化', value: '3.31%', note: '需继续爬升', status: 'warn' }
    ], '320', '流量目标：320 UV'),
    ads: buildMetricBlock('广告数据', [
      { label: '今', value: '46', height: 18, type: 'today' },
      { label: '均', value: '52', height: 22, type: 'avg' },
      { label: '周', value: '48', height: 20, type: 'lastweek' },
      { label: '目', value: '55', height: 23, type: 'target' }
    ], [
      { label: '广告花费', value: '$46', note: '预算偏保守', status: 'neutral' },
      { label: '广告销售额', value: '$121', note: '拉动有限', status: 'warn' },
      { label: 'ACOS', value: '38.02%', note: '明显偏高', status: 'risk' },
      { label: 'CVR', value: '3.86%', note: '需优化关键词', status: 'risk' }
    ], 'ACOS ≤ 25%', '广告目标：先控 ACOS，再稳步放量')
  },
  {
    id: 'P005',
    productName: '新款书架彩盆',
    productCode: 'EXPERLAM-C005',
    shopName: 'EXPERLAM',
    siteName: '美国站',
    productTag: '观察款',
    coverText: '彩盆',
    coverTone: 'purple',
    childAsin: 'B0DNQ3BDWF',
    childSku: 'BOOKCASE-COLOR',
    review: {
      score: '4.4', reviewCount: '406', newReviewCount: '+3', badReviewCount: '1', latestTitle: '颜色吸引小朋友', latestContent: '彩色收纳盒更讨孩子喜欢，但组装步骤可以再清晰一点。', latestDate: '今天 11:28', latestAuthor: 'Noah K.'
    },
    sales: buildMetricBlock('销售数据', [
      { label: '今', value: '1', height: 8, type: 'today' },
      { label: '均', value: '3', height: 16, type: 'avg' },
      { label: '周', value: '2', height: 12, type: 'lastweek' },
      { label: '目', value: '4', height: 20, type: 'target' }
    ], [
      { label: '销量', value: '1', note: '明显低于目标', status: 'risk' },
      { label: '销售额', value: '$79', note: '需重点观察', status: 'risk' },
      { label: '订单数', value: '1', note: '自然单为主', status: 'neutral' },
      { label: '转化率', value: '0.42%', note: '转化偏低', status: 'risk' }
    ], '4单', '销售目标：4单 / $300'),
    traffic: buildMetricBlock('流量数据', [
      { label: '今', value: '238', height: 22, type: 'today' },
      { label: '均', value: '251', height: 24, type: 'avg' },
      { label: '周', value: '244', height: 23, type: 'lastweek' },
      { label: '目', value: '280', height: 28, type: 'target' }
    ], [
      { label: '总流量', value: '238', note: '流量尚可', status: 'neutral' },
      { label: '自然流量', value: '220', note: '广告很少', status: 'neutral' },
      { label: '广告流量', value: '18', note: '基本未放量', status: 'neutral' },
      { label: 'Listing转化', value: '0.42%', note: '主问题在转化', status: 'risk' }
    ], '280', '流量目标：280 UV / 先解决转化'),
    ads: buildMetricBlock('广告数据', [
      { label: '今', value: '12', height: 10, type: 'today' },
      { label: '均', value: '18', height: 14, type: 'avg' },
      { label: '周', value: '16', height: 12, type: 'lastweek' },
      { label: '目', value: '20', height: 16, type: 'target' }
    ], [
      { label: '广告花费', value: '$12', note: '投入较少', status: 'neutral' },
      { label: '广告销售额', value: '$0', note: '暂无广告产出', status: 'risk' },
      { label: 'ACOS', value: '-', note: '无广告销售额', status: 'risk' },
      { label: 'CVR', value: '0%', note: '需重新选词', status: 'risk' }
    ], '先验证词包', '广告目标：先小预算试错，再看能否转化')
  },
  {
    id: 'P006',
    productName: '加宽新款彩盆',
    productCode: 'EXPERLAM-C006',
    shopName: 'EXPERLAM',
    siteName: '美国站',
    productTag: '潜力款',
    coverText: '加彩',
    coverTone: 'orange',
    childAsin: 'B0FRF6F82F',
    childSku: 'WIDE-COLOR',
    review: {
      score: '4.6', reviewCount: '603', newReviewCount: '+6', badReviewCount: '1', latestTitle: '外观吸睛，容量也够', latestContent: '孩子喜欢彩色版本，整体储物能力也不错，适合玩具较多的家庭。', latestDate: '今天 12:02', latestAuthor: 'Olivia P.'
    },
    sales: buildMetricBlock('销售数据', [
      { label: '今', value: '7', height: 20, type: 'today' },
      { label: '均', value: '6', height: 18, type: 'avg' },
      { label: '周', value: '5', height: 14, type: 'lastweek' },
      { label: '目', value: '8', height: 24, type: 'target' }
    ], [
      { label: '销量', value: '7', note: '接近目标', status: 'good' },
      { label: '销售额', value: '$623', note: '有提升空间', status: 'neutral' },
      { label: '订单数', value: '7', note: '自然+广告均衡', status: 'neutral' },
      { label: '转化率', value: '6.14%', note: '转化不错', status: 'good' }
    ], '8单', '销售目标：8单 / $700'),
    traffic: buildMetricBlock('流量数据', [
      { label: '今', value: '114', height: 12, type: 'today' },
      { label: '均', value: '132', height: 16, type: 'avg' },
      { label: '周', value: '127', height: 15, type: 'lastweek' },
      { label: '目', value: '140', height: 18, type: 'target' }
    ], [
      { label: '总流量', value: '114', note: '略低于目标', status: 'warn' },
      { label: '自然流量', value: '81', note: '占比 71%', status: 'good' },
      { label: '广告流量', value: '33', note: '还可继续放量', status: 'neutral' },
      { label: 'Listing转化', value: '6.14%', note: '承接较好', status: 'good' }
    ], '140', '流量目标：140 UV'),
    ads: buildMetricBlock('广告数据', [
      { label: '今', value: '39', height: 16, type: 'today' },
      { label: '均', value: '34', height: 14, type: 'avg' },
      { label: '周', value: '31', height: 12, type: 'lastweek' },
      { label: '目', value: '36', height: 15, type: 'target' }
    ], [
      { label: '广告花费', value: '$39', note: '略高于目标', status: 'warn' },
      { label: '广告销售额', value: '$183', note: '投产还可以', status: 'good' },
      { label: 'ACOS', value: '21.31%', note: '略高', status: 'warn' },
      { label: 'CVR', value: '5.02%', note: '继续优化', status: 'neutral' }
    ], 'ACOS ≤ 20%', '广告目标：ACOS ≤ 20%')
  },
  {
    id: 'P007',
    productName: '毛绒玩具收纳柜',
    productCode: 'EXPERLAM-C007',
    shopName: 'EXPERLAM',
    siteName: '美国站',
    productTag: '评价敏感',
    coverText: '收纳',
    coverTone: 'green',
    childAsin: 'B0F9T1L2NZ',
    childSku: 'STUFFED-STORAGE',
    review: {
      score: '4.5', reviewCount: '1,109', newReviewCount: '+7', badReviewCount: '2', latestTitle: '容量足，但包装要再稳一些', latestContent: '空间非常够用，适合大一点的玩具和毛绒收纳。', latestDate: '今天 12:18', latestAuthor: 'Ella R.'
    },
    sales: buildMetricBlock('销售数据', [
      { label: '今', value: '34', height: 36, type: 'today' },
      { label: '均', value: '31', height: 30, type: 'avg' },
      { label: '周', value: '29', height: 26, type: 'lastweek' },
      { label: '目', value: '32', height: 32, type: 'target' }
    ], [
      { label: '销量', value: '34', note: '高于目标', status: 'good' },
      { label: '销售额', value: '$3,409', note: '表现稳定', status: 'good' },
      { label: '订单数', value: '34', note: '广告贡献适中', status: 'neutral' },
      { label: '转化率', value: '2.27%', note: '转化仍偏低', status: 'warn' }
    ], '32单', '销售目标：32单 / $3,200'),
    traffic: buildMetricBlock('流量数据', [
      { label: '今', value: '1498', height: 46, type: 'today' },
      { label: '均', value: '1430', height: 42, type: 'avg' },
      { label: '周', value: '1398', height: 40, type: 'lastweek' },
      { label: '目', value: '1450', height: 44, type: 'target' }
    ], [
      { label: '总流量', value: '1,498', note: '略高于目标', status: 'good' },
      { label: '自然流量', value: '1,133', note: '占比 75.6%', status: 'good' },
      { label: '广告流量', value: '365', note: '拉新稳定', status: 'neutral' },
      { label: 'Listing转化', value: '2.27%', note: '需优化页面卖点', status: 'warn' }
    ], '1,450', '流量目标：1,450 UV'),
    ads: buildMetricBlock('广告数据', [
      { label: '今', value: '196', height: 28, type: 'today' },
      { label: '均', value: '182', height: 24, type: 'avg' },
      { label: '周', value: '176', height: 22, type: 'lastweek' },
      { label: '目', value: '190', height: 26, type: 'target' }
    ], [
      { label: '广告花费', value: '$196', note: '接近预算', status: 'neutral' },
      { label: '广告销售额', value: '$420', note: '可继续提升', status: 'neutral' },
      { label: 'ACOS', value: '18.72%', note: '略优于目标', status: 'good' },
      { label: 'CVR', value: '4.91%', note: '表现中等', status: 'neutral' }
    ], 'ACOS ≤ 20%', '广告目标：ACOS ≤ 20%')
  },
  {
    id: 'P008',
    productName: '九格玩具收纳柜',
    productCode: 'EXPERLAM-C008',
    shopName: 'EXPERLAM',
    siteName: '美国站',
    productTag: '放量款',
    coverText: '九格',
    coverTone: 'purple',
    childAsin: 'B0FC6HQKCS',
    childSku: '9BIN-STORAGE',
    review: {
      score: '4.6', reviewCount: '1,587', newReviewCount: '+9', badReviewCount: '1', latestTitle: '储物区分很清晰', latestContent: '分类收纳很方便，孩子自己也愿意整理玩具。', latestDate: '今天 12:33', latestAuthor: 'Lucas D.'
    },
    sales: buildMetricBlock('销售数据', [
      { label: '今', value: '31', height: 34, type: 'today' },
      { label: '均', value: '28', height: 28, type: 'avg' },
      { label: '周', value: '27', height: 26, type: 'lastweek' },
      { label: '目', value: '30', height: 32, type: 'target' }
    ], [
      { label: '销量', value: '31', note: '略高于目标', status: 'good' },
      { label: '销售额', value: '$3,615', note: '走势稳定', status: 'good' },
      { label: '订单数', value: '31', note: '广告带动明显', status: 'neutral' },
      { label: '转化率', value: '2.13%', note: '偏低但可接受', status: 'warn' }
    ], '30单', '销售目标：30单 / $3,400'),
    traffic: buildMetricBlock('流量数据', [
      { label: '今', value: '1500', height: 44, type: 'today' },
      { label: '均', value: '1458', height: 40, type: 'avg' },
      { label: '周', value: '1412', height: 36, type: 'lastweek' },
      { label: '目', value: '1480', height: 42, type: 'target' }
    ], [
      { label: '总流量', value: '1,500', note: '略高于目标', status: 'good' },
      { label: '自然流量', value: '1,135', note: '自然流量稳定', status: 'good' },
      { label: '广告流量', value: '365', note: '广告承接较好', status: 'neutral' },
      { label: 'Listing转化', value: '2.13%', note: '可继续优化', status: 'warn' }
    ], '1,480', '流量目标：1,480 UV'),
    ads: buildMetricBlock('广告数据', [
      { label: '今', value: '214', height: 30, type: 'today' },
      { label: '均', value: '206', height: 28, type: 'avg' },
      { label: '周', value: '198', height: 24, type: 'lastweek' },
      { label: '目', value: '205', height: 27, type: 'target' }
    ], [
      { label: '广告花费', value: '$214', note: '略高于预算', status: 'warn' },
      { label: '广告销售额', value: '$512', note: '投放有回报', status: 'good' },
      { label: 'ACOS', value: '17.85%', note: '控制良好', status: 'good' },
      { label: 'CVR', value: '5.14%', note: '中上水平', status: 'good' }
    ], 'ACOS ≤ 18%', '广告目标：ACOS ≤ 18%')
  }
]
