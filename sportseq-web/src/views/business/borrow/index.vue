<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.orderNo" placeholder="借用单号" clearable style="width: 190px" @keyup.enter="loadData" />
      <el-input v-model="query.username" placeholder="借用人" clearable style="width: 130px" @keyup.enter="loadData" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
        <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="Number(key)" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button v-permission="['borrow:add']" type="success" @click="openCreate">新建借用</el-button>
    </div>

    <el-table :data="orderList" border>
      <el-table-column prop="orderNo" label="借用单号" width="160" />
      <el-table-column prop="realName" label="借用人" width="100" />
      <el-table-column prop="purpose" label="用途" min-width="120" show-overflow-tooltip />
      <el-table-column prop="expectedReturnDate" label="预计归还" width="110" />
      <el-table-column prop="totalQuantity" label="数量" width="70" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="['borrow:audit']" v-if="row.status === 0" link type="primary" @click="handleAudit(row, true)">通过</el-button>
          <el-button v-permission="['borrow:audit']" v-if="row.status === 0" link type="danger" @click="handleAudit(row, false)">驳回</el-button>
          <el-button v-permission="['borrow:issue']" v-if="row.status === 1" link type="success" @click="handleIssue(row)">领用发放</el-button>
          <el-button v-permission="['borrow:extend']" v-if="row.status === 2" link type="primary" @click="handleExtend(row)">续借</el-button>
          <el-button v-permission="['borrow:cancel']" v-if="row.status === 0" link type="warning" @click="handleCancel(row)">取消</el-button>
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

    <el-dialog v-model="createVisible" title="新建借用申请" width="680px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="借用类型">
          <el-radio-group v-model="form.borrowType">
            <el-radio-button :value="1">个人借用</el-radio-button>
            <el-radio-button :value="2">集体领用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用途">
          <el-input v-model="form.purpose" placeholder="请说明借用用途" />
        </el-form-item>
        <el-form-item label="预计归还日">
          <el-date-picker v-model="form.expectedReturnDate" type="date" value-format="YYYY-MM-DD" style="width: 200px" />
        </el-form-item>
        <el-form-item label="借用明细">
          <div class="items">
            <div v-for="(item, index) in form.items" :key="index" class="item-row">
              <el-select v-model="item.equipmentId" filterable placeholder="器材" style="width: 220px">
                <el-option v-for="e in equipmentOptions" :key="e.id" :label="`${e.equipmentName} (${e.equipmentCode})`" :value="e.id" />
              </el-select>
              <el-select v-model="item.warehouseId" placeholder="仓库" style="width: 130px">
                <el-option v-for="w in warehouseList" :key="w.id" :label="w.warehouseName" :value="w.id" />
              </el-select>
              <el-input-number v-model="item.quantity" :min="1" placeholder="数量" style="width: 110px" />
              <el-button type="danger" link @click="form.items.splice(index, 1)">移除</el-button>
            </div>
            <el-button type="primary" plain size="small" @click="addRow">添加明细</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="借用单详情" width="760px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="单号">{{ detail?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="借用人">{{ detail?.realName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail ? statusMap[detail.status] : '' }}</el-descriptions-item>
        <el-descriptions-item label="用途" :span="2">{{ detail?.purpose }}</el-descriptions-item>
        <el-descriptions-item label="预计归还">{{ detail?.expectedReturnDate }}</el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="3">{{ detail?.auditRemark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.items || []" border style="margin-top: 12px">
        <el-table-column prop="equipmentCode" label="编码" width="150" />
        <el-table-column prop="equipmentName" label="名称" min-width="120" />
        <el-table-column prop="warehouseName" label="仓库" width="110" />
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column prop="issuedQuantity" label="已领" width="70" />
        <el-table-column prop="returnedQuantity" label="已还" width="70" />
        <el-table-column label="逾期" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.overdueFlag === 1" type="danger">{{ row.overdueDays }}天</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  auditBorrow,
  cancelBorrow,
  createBorrow,
  extendBorrow,
  getBorrowDetail,
  getBorrowPage,
  issueBorrow,
  type BorrowOrder,
} from '@/api/business/borrow'
import { getEquipmentPage, type EquipmentItem } from '@/api/equipment/equipment'
import { getWarehouseList, type WarehouseItem } from '@/api/equipment/warehouse'

const statusMap: Record<number, string> = {
  0: '待审核',
  1: '已通过',
  2: '借用中',
  3: '部分归还',
  4: '已归还',
  5: '已驳回',
  6: '已取消',
}
function statusTag(status: number) {
  return ({ 0: 'warning', 1: 'primary', 2: 'success', 3: '', 4: 'info', 5: 'danger', 6: 'info' } as Record<number, string>)[status] || 'info'
}

const orderList = ref<BorrowOrder[]>([])
const equipmentOptions = ref<EquipmentItem[]>([])
const warehouseList = ref<WarehouseItem[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, orderNo: '', username: '', status: undefined as number | undefined })
const createVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<BorrowOrder | null>(null)
const form = reactive<{
  borrowType: number
  purpose: string
  expectedReturnDate: string
  items: { equipmentId: number; warehouseId: number; quantity: number }[]
}>({ borrowType: 1, purpose: '', expectedReturnDate: '', items: [] })

