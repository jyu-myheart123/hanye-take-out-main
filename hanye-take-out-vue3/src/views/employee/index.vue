<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
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
  role: number
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

// 是否为超级管理员（role=1）。所有权限判断都用这一个变量，避免到处写死账号
const isAdmin = computed(() => userInfoStore.userInfo?.role === 1)
// 当前登录员工id（普通员工只能修改自己）
const currentId = computed(() => userInfoStore.userInfo?.id ?? 0)
/**
 * 修改按钮是否可用：
 *   超级管理员 -> 可以修改任何人
 *   普通员工   -> 只能修改自己那一行
 */
const canEdit = (row: Employee) => isAdmin.value || row.id === currentId.value
/**
 * 禁用/删除按钮是否可用：仅超级管理员，且不能对超管账号操作
 */
const canManage = (row: Employee) => isAdmin.value && row.role !== 1

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
  // 方法层守卫：即使按钮被绕过，普通员工也无法操作别人
  if (!canEdit(row)) {
    ElMessage.error('普通员工只能修改自己的信息')
    return
  }
  router.push({
    name: 'employee_update',
    query: { id: row.id },
  })
}

const changeBtn = async (row: Employee) => {
  // 方法层守卫：仅超级管理员可启停，且超管账号受保护
  if (!canManage(row)) {
    ElMessage.error('权限不足，仅超级管理员可执行该操作')
    return
  }
  await updateEmployeeStatusAPI(row.id)
  await init()
  ElMessage.success('状态修改成功')
}

const deleteBtn = (row: Employee) => {
  // 方法层守卫：仅超级管理员可删除，且超管账号受保护
  if (!canManage(row)) {
    ElMessage.error('权限不足，仅超级管理员可执行该操作')
    return
  }
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
      <!-- 新增员工：仅超级管理员可见，普通员工界面上根本没有这个入口 -->
      <el-button v-if="isAdmin" size="large" type="primary" @click="router.push('/employee/add')">
        <el-icon style="font-size: 15px; margin-right: 10px;">
          <Plus />
        </el-icon>
        添加员工
      </el-button>
    </div>

    <el-table :data="employeeList" stripe>
      <el-table-column prop="name" label="姓名" align="center" />
      <el-table-column prop="account" label="账号" align="center" />
      <!-- 角色列：一眼看出谁是超级管理员 -->
      <el-table-column label="角色" align="center" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.role === 1 ? 'warning' : 'info'" round>
            {{ scope.row.role === 1 ? '超级管理员' : '普通员工' }}
          </el-tag>
        </template>
      </el-table-column>
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
          <!-- 修改：超管可改所有人，普通员工只能改自己 -->
          <el-button
            type="primary"
            :disabled="!canEdit(scope.row)"
            @click="updateBtn(scope.row)"
          >
            修改
          </el-button>
          <!-- 禁用/启用、删除：仅超级管理员可见，且超管账号受保护不显示 -->
          <el-button
            v-if="canManage(scope.row)"
            plain
            :type="scope.row.status === 1 ? 'danger' : 'primary'"
            @click="changeBtn(scope.row)"
          >
            {{ scope.row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button
            v-if="canManage(scope.row)"
            type="danger"
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
