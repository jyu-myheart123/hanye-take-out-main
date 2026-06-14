<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteEmployeeAPI, getEmployeePageListAPI, updateEmployeeStatusAPI } from '@/api/employee'
import { useUserInfoStore } from '@/store'

interface Employee {
  id: number
  name: string
  account: string
  phone: string
  age: number
  gender: string
  pic: string
  status: number
  updateTime: string
}

const router = useRouter()
const userInfoStore = useUserInfoStore()
const employeeList = ref<Employee[]>([])
const pageData = reactive({
  name: '',
  page: 1,
  pageSize: 6,
  total: 0,
})

const init = async () => {
  const { data: res } = await getEmployeePageListAPI({
    page: pageData.page,
    pageSize: pageData.pageSize,
    name: pageData.name,
  })
  employeeList.value = res.data.records
  pageData.total = res.data.total
}

init()

const handleCurrentChange = (val: number) => {
  pageData.page = val
  init()
}

const handleSizeChange = (val: number) => {
  pageData.pageSize = val
  init()
}

const updateBtn = (row: Employee) => {
  router.push({
    name: 'employee_update',
    query: { id: row.id },
  })
}

const changeBtn = async (row: Employee) => {
  await updateEmployeeStatusAPI(row.id)
  await init()
  ElMessage.success('状态修改成功')
}

const deleteBtn = (row: Employee) => {
  ElMessageBox.confirm('该操作会永久删除该员工，是否继续？', '删除员工', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await deleteEmployeeAPI(row.id)
      await init()
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
        <h2>员工管理</h2>
        <p>维护后台账号、人员资料与启停状态，保留现有接口和权限判断。</p>
      </div>
      <div class="page-head__stats">
        <span class="page-stat">当前页 <strong>{{ pageData.page }}</strong></span>
        <span class="page-stat">员工总数 <strong>{{ pageData.total }}</strong></span>
      </div>
    </div>

    <div class="page-toolbar">
      <el-input v-model="pageData.name" size="large" placeholder="输入员工姓名搜索" clearable />
      <div class="toolbar-spacer"></div>
      <el-button size="large" type="success" @click="init()">查询员工</el-button>
      <el-button size="large" type="primary" @click="router.push('/employee/add')">
        <el-icon style="font-size: 15px; margin-right: 10px;">
          <Plus />
        </el-icon>
        添加员工
      </el-button>
    </div>

    <el-table :data="employeeList" stripe>
      <el-table-column prop="name" label="姓名" align="center" />
      <el-table-column prop="account" label="账号" align="center" />
      <el-table-column prop="phone" label="手机号" width="140" align="center" />
      <el-table-column prop="age" label="年龄" align="center" />
      <el-table-column prop="gender" label="性别" align="center" />
      <el-table-column prop="pic" label="头像" align="center">
        <template #default="scope">
          <img v-if="scope.row.pic" :src="scope.row.pic" alt="" class="page-table-image" />
          <img v-else src="/src/assets/image/user_default.png" alt="" class="page-table-image" />
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" round>
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="最近操作时间" width="180" align="center" />
      <el-table-column label="操作" width="240" align="center">
        <template #default="scope">
          <el-button
            type="primary"
            @click="updateBtn(scope.row)"
            :disabled="
              userInfoStore.userInfo?.account !== 'cyh' &&
              userInfoStore.userInfo?.account !== scope.row.account
            "
          >
            修改
          </el-button>
          <el-button
            plain
            :type="scope.row.status === 1 ? 'danger' : 'primary'"
            :disabled="userInfoStore.userInfo?.account !== 'cyh'"
            @click="changeBtn(scope.row)"
          >
            {{ scope.row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button
            type="danger"
            :disabled="userInfoStore.userInfo?.account !== 'cyh'"
            @click="deleteBtn(scope.row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无员工数据" />
      </template>
    </el-table>

    <el-pagination
      class="page"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="pageData.total"
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
