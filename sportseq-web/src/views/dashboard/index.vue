<template>
  <div class="dashboard">
    <el-row :gutter="12">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="8" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card__body">
            <el-icon :size="34" :color="card.color"><component :is="card.icon" /></el-icon>
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
  { label: '今日借用', value: summary.value.todayBorrowCount, icon: markRaw(Odometer), color: '#409eff' },
  { label: '今日归还', value: summary.value.todayReturnCount, icon: markRaw(Back), color: '#67c23a' },
  { label: '今日入库', value: summary.value.todayStockInCount, icon: markRaw(Download), color: '#909399' },
  { label: '库存预警', value: summary.value.warningStockCount, icon: markRaw(Warning), color: '#e6a23c' },
  { label: '报废待审', value: summary.value.pendingScrapCount, icon: markRaw(Delete), color: '#f56c6c' },
  { label: '器材总数', value: summary.value.equipmentTotal, icon: markRaw(TrophyBase), color: '#722ed1' },
  { label: '库存总价值(元)', value: summary.value.stockTotalValue.toLocaleString('zh-CN'), icon: markRaw(Money), color: '#13c2c2' },
])

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
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        name: '库存数量',
        type: 'pie',
        radius: ['38%', '68%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}: {c}' },
        data: rows.map((row) => ({ name: row.categoryName, value: row.quantity })),
      },
    ],
  })
}

function renderWarehouse(rows: { warehouseName: string; quantity: number }[]) {
  if (!warehouseChart) return
  warehouseChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 24, bottom: 28 },
    xAxis: { type: 'category', data: rows.map((row) => row.warehouseName), axisLabel: { interval: 0 } },
    yAxis: { type: 'value' },
    series: [
      {
        name: '库存数量',
        type: 'bar',
        barMaxWidth: 40,
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
        data: rows.map((row) => row.quantity),
      },
    ],
  })
}

function renderTrend(rows: { month: string; borrowQuantity: number; returnQuantity: number }[]) {
  if (!trendChart) return
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['借用数量', '归还数量'], top: 0 },
    grid: { left: 40, right: 16, top: 40, bottom: 28 },
    xAxis: { type: 'category', boundaryGap: false, data: rows.map((row) => row.month) },
    yAxis: { type: 'value' },
    series: [
      {
        name: '借用数量',
        type: 'line',
        smooth: true,
        itemStyle: { color: '#409eff' },
        areaStyle: { opacity: 0.08 },
        data: rows.map((row) => row.borrowQuantity),
      },
      {
        name: '归还数量',
        type: 'line',
        smooth: true,
        itemStyle: { color: '#67c23a' },
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
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 110, right: 24, top: 16, bottom: 24 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: {
      type: 'category',
      data: sorted.map((row) => row.equipmentName),
      axisLabel: { width: 90, overflow: 'truncate' },
    },
    series: [
      {
        name: '借用次数',
        type: 'bar',
        barMaxWidth: 16,
        itemStyle: { color: '#722ed1', borderRadius: [0, 4, 4, 0] },
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

  &__body {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__value {
    font-size: 22px;
    font-weight: 600;
    color: #303133;
    line-height: 1.2;
  }

  &__label {
    margin-top: 4px;
    font-size: 13px;
    color: #909399;
  }
}

.chart-card {
  margin-bottom: 12px;
}

.chart {
  height: 300px;
}
</style>