async function loadData() {
  const data = await getBorrowPage(query)
  orderList.value = data.records
  total.value = data.total
}

async function loadOptions() {
  equipmentOptions.value = (await getEquipmentPage({ current: 1, size: 100 })).records
  warehouseList.value = await getWarehouseList()
}

function openCreate() {
  form.borrowType = 1
  form.purpose = ''
  form.expectedReturnDate = ''
  form.items = [{ equipmentId: equipmentOptions.value[0]?.id ?? 0, warehouseId: warehouseList.value[0]?.id ?? 0, quantity: 1 }]
  createVisible.value = true
}

function addRow() {
  form.items.push({ equipmentId: equipmentOptions.value[0]?.id ?? 0, warehouseId: warehouseList.value[0]?.id ?? 0, quantity: 1 })
}

async function handleCreate() {
  if (!form.purpose || !form.expectedReturnDate || form.items.some((i) => !i.equipmentId || !i.warehouseId || !i.quantity)) {
    ElMessage.warning('请完整填写用途、归还日期与明细')
    return
  }
  await createBorrow({ ...form })
  ElMessage.success('申请已提交，库存已锁定')
  createVisible.value = false
  await loadData()
}

async function handleAudit(row: BorrowOrder, pass: boolean) {
  const { value } = await ElMessageBox.prompt(pass ? '请输入审核意见' : '请输入驳回原因', pass ? '审核通过' : '驳回', {
    inputValidator: (v) => (v ? true : '请输入意见'),
  })
  await auditBorrow(row.id, pass, value)
  ElMessage.success(pass ? '已通过' : '已驳回，库存已解锁')
  await loadData()
}

async function handleIssue(row: BorrowOrder) {
  await ElMessageBox.confirm(`确认向借用人发放器材？库存将扣减`, '领用发放', { type: 'warning' })
  await issueBorrow(row.id)
  ElMessage.success('发放完成')
  await loadData()
}

async function handleExtend(row: BorrowOrder) {
  const { value } = await ElMessageBox.prompt('请输入续借天数（最多90天）', '续借', {
    inputPattern: /^([1-9]|[1-8][0-9]|90)$/,
    inputErrorMessage: '请输入1-90的整数',
  })
  await extendBorrow(row.id, Number(value))
  ElMessage.success('续借成功')
  await loadData()
}

async function handleCancel(row: BorrowOrder) {
  await ElMessageBox.confirm(`确认取消借用单 ${row.orderNo}？库存将解锁`, '提示', { type: 'warning' })
  await cancelBorrow(row.id)
  ElMessage.success('已取消')
  await loadData()
}

async function handleDetail(row: BorrowOrder) {
  detail.value = await getBorrowDetail(row.id)
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
  flex-wrap: wrap;
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
