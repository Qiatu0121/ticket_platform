<template>
  <div v-if="vo" class="detail">
    <el-page-header class="mb" content="活动详情" @back="$router.back()" />

    <el-card class="mb">
      <h2 class="mb">{{ vo.activity.name }}</h2>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="开始时间">{{ formatTime(vo.activity.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="地点">{{ vo.activity.location }}</el-descriptions-item>
        <el-descriptions-item label="简介">{{ vo.activity.detail }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card>
      <template #header>选择票种</template>
      <el-table :data="vo.tickets" border>
        <el-table-column prop="name" label="票种" />
        <el-table-column label="价格" width="120">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="余票" width="100" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button type="danger" :disabled="row.stock <= 0" @click="grab(row)">
              {{ row.stock <= 0 ? '已抢完' : '立即抢票' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivityDetail } from '../api/activity'
import { grabTicket } from '../api/order'
import { getUser } from '../utils/user'

const route = useRoute()
const router = useRouter()
const vo = ref(null)

onMounted(async () => {
  vo.value = await getActivityDetail(route.params.id)
})

const formatTime = (t) => (t ? t.replace('T', ' ').slice(0, 16) : '')

async function grab(ticket) {
  if (!getUser()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  // userId 由后端从登录 token 解析，前端只传票种 id
  await grabTicket(ticket.id)
  ElMessage.success('抢票成功！请在 15 分钟内完成支付')
  router.push('/orders')
}
</script>

<style scoped>
.detail { max-width: 720px; margin: 0 auto; }
</style>
