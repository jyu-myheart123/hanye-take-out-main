<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElTable } from 'element-plus'
import { deleteSetmealsAPI, getSetmealPageListAPI, updateSetmealStatusAPI } from '@/api/setmeal'
import { getCategoryPageListAPI } from '@/api/category'

interface Setmeal {
  id: number
  name: string
  pic: string
  detail: string
  price: number
  status: number
  categoryId: number
  updateTime: string
}

interface Category {
  id: number
  name: string
}

const router = useRouter()
const setmealList = ref<Setmeal[]>([])
const categoryList = ref<Category[]>([])
const total = ref(0)
const multiTableRef = ref<InstanceType<typeof ElTable>>()
const multiSelection = ref<Setmeal[]>([])

const pageData = reactive({
  name: '',
  categoryId: '',
  status: '',
  page: 1,
  pageSize: 6,
})

const options = [
  { value: '1', label: '起售' },
  { value: '0', label: '停售' },
]

const init = async () => {
  const { data: resCategory } = await getCategoryPageListAPI({ page: 1, pageSize: 100, type: 2 })
  categoryList.value = resCategory.data.records
}

const showPageList = async () => {
  const { data: res } = await getSetmealPageListAPI(pageData)
  setmealList.value = res.data.records
  total.value = res.data.total
}

init()
showPageList()

const handleCurrentChange = (val: number) => {
  pageData.page = val
  showPageList()
}

const handleSizeChange = (val: number) => {
  pageData.pageSize = val
  showPageList()
}

const handleSelectionChange = (val: Setmeal[]) => {
  multiSelection.value = val
}

const toAddUpdate = (row?: Setmeal) => {
  if (row?.id) {
    router.push({
      path: '/setmeal/add',
      query: { id: row.id },
    })
    return
  }
  router.push('/setmeal/add')
}

const changeBtn = async (row: Setmeal) => {
  await updateSetmealStatusAPI(row.id)
  await showPageList()
  ElMessage.success('状态修改成功')
}

const deleteBatch = (row?: Setmeal) => {
  ElMessageBox.confirm('该操作会永久删除套餐，是否继续？', '删除套餐', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      if (!row) {
        if (multiSelection.value.length === 0) {
          ElMessage.warning('请先勾选需要删除的套餐')
          return
        }
        const ids = multiSelection.value.map((item) => item.id).join(',')
        const res = await deleteSetmealsAPI(ids)
        if (res.data.code !== 0) return
      } else {
        const res = await deleteSetmealsAPI(String(row.id))
        if (res.data.code !== 0) return
      }
      await showPageList()
      ElMessage.success('删除成功')
    })
    .catch(() => {
      ElMessage.info('已取消删除')
    })
}
</script>

<template>
  <el-card class="page-card">
    <div class="page-head">
      <div class="page-head__meta">
        <h2>套餐管理</h2>
        <p>更集中地查看套餐图片、价格、分类与上下架状态，支持批量操作。</p>
      </div>
      <div class="page-head__stats">
        <span class="page-stat">套餐总数 <strong>{{ total }}</strong></span>
        <span class="page-stat">已选条目 <strong>{{ multiSelection.length }}</strong></span>
      </div>
    </div>

    <div class="page-toolbar">
      <el-input v-model="pageData.name" size="large" placeholder="输入套餐名称搜索" clearable />
      <el-select v-model="pageData.categoryId" size="large" clearable placeholder="选择分类">
        <el-option v-for="item in categoryList" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="pageData.status" size="large" clearable placeholder="选择售卖状态">
        <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <div class="toolbar-spacer"></div>
      <el-button size="large" type="success" @click="showPageList()">查询套餐</el-button>
      <el-button size="large" type="danger" plain @click="deleteBatch()">批量删除</el-button>
      <el-button size="large" type="primary" @click="toAddUpdate()">
        <el-icon style="font-size: 15px; margin-right: 10px;">
          <Plus />
        </el-icon>
        添加套餐
      </el-button>
    </div>

    <el-table ref="multiTableRef" :data="setmealList" stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="name" label="套餐名称" align="center" />
      <el-table-column prop="pic" label="图片" align="center">
        <template #default="scope">
          <img v-if="scope.row.pic" :src="scope.row.pic" alt="" class="page-table-image" />
          <img v-else src="/src/assets/image/user_default.png" alt="" class="page-table-image" />
        </template>
      </el-table-column>
      <el-table-column prop="detail" label="详情" width="200" align="center" show-overflow-tooltip />
      <el-table-column prop="price" label="价格" align="center" />
      <el-table-column prop="status" label="状态" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" round>
            {{ scope.row.status === 1 ? '起售' : '停售' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="categoryId" label="所属分类" width="140" align="center">
        <template #default="scope">
          {{ categoryList.find((item) => item.id === scope.row.categoryId)?.name }}
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="最近操作时间" width="180" align="center" />
      <el-table-column label="操作" width="220" align="center">
        <template #default="scope">
          <el-button type="primary" @click="toAddUpdate(scope.row)">修改</el-button>
          <el-button plain :type="scope.row.status === 1 ? 'danger' : 'primary'" @click="changeBtn(scope.row)">
            {{ scope.row.status === 1 ? '停售' : '起售' }}
          </el-button>
          <el-button type="danger" @click="deleteBatch(scope.row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无套餐数据" />
      </template>
    </el-table>

    <el-pagination
      class="page"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      :page-sizes="[2, 4, 6, 8]"
      v-model:current-page="pageData.page"
      v-model:page-size="pageData.pageSize"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    />
  </el-card>
</template>

<style lang="less" scoped>
.page-card {
  :deep(.el-card__body) {
    padding: 28px;
  }
}

:deep(.el-table tr) {
  font-size: 13px;
}

:deep(.el-table .el-button) {
  min-width: 56px;
}

.el-pagination {
  justify-content: center;
}
</style>
