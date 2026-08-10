<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.equipmentName" placeholder="器材名称" clearable style="width: 150px" @keyup.enter="loadData" />
      <el-select v-model="query.warehouseId" placeholder="仓库" clearable style="width: 140px">
        <el-option v-for="w in warehouseList" :key="w.id" :label="w.warehouseName" :value="w.id" />
      </el-select>
      <el-checkbox v-model="query.warningOnly" label="仅看预警" @change="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button v-permission="['stock:adjust']" type="success" @click="openAdjust">库存调整</el-button>
    </div>

    <el-table :data="stockList" border>
      <el-table-column prop="equipmentCode" label="器材编码" width="150" />
      <el-table-column prop="equipmentName" label="器材名称" min-width="130" />
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column prop="quantity" label="在库数量" width="90" />
      <el-table-column prop="lockedQuantity" label="锁定数量" width="90" />
      <el-table-column prop="safeStock" label="安全库存" width="90" />
      <el-table-column label="预警" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.warning" type="danger">预警</el-tag>
          <el-tag v-else type="success">正常</el-tag>
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

    <el-dialog v-model="adjustVisible" title="库存调整（盘盈为正、盘亏为负）" width="480px">
      <el-form :model="adjustForm" label-width="90px">
        <el-form-item label="器材">
          <el-select v-model="adjustForm.equipmentId" filterable style="width: 100%">
            <el-option v-for="e in equipmentOptions" :key="e.id" :label="`${e.equipmentName} (${e.equipmentCode})`" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="adjustForm.warehouseId" style="width: 100%">
            <el-option v-for="w in warehouseList" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="调整数量">
          <el-input-number v-model="adjustForm.changeQuantity" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="adjustForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdjust">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adjustStock, getStockPage, type StockItem } from '@/api/stock/stock'
import { getEquipmentPage, type EquipmentItem } from '@/api/equipment/equipment'
import { getWarehouseList, type WarehouseItem } from '@/api/equipment/warehouse'

const stockList = ref<StockItem[]>([])
const warehouseList = ref<WarehouseItem[]>([])
const equipmentOptions = ref<EquipmentItem[]>([])
const total = ref(0)
const query = reactive({
  current: 1,
  size: 10,
  equipmentName: '',
  warehouseId: undefined as number | undefined,
  warningOnly: false,
})
const adjustVisible = ref(false)
const adjustForm = reactive({ equipmentId: 0, warehouseId: 0, changeQuantity: 1, remark: '' })

async function loadData() {
  const data = await getStockPage(query)
  stockList.value = data.records
  total.value = data.total
}

async function loadOptions() {
  warehouseList.value = await getWarehouseList()
  const equipments = await getEquipmentPage({ current: 1, size: 100 })
  equipmentOptions.value = equipments.records
}

async function openAdjust() {
  adjustForm.equipmentId = equipmentOptions.value[0]?.id ?? 0
  adjustForm.warehouseId = warehouseList.value[0]?.id ?? 0
  adjustForm.changeQuantity = 1
  adjustForm.remark = ''
  adjustVisible.value = true
}

async function handleAdjust() {
  if (!adjustForm.equipmentId || !adjustForm.warehouseId || adjustForm.changeQuantity === 0) {
    ElMessage.warning('请选择器材、仓库并填写非零数量')
    return
  }
  await adjustStock({ ...adjustForm })
  ElMessage.success('调整成功')
  adjustVisible.value = false
  await loadData()
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
  align-items: center;
}
</style>
