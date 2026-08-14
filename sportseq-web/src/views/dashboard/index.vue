<template>
  <div class="dashboard">
    <el-row :gutter="12" justify="center">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="8" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card__body">
            <span class="stat-card__icon" :style="{ color: card.color, background: card.wash }">
              <el-icon :size="22"><component :is="card.icon" /></el-icon>
            </span>
            <div class="stat-card__info">
              <div class="stat-card__value">{{ card.value }}</div>
              <div class="stat-card__label">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>库存分布（按分类）</template>
          <div ref="categoryChartEl" class="chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>各仓库库存量</template>
          <div ref="warehouseChartEl" class="chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>借用趋势（近 12 个月）</template>
          <div ref="trendChartEl" class="chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>器材使用率 TOP 10</template>
          <div ref="usageChartEl" class="chart" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import {
  Back,
  Delete,
  Download,
  Money,
  Odometer,
  TrophyBase,
  Warning,
} from '@element-plus/icons-vue'
import { getDashboardSummary, type DashboardSummary } from '@/api/dashboard'
import {
  getBorrowTrend,
  getCategoryStock,
  getEquipmentUsage,
  getWarehouseStock,
} from '@/api/statistics'

const summary = ref<DashboardSummary>({
  todayBorrowCount: 0,
  todayReturnCount: 0,
  todayStockInCount: 0,
  warningStockCount: 0,
  pendingScrapCount: 0,
  equipmentTotal: 0,
  stockTotalValue: 0,
})

const cards = computed(() => [
  { label: '今日借用', value: summary.value.todayBorrowCount, icon: markRaw(Odometer), color: '#2e5aa0', wash: '#ecf1f9' },
  { label: '今日归还', value: summary.value.todayReturnCount, icon: markRaw(Back), color: '#3a6b2a', wash: '#ebf0ea' },
  { label: '今日入库', value: summary.value.todayStockInCount, icon: markRaw(Download), color: '#797267', wash: '#f2f1f0' },
  { label: '库存预警', value: summary.value.warningStockCount, icon: markRaw(Warning), color: '#a66a24', wash: '#f6f0e9' },
  { label: '报废待审', value: summary.value.pendingScrapCount, icon: markRaw(Delete), color: '#c64a45', wash: '#f9edec' },
  { label: '器材总数', value: summary.value.equipmentTotal, icon: markRaw(TrophyBase), color: '#8a5ca8', wash: '#f3edf7' },
  { label: '库存总价值(元)', value: summary.value.stockTotalValue.toLocaleString('zh-CN'), icon: markRaw(Money), color: '#0f7a78', wash: '#e5f4f3' },
])

const CHART_COLORS = {
  azure: '#518bdb',
  teal: '#36bab8',
  amber: '#e5a057',
  orchid: '#bf89cd',
  coral: '#ed6d68',
  green: '#3a6b2a',
}

const CHART_TEXT = '#4a453f'
const CHART_MUTED = '#797267'
const CHART_AXIS_LINE = '#e6e3dd'
const CHART_SPLIT_LINE = '#f0eee9'

function chartTooltip(extra: Record<string, unknown> = {}) {
  return {
    backgroundColor: '#ffffff',
    borderColor: '#e6e3dd',
    borderWidth: 1,
    padding: [8, 12] as [number, number],
    textStyle: { color: '#221f1c', fontSize: 13 },
    extraCssText: 'box-shadow: 0 10px 15px -3px rgba(34,31,28,0.12); border-radius: 8px;',
    ...extra,
  }
}

const categoryChartEl = ref<HTMLDivElement>()
const warehouseChartEl = ref<HTMLDivElement>()
const trendChartEl = ref<HTMLDivElement>()
const usageChartEl = ref<HTMLDivElement>()

let categoryChart: echarts.ECharts | null = null
let warehouseChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null
let usageChart: echarts.ECharts | null = null

async function loadAll() {
  const [summaryData, categories, warehouses, trend, usage] = await Promise.all([
    getDashboardSummary(),
    getCategoryStock(),
    getWarehouseStock(),
    getBorrowTrend(),
    getEquipmentUsage(),
  ])
  summary.value = summaryData
  renderCategory(categories)
  renderWarehouse(warehouses)
  renderTrend(trend)
  renderUsage(usage)
}

function renderCategory(rows: { categoryName: string; quantity: number }[]) {
  if (!categoryChart) return
  categoryChart.setOption({
    tooltip: chartTooltip({ trigger: 'item' }),
    title: rows.length
      ? undefined
      : {
          text: '暂无数据',
          left: 'center',
          top: 'middle',
          textStyle: { color: '#8c8a88', fontSize: 13, fontWeight: 400 },
        },
    color: [CHART_COLORS.azure, CHART_COLORS.teal, CHART_COLORS.amber, CHART_COLORS.orchid, CHART_COLORS.coral, CHART_COLORS.green],
    legend: { bottom: 0, textStyle: { color: CHART_TEXT } },
    series: [
      {
        name: '库存数量',
        type: 'pie',
        radius: ['38%', '68%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#ffffff', borderWidth: 2 },
        label: { formatter: '{b}: {c}', color: CHART_TEXT },
        data: rows.map((row) => ({ name: row.categoryName, value: row.quantity })),
      },
    ],
  })
}

