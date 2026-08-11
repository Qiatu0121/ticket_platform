<template>
  <div>
    <h2 class="page-title">我的订单</h2>
    <el-table :data="orders" border v-loading="loading">
      <el-table-column prop="orderNo" label="订单号" width="210" />
      <el-table-column label="金额" width="110">
        <template #default="{ row }">¥{{ row.amount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.status === 'WAIT_PAY'" type="success" @click="pay(row)">去支付</el-button>
          <el-button v-else-if="row.status === 'PAID'" disabled>等待核销</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { myOrders, payOrder } from '../api/order'
import { getUser } from '../utils/user'

const router = useRouter()
const orders = ref([])
const loading = ref(true)

onMounted(async () => {
  if (!getUser()) {
    router.push('/login')
    return
  }
  // userId 由后端从登录 token 解析
  orders.value = await myOrders()
  loading.value = false
})

const formatTime = (t) => (t ? t.replace('T', ' ').slice(0, 16) : '')
const statusText = (s) => ({
  WAIT_PAY: '待支付', PAID: '已支付', CANCELED: '已取消', VERIFIED: '已核销', REFUNDED: '已退款'
})[s] ?? s
const statusType = (s) => ({
  WAIT_PAY: 'warning', PAID: 'success', CANCELED: 'info', VERIFIED: 'primary', REFUNDED: 'info'
})[s] ?? 'info'

async function pay(row) {
  await payOrder(row.id)
  ElMessage.success('支付成功')
  orders.value = await myOrders()
}
</script>

<style scoped>
.page-title { margin-bottom: 16px; }
</style>
