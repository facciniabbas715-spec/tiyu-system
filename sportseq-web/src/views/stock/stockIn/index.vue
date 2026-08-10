<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.orderNo" placeholder="入库单号" clearable style="width: 190px" @keyup.enter="loadData" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
        <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="Number(key)" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button v-permission="['stock:in:add']" type="success" @click="openCreate">新建入库单</el-button>
    </div>

    <el-table :data="orderList" border>
      <el-table-column prop="orderNo" label="入库单号" width="180" />
      <el-table-column prop="warehouseName" label="仓库" width="110" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">{{ inTypeMap[row.inType] }}</template>
      </el-table-column>
      <el-table-column prop="totalQuantity" label="数量" width="80" />
      <el-table-column prop="totalAmount" label="金额" width="100" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="['stock:in:add']" v-if="row.status === 0" link type="primary" @click="handleSubmit(row)">提交</el-button>
          <el-button v-permission="['stock:in:audit']" v-if="row.status === 1" link type="primary" @click="handleAudit(row, true)">通过</el-button>
          <el-button v-permission="['stock:in:audit']" v-if="row.status === 1" link type="danger" @click="handleAudit(row, false)">驳回</el-button>
          <el-button v-permission="['stock:in:receive']" v-if="row.status === 2" link type="success" @click="handleReceive(row)">验收</el-button>
          <el-button v-permission="['stock:in:add']" v-if="[0, 1, 4].includes(row.status)" link type="warning" @click="handleCancel(row)">作废</el-button>
          <el-button v-permission="['stock:in:remove']" v-if="row.status === 0" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="createVisible" title="新建入库单" width="720px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="仓库">
          <el-select v-model="form.warehouseId" style="width: 200px">
            <el-option v-for="w in warehouseList" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库类型">
          <el-select v-model="form.inType" style="width: 200px">
            <el-option label="采购入库" :value="1" />
            <el-option label="退货退回" :value="2" />
            <el-option label="盘盈入库" :value="3" />
            <el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="form.supplier" style="width: 300px" />
        </el-form-item>
        <el-form-item label="明细">
          <div class="items">
            <div v-for="(item, index) in form.items" :key="index" class="item-row">
              <el-select v-model="item.equipmentId" filterable placeholder="器材" style="width: 220px">
                <el-option v-for="e in equipmentOptions" :key="e.id" :label="`${e.equipmentName} (${e.equipmentCode})`" :value="e.id" />
              </el-select>
              <el-input-number v-model="item.quantity" :min="1" placeholder="数量" style="width: 120px" />
              <el-input-number v-model="item.unitPrice" :min="0" :precision="2" placeholder="单价" style="width: 130px" />
              <el-button type="danger" link @click="form.items.splice(index, 1)">移除</el-button>
            </div>
            <el-button type="primary" plain size="small" @click="addRow">添加明细</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">保存草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="入库单详情" width="720px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="单号">{{ detail?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail?.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail ? statusMap[detail.status] : '' }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail?.supplier || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ detail?.totalQuantity }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detail?.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="3">{{ detail?.auditRemark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.items || []" border style="margin-top: 12px">
        <el-table-column prop="equipmentCode" label="编码" width="150" />
        <el-table-column prop="equipmentName" label="名称" min-width="120" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="unitPrice" label="单价" width="100" />
        <el-table-column prop="amount" label="金额" width="100" />
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  auditStockIn,
  cancelStockIn,
  createStockIn,
  deleteStockIn,
  getStockInDetail,
  getStockInPage,
  receiveStockIn,
  submitStockIn,
  type StockInOrder,
} from '@/api/stock/stockIn'
import { getEquipmentPage, type EquipmentItem } from '@/api/equipment/equipment'
import { getWarehouseList, type WarehouseItem } from '@/api/equipment/warehouse'

const statusMap: Record<number, string> = {
  0: '草稿',
  1: '待审核',
  2: '已通过',
  3: '已验收',
  4: '已驳回',
  5: '已作废',
}
const inTypeMap: Record<number, string> = { 1: '采购入库', 2: '退货退回', 3: '盘盈入库', 4: '其他' }

function statusTag(status: number) {
  return ({ 0: 'info', 1: 'warning', 2: 'primary', 3: 'success', 4: 'danger', 5: 'info' } as Record<number, string>)[status] || 'info'
}

const orderList = ref<StockInOrder[]>([])
const warehouseList = ref<WarehouseItem[]>([])
const equipmentOptions = ref<EquipmentItem[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, orderNo: '', status: undefined as number | undefined })
const createVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<StockInOrder | null>(null)
const form = reactive<{ warehouseId: number; inType: number; supplier: string; items: { equipmentId: number; quantity: number; unitPrice?: number }[] }>({
  warehouseId: 0,
  inType: 1,
  supplier: '',
  items: [],
})

async function loadData() {
  const data = await getStockInPage(query)
  orderList.value = data.records
  total.value = data.total
}

async function loadOptions() {
  warehouseList.value = await getWarehouseList()
  const equipments = await getEquipmentPage({ current: 1, size: 100 })
  equipmentOptions.value = equipments.records
}

function openCreate() {
  form.warehouseId = warehouseList.value[0]?.id ?? 0
  form.inType = 1
  form.supplier = ''
  form.items = [{ equipmentId: equipmentOptions.value[0]?.id ?? 0, quantity: 1, unitPrice: 0 }]
  createVisible.value = true
}

function addRow() {
  form.items.push({ equipmentId: equipmentOptions.value[0]?.id ?? 0, quantity: 1, unitPrice: 0 })
}

async function handleCreate() {
  if (!form.warehouseId || form.items.some((i) => !i.equipmentId || !i.quantity)) {
    ElMessage.warning('请完整填写仓库与明细')
    return
  }
  await createStockIn({ ...form })
  ElMessage.success('已保存草稿')
  createVisible.value = false
  await loadData()
}

async function handleSubmit(row: StockInOrder) {
  await submitStockIn(row.id)
  ElMessage.success('已提交审核')
  await loadData()
}

async function handleAudit(row: StockInOrder, pass: boolean) {
  const { value } = await ElMessageBox.prompt(pass ? '请输入审核意见' : '请输入驳回原因', pass ? '审核通过' : '驳回', {
    inputValidator: (v) => (v ? true : '请输入意见'),
  })
  await auditStockIn(row.id, pass, value)
  ElMessage.success(pass ? '已通过' : '已驳回')
  await loadData()
}

async function handleReceive(row: StockInOrder) {
  await ElMessageBox.confirm(`确认验收入库单 ${row.orderNo}？入库后库存立即增加`, '验收确认', { type: 'warning' })
  await receiveStockIn(row.id)
  ElMessage.success('验收完成，库存已入账')
  await loadData()
}

async function handleCancel(row: StockInOrder) {
  await ElMessageBox.confirm(`确认作废入库单 ${row.orderNo}？`, '提示', { type: 'warning' })
  await cancelStockIn(row.id)
  ElMessage.success('已作废')
  await loadData()
}

async function handleDelete(row: StockInOrder) {
  await ElMessageBox.confirm(`确认删除草稿 ${row.orderNo}？`, '提示', { type: 'warning' })
  await deleteStockIn(row.id)
  ElMessage.success('已删除')
  await loadData()
}

async function handleDetail(row: StockInOrder) {
  detail.value = await getStockInDetail(row.id)
  detailVisible.value = true
}

onMounted(() => {
  loadData()
  loadOptions()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.items {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.item-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
