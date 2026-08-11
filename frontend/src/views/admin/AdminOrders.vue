<template>
  <div>
    <h2 class="page-title">管理端 · 订单核销</h2>

    <div class="mb">
      <el-radio-group v-model="status" @change="load">
        <el-radio-button label="PAID">已支付（待核销）</el-radio-button>
        <el-radio-button label="VERIFIED">已核销</el-radio-button>
        <el-radio-button label="">全部</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="orders" border v-loading="loading">
      <el-table-column prop="orderNo" label="订单号" width="210" />
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column label="金额" width="110">
        <template #default="{ row }">¥{{ row.amount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PAID' ? 'success' : 'info'">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PAID'" type="warning" @click="verify(row)">核销</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listOrders } from '../../api/admin'
import { verifyOrder } from '../../api/order'

const status = ref('PAID')
const orders = ref([])
const loading = ref(true)

onMounted(load)

async function load() {
  loading.value = true
  try {
    orders.value = await listOrders(status.value)
  } finally {
    loading.value = false
  }
}

async function verify(row) {
  await verifyOrder(row.id)
  ElMessage.success('核销成功')
  load()
}

const formatTime = (t) => (t ? t.replace('T', ' ').slice(0, 16) : '')
const statusText = (s) => ({
  WAIT_PAY: '待支付', PAID: '已支付', CANCELED: '已取消', VERIFIED: '已核销', REFUNDED: '已退款'
})[s] ?? s
</script>

<style scoped>
.page-title { margin-bottom: 16px; }
</style>
