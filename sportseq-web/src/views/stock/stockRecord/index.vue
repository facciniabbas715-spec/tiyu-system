<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="query.changeType" placeholder="变动类型" clearable style="width: 140px">
        <el-option v-for="(label, key) in changeTypes" :key="key" :label="label" :value="Number(key)" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <el-table :data="recordList" border>
      <el-table-column prop="createTime" label="操作时间" width="170" />
      <el-table-column prop="equipmentCode" label="器材编码" width="150" />
      <el-table-column prop="equipmentName" label="器材名称" min-width="130" />
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.changeQuantity > 0 ? 'success' : 'danger'">
            {{ changeTypes[row.changeType] || row.changeType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="变动数量" width="90">
        <template #default="{ row }">
          <span :style="{ color: row.changeQuantity > 0 ? '#67c23a' : '#f56c6c' }">
            {{ row.changeQuantity > 0 ? `+${row.changeQuantity}` : row.changeQuantity }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="beforeQuantity" label="变动前" width="80" />
      <el-table-column prop="afterQuantity" label="变动后" width="80" />
      <el-table-column prop="refOrderNo" label="来源单据" width="170" />
      <el-table-column prop="remark" label="备注" min-width="120" />
    </el-table>

    <el-pagination
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 14px; justify-content: flex-end"
      @change="loadData"
    />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getStockRecordPage, type StockRecordItem } from '@/api/stock/stock'

const changeTypes: Record<number, string> = {
  1: '入库',
  2: '借用出库',
  3: '归还入库',
  4: '报废出库',
  5: '盘盈',
  6: '盘亏',
  7: '锁定',
  8: '解锁',
  9: '领用核销',
}

const recordList = ref<StockRecordItem[]>([])
const total = ref(0)
const query = reactive({
  current: 1,
  size: 10,
  changeType: undefined as number | undefined,
})

async function loadData() {
  const data = await getStockRecordPage(query)
  recordList.value = data.records
  total.value = data.total
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