function renderWarehouse(rows: { warehouseName: string; quantity: number }[]) {
  if (!warehouseChart) return
  warehouseChart.setOption({
    tooltip: chartTooltip({ trigger: 'axis' }),
    title: rows.length
      ? undefined
      : {
          text: '暂无数据',
          left: 'center',
          top: 'middle',
          textStyle: { color: '#8c8a88', fontSize: 13, fontWeight: 400 },
        },
    grid: { left: 40, right: 16, top: 24, bottom: 28 },
    xAxis: {
      type: 'category',
      data: rows.map((row) => row.warehouseName),
      axisLabel: { interval: 0, color: CHART_MUTED },
      axisLine: { lineStyle: { color: CHART_AXIS_LINE } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: CHART_MUTED },
      splitLine: { lineStyle: { color: CHART_SPLIT_LINE } },
    },
    series: [
      {
        name: '库存数量',
        type: 'bar',
        barMaxWidth: 40,
        itemStyle: { color: CHART_COLORS.azure, borderRadius: [6, 6, 0, 0] },
        data: rows.map((row) => row.quantity),
      },
    ],
  })
}

function renderTrend(rows: { month: string; borrowQuantity: number; returnQuantity: number }[]) {
  if (!trendChart) return
  trendChart.setOption({
    tooltip: chartTooltip({ trigger: 'axis' }),
    title: rows.length
      ? undefined
      : {
          text: '暂无数据',
          left: 'center',
          top: 'middle',
          textStyle: { color: '#8c8a88', fontSize: 13, fontWeight: 400 },
        },
    legend: { data: ['借用数量', '归还数量'], top: 0, textStyle: { color: CHART_TEXT } },
    grid: { left: 40, right: 16, top: 40, bottom: 28 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: rows.map((row) => row.month),
      axisLabel: { color: CHART_MUTED },
      axisLine: { lineStyle: { color: CHART_AXIS_LINE } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: CHART_MUTED },
      splitLine: { lineStyle: { color: CHART_SPLIT_LINE } },
    },
    series: [
      {
        name: '借用数量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: CHART_COLORS.azure },
        itemStyle: { color: CHART_COLORS.azure },
        areaStyle: { opacity: 0.08 },
        data: rows.map((row) => row.borrowQuantity),
      },
      {
        name: '归还数量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: CHART_COLORS.teal },
        itemStyle: { color: CHART_COLORS.teal },
        areaStyle: { opacity: 0.08 },
        data: rows.map((row) => row.returnQuantity),
      },
    ],
  })
}

function renderUsage(rows: { equipmentName: string; borrowCount: number }[]) {
  if (!usageChart) return
  const sorted = [...rows].reverse()
  usageChart.setOption({
    tooltip: chartTooltip({ trigger: 'axis', axisPointer: { type: 'shadow' } }),
    title: rows.length
      ? undefined
      : {
          text: '暂无数据',
          left: 'center',
          top: 'middle',
          textStyle: { color: '#8c8a88', fontSize: 13, fontWeight: 400 },
        },
    grid: { left: 110, right: 24, top: 16, bottom: 24 },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: CHART_MUTED },
      splitLine: { lineStyle: { color: CHART_SPLIT_LINE } },
    },
    yAxis: {
      type: 'category',
      data: sorted.map((row) => row.equipmentName),
      axisLabel: { width: 90, overflow: 'truncate', color: CHART_MUTED },
      axisLine: { lineStyle: { color: CHART_AXIS_LINE } },
      axisTick: { show: false },
    },
    series: [
      {
        name: '借用次数',
        type: 'bar',
        barMaxWidth: 16,
        itemStyle: { color: CHART_COLORS.orchid, borderRadius: [0, 6, 6, 0] },
        data: sorted.map((row) => row.borrowCount),
      },
    ],
  })
}

function initCharts() {
  if (categoryChartEl.value) categoryChart = echarts.init(categoryChartEl.value)
  if (warehouseChartEl.value) warehouseChart = echarts.init(warehouseChartEl.value)
  if (trendChartEl.value) trendChart = echarts.init(trendChartEl.value)
  if (usageChartEl.value) usageChart = echarts.init(usageChartEl.value)
}

function handleResize() {
  categoryChart?.resize()
  warehouseChart?.resize()
  trendChart?.resize()
  usageChart?.resize()
}

onMounted(() => {
  initCharts()
  window.addEventListener('resize', handleResize)
  loadAll()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  categoryChart?.dispose()
  warehouseChart?.dispose()
  trendChart?.dispose()
  usageChart?.dispose()
})
</script>

<style scoped lang="scss">
.stat-card {
  margin-bottom: 12px;
  box-shadow: var(--shadow-subtle);
  transition: transform 0.18s ease, box-shadow 0.18s ease;

  &:hover {
    transform: translateY(-2px);
  }

  &.is-hover-shadow:hover {
    box-shadow: var(--shadow-md);
  }

  &__body {
    display: flex;
    align-items: center;
    gap: 14px;
  }

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 46px;
    height: 46px;
    border-radius: 12px;
    flex-shrink: 0;
  }

  &__value {
    font-size: 24px;
    font-weight: 700;
    color: #221f1c;
    line-height: 1.2;
    font-variant-numeric: tabular-nums;
  }

  &__label {
    margin-top: 3px;
    font-size: 13px;
    color: #797267;
  }
}

.chart-card {
  margin-bottom: 12px;
}

.chart {
  height: 300px;
}
</style>
