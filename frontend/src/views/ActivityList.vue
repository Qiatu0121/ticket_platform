<template>
  <div>
    <h2 class="page-title">最新活动</h2>
    <el-empty v-if="!loading && activities.length === 0" :description="emptyText" />
    <el-row :gutter="16">
      <el-col v-for="a in activities" :key="a.id" :xs="24" :sm="12" :md="8" class="mb">
        <el-card shadow="hover">
          <h3>{{ a.name }}</h3>
          <p class="meta">🕐 {{ formatTime(a.startTime) }}</p>
          <p class="meta">📍 {{ a.location }}</p>
          <div class="card-foot">
            <el-tag :type="statusType(a.status)">{{ statusText(a.status) }}</el-tag>
            <el-button type="primary" size="small" @click="goDetail(a.id)">查看详情</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listActivities } from '../api/activity'
import { getUser } from '../utils/user'

const router = useRouter()
const activities = ref([])
const loading = ref(true)

// 空态提示按角色区分：只有管理员有权限去发布，普通用户引导等待
const emptyText = computed(() =>
  getUser()?.role === 'ADMIN' ? '暂无活动，去管理端发布一个吧' : '暂无活动，请耐心等待'
)

onMounted(async () => {
  activities.value = await listActivities()
  loading.value = false
})

const formatTime = (t) => (t ? t.replace('T', ' ').slice(0, 16) : '')
const statusText = (s) => ({ 0: '未开始', 1: '进行中', 2: '已结束' })[s] ?? '未知'
const statusType = (s) => ({ 0: 'info', 1: 'success', 2: 'warning' })[s] ?? 'info'
const goDetail = (id) => router.push(`/activity/${id}`)
</script>

<style scoped>
.page-title { margin-bottom: 16px; }
.meta { margin: 4px 0; color: #909399; font-size: 13px; }
.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}
</style>
