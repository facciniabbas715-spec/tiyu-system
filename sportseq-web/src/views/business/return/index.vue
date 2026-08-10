<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.orderNo" placeholder="归还单号" clearable style="width: 190px" @keyup.enter="loadData" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
        <el-option label="待确认" :value="0" />
        <el-option label="已确认" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button v-permission="['return:add']" type="success" @click="openCreate">登记归还</el-button>
    </div>

    <el-table :data="orderList" border>
      <el-table-column prop="orderNo" label="归还单号" width="180" />
      <el-table-column prop="borrowOrderNo" label="借用单号" width="180" />
      <el-table-column prop="username" label="借用人" width="100" />
      <el-table-column prop="warehouseName" label="仓库" width="110" />
      <el-table-column prop="totalQuantity" label="数量" width="70" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : row.status === 0 ? 'warning' : 'danger'">
            {{ row.status === 1 ? '已确认' : row.status === 0 ? '待确认' : '已驳回' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="登记时间" width="170" />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="['return:confirm']" v-if="row.status === 0" link type="success" @click="handleConfirm(row)">确认</el-button>
          <el-button v-permission="['return:confirm']" v-if="row.status === 0" link type="danger" @click="handleReject(row)">驳回</el-button>
          <el-button link @click="handleDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 14px; justify-content: flex-end"
      @change="loadData"
    />

    <el-dialog v-model="createVisible" title="登记归还" width="720px">
      <el-form label-width="100px">
        <el-form-item label="借用单号">
          <el-select
            v-model="selectedBorrowId"
            filterable
            placeholder="选择借用中的单号"
            style="width: 100%"
            @change="loadBorrowItems"
          >
            <el-option v-for="b in activeBorrows" :key="b.id" :label="`${b.orderNo}（${b.realName}）`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="归还明细">
          <el-table :data="returnRows" border size="small">
            <el-table-column prop="equipmentName" label="器材" min-width="140" />
            <el-table-column prop="unreturned" label="未还数量" width="90" />
            <el-table-column label="本次归还" width="120">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="1" :max="row.unreturned" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="器材状况" width="130">
              <template #default="{ row }">
                <el-select v-model="row.conditionStatus" size="small">
                  <el-option label="完好" :value="1" />
                  <el-option label="轻微损坏" :value="2" />
                  <el-option label="严重损坏" :value="3" />
                  <el-option label="丢失" :value="4" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="损坏说明" min-width="120">
              <template #default="{ row }">
                <el-input v-model="row.damageDesc" size="small" />
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!returnRows.length" @click="handleCreate">提交归还</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="归还单详情" width="760px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="单号">{{ detail?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="借用单">{{ detail?.borrowOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="借用人">{{ detail?.username }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail?.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ detail?.totalQuantity }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail?.status === 1 ? '已确认' : detail?.status === 0 ? '待确认' : '已驳回' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.items || []" border style="margin-top: 12px">
        <el-table-column prop="equipmentName" label="器材" min-width="140" />
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column label="状况" width="100">
          <template #default="{ row }">
            {{ conditionText(row.conditionStatus) }}
          </template>
        </el-table-column>
        <el-table-column label="逾期" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.isOverdue === 1" type="danger">{{ row.overdueDays }}天</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="penaltyAmount" label="违约金" width="90" />
        <el-table-column prop="damageDesc" label="说明" min-width="120" />
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  confirmReturn,
  createReturn,
  getReturnDetail,
  getReturnPage,
  rejectReturn,
  type ReturnOrder,
} from '@/api/business/return'
import { getBorrowPage, type BorrowOrder } from '@/api/business/borrow'

const orderList = ref<ReturnOrder[]>([])
const activeBorrows = ref<BorrowOrder[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, orderNo: '', status: undefined as number | undefined })
const createVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<ReturnOrder | null>(null)
const selectedBorrowId = ref<number>()
const returnRows = ref<{ borrowItemId: number; equipmentName: string; unreturned: number; quantity: number; conditionStatus: number; damageDesc: string }[]>([])

function conditionText(status: number) {
  return ({ 1: '完好', 2: '轻微损坏', 3: '严重损坏', 4: '丢失' } as Record<number, string>)[status] || '-'
}

async function loadData() {
  const data = await getReturnPage(query)
  orderList.value = data.records
  total.value = data.total
}

async function openCreate() {
  activeBorrows.value = (await getBorrowPage({ current: 1, size: 100, status: 2 })).records
  selectedBorrowId.value = undefined
  returnRows.value = []
  createVisible.value = true
}

async function loadBorrowItems(borrowId: number) {
  const borrow = activeBorrows.value.find((b) => b.id === borrowId)
  if (!borrow) return
  returnRows.value = borrow.items
    .filter((item) => item.quantity > item.returnedQuantity)
    .map((item) => ({
      borrowItemId: item.id,
      equipmentName: item.equipmentName,
      unreturned: item.quantity - item.returnedQuantity,
      quantity: item.quantity - item.returnedQuantity,
      conditionStatus: 1,
      damageDesc: '',
    }))
}

async function handleCreate() {
  if (!selectedBorrowId.value || !returnRows.value.length) return
  await createReturn({
    borrowOrderId: selectedBorrowId.value,
    items: returnRows.value.map((r) => ({
      borrowItemId: r.borrowItemId,
      quantity: r.quantity,
      conditionStatus: r.conditionStatus,
      damageDesc: r.damageDesc || undefined,
    })),
  })
  ElMessage.success('归还已登记，待仓库确认')
  createVisible.value = false
  await loadData()
}

async function handleConfirm(row: ReturnOrder) {
  await ElMessageBox.confirm(`确认验收归还单 ${row.orderNo}？库存将回补`, '验收确认', { type: 'warning' })
  await confirmReturn(row.id)
  ElMessage.success('验收完成，库存已回补')
  await loadData()
}

async function handleReject(row: ReturnOrder) {
  await ElMessageBox.confirm(`确认驳回归还单 ${row.orderNo}？`, '提示', { type: 'warning' })
  await rejectReturn(row.id)
  ElMessage.success('已驳回')
  await loadData()
}

async function handleDetail(row: ReturnOrder) {
  detail.value = await getReturnDetail(row.id)
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
</style>
