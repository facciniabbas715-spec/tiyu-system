<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.orderNo" placeholder="报废单号" clearable style="width: 190px" @keyup.enter="loadData" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 150px">
        <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="Number(key)" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button v-permission="['scrap:add']" type="success" @click="openCreate">申请报废</el-button>
    </div>

    <el-table :data="orderList" border>
      <el-table-column prop="orderNo" label="报废单号" width="180" />
      <el-table-column prop="warehouseName" label="仓库" width="110" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ scrapTypeMap[row.scrapType] }}</template>
      </el-table-column>
      <el-table-column prop="totalQuantity" label="数量" width="70" />
      <el-table-column prop="totalLossAmount" label="损失金额" width="110" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="['scrap:audit']" v-if="row.status === 0" link type="primary" @click="handleAudit(row, true)">通过</el-button>
          <el-button v-permission="['scrap:audit']" v-if="row.status === 0" link type="danger" @click="handleAudit(row, false)">驳回</el-button>
          <el-button v-permission="['scrap:dispose']" v-if="row.status === 1" link type="success" @click="handleDispose(row)">处置</el-button>
          <el-button v-permission="['scrap:add']" v-if="[0, 3].includes(row.status)" link type="warning" @click="handleCancel(row)">作废</el-button>
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

    <el-dialog v-model="createVisible" title="申请报废" width="680px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="仓库">
          <el-select v-model="form.warehouseId" style="width: 200px">
            <el-option v-for="w in warehouseList" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报废类型">
          <el-select v-model="form.scrapType" style="width: 200px">
            <el-option label="自然损耗" :value="1" />
            <el-option label="损坏报废" :value="2" />
            <el-option label="到期报废" :value="3" />
            <el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="报废明细">
          <div class="items">
            <div v-for="(item, index) in form.items" :key="index" class="item-row">
              <el-select v-model="item.equipmentId" filterable placeholder="器材" style="width: 220px">
                <el-option v-for="e in equipmentOptions" :key="e.id" :label="`${e.equipmentName} (${e.equipmentCode})`" :value="e.id" />
              </el-select>
              <el-input-number v-model="item.quantity" :min="1" placeholder="数量" style="width: 110px" />
              <el-input v-model="item.scrapReason" placeholder="报废原因" style="width: 160px" />
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

    <el-dialog v-model="detailVisible" title="报废单详情" width="720px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="单号">{{ detail?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail?.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail ? statusMap[detail.status] : '' }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail ? scrapTypeMap[detail.scrapType] : '' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ detail?.totalQuantity }}</el-descriptions-item>
        <el-descriptions-item label="损失">{{ detail?.totalLossAmount }}</el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="3">{{ detail?.auditRemark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.items || []" border style="margin-top: 12px">
        <el-table-column prop="equipmentCode" label="编码" width="150" />
        <el-table-column prop="equipmentName" label="名称" min-width="120" />
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column prop="scrapReason" label="原因" min-width="140" />
        <el-table-column prop="lossAmount" label="损失" width="100" />
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  auditScrap,
  cancelScrap,
  createScrap,
  disposeScrap,
  getScrapDetail,
  getScrapPage,
  type ScrapOrder,
} from '@/api/business/scrap'
import { getEquipmentPage, type EquipmentItem } from '@/api/equipment/equipment'
import { getWarehouseList, type WarehouseItem } from '@/api/equipment/warehouse'

const statusMap: Record<number, string> = { 0: '待审核', 1: '已审核', 2: '已处置', 3: '已驳回', 4: '已作废' }
const scrapTypeMap: Record<number, string> = { 1: '自然损耗', 2: '损坏报废', 3: '到期报废', 4: '其他' }
function statusTag(status: number) {
  return ({ 0: 'warning', 1: 'primary', 2: 'success', 3: 'danger', 4: 'info' } as Record<number, string>)[status] || 'info'
}

const orderList = ref<ScrapOrder[]>([])
const warehouseList = ref<WarehouseItem[]>([])
const equipmentOptions = ref<EquipmentItem[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, orderNo: '', status: undefined as number | undefined })
const createVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<ScrapOrder | null>(null)
const form = reactive<{ warehouseId: number; scrapType: number; items: { equipmentId: number; quantity: number; scrapReason: string }[] }>({
  warehouseId: 0,
  scrapType: 1,
  items: [],
})

async function loadData() {
  const data = await getScrapPage(query)
  orderList.value = data.records
  total.value = data.total
}

async function loadOptions() {
  warehouseList.value = await getWarehouseList()
  equipmentOptions.value = (await getEquipmentPage({ current: 1, size: 100 })).records
}

function openCreate() {
  form.warehouseId = warehouseList.value[0]?.id ?? 0
  form.scrapType = 1
  form.items = [{ equipmentId: equipmentOptions.value[0]?.id ?? 0, quantity: 1, scrapReason: '' }]
  createVisible.value = true
}

function addRow() {
  form.items.push({ equipmentId: equipmentOptions.value[0]?.id ?? 0, quantity: 1, scrapReason: '' })
}

async function handleCreate() {
  if (!form.warehouseId || form.items.some((i) => !i.equipmentId || !i.quantity || !i.scrapReason)) {
    ElMessage.warning('请完整填写仓库与明细')
    return
  }
  await createScrap({ ...form })
  ElMessage.success('报废申请已提交')
  createVisible.value = false
  await loadData()
}

async function handleAudit(row: ScrapOrder, pass: boolean) {
  const { value } = await ElMessageBox.prompt(pass ? '请输入审核意见' : '请输入驳回原因', pass ? '审核通过' : '驳回', {
    inputValidator: (v) => (v ? true : '请输入意见'),
  })
  await auditScrap(row.id, pass, value)
  ElMessage.success(pass ? '已通过，可执行处置' : '已驳回')
  await loadData()
}

async function handleDispose(row: ScrapOrder) {
  const { value } = await ElMessageBox.prompt('选择处置方式（1销毁 2变卖 3捐赠 4回收）', '报废处置', {
    inputPattern: /^[1-4]$/,
    inputErrorMessage: '请输入1-4',
  })
  await ElMessageBox.confirm(`确认处置报废单 ${row.orderNo}？库存将扣减`, '处置确认', { type: 'warning' })
  await disposeScrap(row.id, Number(value))
  ElMessage.success('处置完成，库存已扣减')
  await loadData()
}

async function handleCancel(row: ScrapOrder) {
  await ElMessageBox.confirm(`确认作废报废单 ${row.orderNo}？`, '提示', { type: 'warning' })
  await cancelScrap(row.id)
  ElMessage.success('已作废')
  await loadData()
}

async function handleDetail(row: ScrapOrder) {
  detail.value = await getScrapDetail(row.id)
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
