<template>
  <div class="statistics">
    <el-card shadow="never">
      <div class="toolbar">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
        <el-button type="primary" @click="loadAll">查询</el-button>
        <el-dropdown v-permission="['statistics:export']" @command="handleExport">
          <el-button type="success">
            导出 Excel<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="category">库存分布</el-dropdown-item>
              <el-dropdown-item command="warehouse">各仓库库存</el-dropdown-item>
              <el-dropdown-item command="trend">借用趋势</el-dropdown-item>
              <el-dropdown-item command="usage">器材使用率</el-dropdown-item>
              <el-dropdown-item command="dept">部门借用统计</el-dropdown-item>
              <el-dropdown-item command="overdue">逾期明细</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-card>

    <el-row :gutter="12">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>借用趋势</template>
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

    <el-card shadow="never" class="table-card">
      <template #header>部门借用统计</template>
      <el-table :data="deptRows" border>
        <el-table-column prop="deptName" label="部门" min-width="140" />
        <el-table-column prop="borrowCount" label="借用单数" width="110" />
        <el-table-column prop="borrowQuantity" label="借用数量" width="110" />
        <el-table-column prop="outstandingQuantity" label="在借数量" width="110" />
      </el-table>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>逾期统计</template>
      <el-row :gutter="12" class="overdue-cards">
        <el-col :xs="24" :sm="8">
          <div class="overdue-card overdue-card--coral">
            <div class="overdue-card__label">逾期单数</div>
            <div class="overdue-card__value">{{ overdue.overdueOrderCount }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="overdue-card overdue-card--amber">
            <div class="overdue-card__label">平均逾期天数</div>
            <div class="overdue-card__value">{{ overdue.avgOverdueDays }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="overdue-card overdue-card--ink">
            <div class="overdue-card__label">违约金合计(元)</div>
            <div class="overdue-card__value">{{ overdue.penaltyTotal }}</div>
          </div>
        </el-col>
      </el-row>
      <el-table :data="overdueRows" border style="margin-top: 14px">
        <el-table-column prop="returnOrderNo" label="归还单号" width="150" />
        <el-table-column prop="borrowOrderNo" label="借用单号" width="150" />
        <el-table-column prop="equipmentCode" label="器材编码" width="150" />
        <el-table-column prop="equipmentName" label="器材名称" min-width="130" />
        <el-table-column prop="quantity" label="归还数量" width="90" />
        <el-table-column prop="overdueDays" label="逾期天数" width="90" />
        <el-table-column prop="penaltyAmount" label="违约金(元)" width="100" />
        <el-table-column prop="confirmTime" label="确认时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { ArrowDown } from '@element-plus/icons-vue'
import {
  getBorrowTrend,
  getDeptBorrowStats,
  getEquipmentUsage,
  getOverdueItems,
  getOverdueStats,
  statisticsExportUrl,
  type BorrowTrendItem,
  type DeptBorrowItem,
  type EquipmentUsageItem,
  type OverdueItem,
  type OverdueStat,
  type RangeParams,
} from '@/api/statistics'
import { downloadFile } from '@/utils/download'

const dateRange = ref<[string, string] | null>(null)
const deptRows = ref<DeptBorrowItem[]>([])
const overdueRows = ref<OverdueItem[]>([])
const overdue = ref<OverdueStat>({
  overdueOrderCount: 0,
  overdueItemCount: 0,
  avgOverdueDays: 0,
  penaltyTotal: 0,
})

const trendChartEl = ref<HTMLDivElement>()
const usageChartEl = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let usageChart: echarts.ECharts | null = null

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

function rangeParams(): RangeParams {
  return dateRange.value
    ? { startDate: dateRange.value[0], endDate: dateRange.value[1] }
    : {}
}

async function loadAll() {
  const params = rangeParams()
  const [trend, usage, dept, stat, items] = await Promise.all([
    getBorrowTrend(params),
    getEquipmentUsage(params),
    getDeptBorrowStats(params),
    getOverdueStats(params),
    getOverdueItems(params),
  ])
  renderTrend(trend)
  renderUsage(usage)
  deptRows.value = dept
  overdue.value = stat
  overdueRows.value = items
}

function renderTrend(rows: BorrowTrendItem[]) {
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
        lineStyle: { width: 2, color: '#518bdb' },
        itemStyle: { color: '#518bdb' },
        areaStyle: { opacity: 0.08 },
        data: rows.map((row) => row.borrowQuantity),
      },
      {
        name: '归还数量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: '#36bab8' },
        itemStyle: { color: '#36bab8' },
        areaStyle: { opacity: 0.08 },
        data: rows.map((row) => row.returnQuantity),
      },
    ],
  })
}

function renderUsage(rows: EquipmentUsageItem[]) {
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
        itemStyle: { color: '#bf89cd', borderRadius: [0, 6, 6, 0] },
        data: sorted.map((row) => row.borrowCount),
      },
    ],
  })
}

async function handleExport(type: string) {
  const labels: Record<string, string> = {
    category: '库存分布',
    warehouse: '各仓库库存',
    trend: '借用趋势',
    usage: '器材使用率',
    dept: '部门借用统计',
    overdue: '逾期明细',
  }
  try {
    await downloadFile(statisticsExportUrl(type, rangeParams()), `${labels[type] ?? type}.xlsx`)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

function initCharts() {
  if (trendChartEl.value) trendChart = echarts.init(trendChartEl.value)
  if (usageChartEl.value) usageChart = echarts.init(usageChartEl.value)
}

function handleResize() {
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
  trendChart?.dispose()
  usageChart?.dispose()
})
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.chart-card,
.table-card {
  margin-bottom: 12px;
}

.chart {
  height: 320px;
}

.overdue-cards {
  margin-bottom: 4px;
}

.overdue-card {
  padding: 16px 18px;
  border-radius: 12px;

  &--coral {
    background: #f9edec;
  }

  &--amber {
    background: #f6f0e9;
  }

  &--ink {
    background: #f2f1f0;
  }

  &__label {
    font-size: 13px;
    color: #797267;
    margin-bottom: 6px;
  }

  &__value {
    font-size: 26px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;

    .overdue-card--coral & {
      color: #c64a45;
    }

    .overdue-card--amber & {
      color: #a66a24;
    }

    .overdue-card--ink & {
      color: #221f1c;
    }
  }
}
</style>
