<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSupplierListAPI,
  saveSupplierAPI,
  deleteSupplierAPI,
  getMaterialListAPI,
  saveMaterialAPI,
  deleteMaterialAPI,
  getPurchasePageAPI,
  addPurchaseAPI,
  getMonthStatementAPI,
  getSupplyOverviewAPI
} from '@/api/supply'

// ============================================================
// 公共数据
// ============================================================
const suppliers = ref<any[]>([])
const materials = ref<any[]>([])
const overview = reactive({
  todayAmount: 0,
  todayCount: 0,
  monthAmount: 0,
  monthCount: 0
})

/** 金额统一显示两位小数 */
const fmtMoney = (v: any) => `¥${Number(v || 0).toFixed(2)}`

/** 根据供应商id找供应商名称 */
const supplierName = (id: number) => suppliers.value.find((s) => s.id === id)?.name || '—'

/** 加载概览（今日/本月金额与单数） */
const loadOverview = async () => {
  const { data: res } = await getSupplyOverviewAPI()
  if (res.code === 0) Object.assign(overview, res.data)
}

// ============================================================
// Tab1：供应商管理
// ============================================================
const supplierVisible = ref(false)
const supplierSaving = ref(false)
const supplierForm = reactive<any>({
  id: undefined,
  name: '',
  contactPerson: '',
  phone: '',
  address: '',
  mainCategory: '',
  status: 1,
  remark: ''
})

/** 新建供应商 */
const openSupplierCreate = () => {
  Object.assign(supplierForm, {
    id: undefined, name: '', contactPerson: '', phone: '', address: '',
    mainCategory: '', status: 1, remark: ''
  })
  supplierVisible.value = true
}

/** 编辑供应商 */
const openSupplierEdit = (row: any) => {
  Object.assign(supplierForm, row)
  supplierVisible.value = true
}

/** 保存供应商 */
const saveSupplier = async () => {
  if (!supplierForm.name.trim()) return ElMessage.warning('请填写供应商名称')
  if (supplierForm.phone.trim() && !/^1\d{10}$/.test(supplierForm.phone.trim())) {
    return ElMessage.warning('电话格式不正确，请填写11位手机号')
  }
  supplierSaving.value = true
  try {
    const { data: res } = await saveSupplierAPI({
      ...supplierForm,
      name: supplierForm.name.trim(),
      contactPerson: supplierForm.contactPerson.trim(),
      phone: supplierForm.phone.trim(),
      address: supplierForm.address.trim(),
      mainCategory: supplierForm.mainCategory.trim(),
      remark: supplierForm.remark?.trim() || null
    })
    if (res.code === 0) {
      ElMessage.success('供应商已保存')
      supplierVisible.value = false
      loadSuppliers()
    }
  } finally {
    supplierSaving.value = false
  }
}

/** 删除供应商（有采购记录的后端会拦截，提示改用停用） */
const removeSupplier = (row: any) => {
  ElMessageBox.confirm(`确定删除供应商【${row.name}】吗？若有采购记录将无法删除。`, '删除确认', {
    type: 'warning'
  })
    .then(async () => {
      const { data: res } = await deleteSupplierAPI(row.id)
      if (res.code === 0) {
        ElMessage.success('已删除')
        loadSuppliers()
      }
    })
    .catch(() => {})
}

/** 供应商列表加载（列表本身带本月统计） */
const loadSuppliers = async () => {
  const { data: res } = await getSupplierListAPI()
  if (res.code === 0) suppliers.value = res.data || []
}

// ============================================================
// Tab2：原材料管理
// ============================================================
/** 原材料分类（与预置数据的10个分类对应，方便下拉选择） */
const categoryOptions = ['蔬菜', '肉类', '水产', '粮油', '调料', '蛋品', '豆制品', '乳品', '冻品', '餐饮用品']

const materialVisible = ref(false)
const materialSaving = ref(false)
const materialForm = reactive<any>({
  id: undefined,
  name: '',
  category: '',
  unit: '斤',
  referencePrice: undefined,
  supplierId: undefined,
  status: 1
})

/** 新建原材料 */
const openMaterialCreate = () => {
  Object.assign(materialForm, {
    id: undefined, name: '', category: '', unit: '斤',
    referencePrice: undefined, supplierId: undefined, status: 1
  })
  materialVisible.value = true
}

/** 编辑原材料 */
const openMaterialEdit = (row: any) => {
  Object.assign(materialForm, row)
  materialVisible.value = true
}

/** 保存原材料 */
const saveMaterial = async () => {
  if (!materialForm.name.trim()) return ElMessage.warning('请填写原材料名称')
  if (!materialForm.category) return ElMessage.warning('请选择分类')
  if (!materialForm.unit.trim()) return ElMessage.warning('请填写单位，如 斤/个/块')
  if (!materialForm.referencePrice || Number(materialForm.referencePrice) <= 0) {
    return ElMessage.warning('请填写参考单价')
  }
  materialSaving.value = true
  try {
    const { data: res } = await saveMaterialAPI({
      ...materialForm,
      name: materialForm.name.trim(),
      unit: materialForm.unit.trim()
    })
    if (res.code === 0) {
      ElMessage.success('原材料已保存')
      materialVisible.value = false
      loadMaterials()
    }
  } finally {
    materialSaving.value = false
  }
}

/** 删除原材料 */
const removeMaterial = (row: any) => {
  ElMessageBox.confirm(`确定删除原材料【${row.name}】吗？若有采购记录将无法删除。`, '删除确认', {
    type: 'warning'
  })
    .then(async () => {
      const { data: res } = await deleteMaterialAPI(row.id)
      if (res.code === 0) {
        ElMessage.success('已删除')
        loadMaterials()
      }
    })
    .catch(() => {})
}

/** 原材料列表加载 */
const loadMaterials = async () => {
  const { data: res } = await getMaterialListAPI()
  if (res.code === 0) materials.value = res.data || []
}

// ============================================================
// Tab3：采购记录
// ============================================================
const purchaseQuery = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
  supplierId: undefined as number | undefined,
  materialId: undefined as number | undefined
})
const purchaseList = ref<any[]>([])

/** 加载采购记录分页 */
const loadPurchases = async () => {
  const { data: res } = await getPurchasePageAPI({
    page: purchaseQuery.page,
    pageSize: purchaseQuery.pageSize,
    supplierId: purchaseQuery.supplierId,
    materialId: purchaseQuery.materialId
  })
  if (res.code === 0) {
    purchaseList.value = res.data.records || []
    purchaseQuery.total = res.data.total || 0
  }
}

/** 翻页 */
const onPageChange = (p: number) => {
  purchaseQuery.page = p
  loadPurchases()
}

/** 重置筛选条件 */
const resetPurchaseFilter = () => {
  purchaseQuery.supplierId = undefined
  purchaseQuery.materialId = undefined
  purchaseQuery.page = 1
  loadPurchases()
}

// ----- 登记采购弹窗 -----
const purchaseVisible = ref(false)
const purchaseSaving = ref(false)
const purchaseForm = reactive<any>({
  supplierId: undefined,
  materialId: undefined,
  quantity: undefined,
  unitPrice: undefined,
  purchaseTime: '',
  isOnTime: 1,
  isQualified: 1,
  remark: ''
})

/** 生成当前时间字符串（yyyy-MM-dd HH:mm:ss），给时间选择框当默认值 */
const nowStr = () => {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

/** 打开登记弹窗 */
const openPurchaseCreate = () => {
  Object.assign(purchaseForm, {
    supplierId: undefined, materialId: undefined, quantity: undefined,
    unitPrice: undefined, purchaseTime: nowStr(),
    isOnTime: 1, isQualified: 1, remark: ''
  })
  purchaseVisible.value = true
}

/** 选好供应商后，原料只显示他家供应的（原料和供应商在档案里是绑定的） */
const materialsOfPickedSupplier = computed(() =>
  purchaseForm.supplierId
    ? materials.value.filter((m) => m.supplierId === purchaseForm.supplierId)
    : []
)

/** 选中原料后自动带出参考单价（可改） */
const onMaterialPicked = (id: number) => {
  const m = materials.value.find((x) => x.id === id)
  if (m) purchaseForm.unitPrice = Number(m.referencePrice)
}

/** 本次采购合计 = 数量 × 单价（页面实时预览；后端也会再算一遍） */
const purchaseTotal = computed(() => {
  const q = Number(purchaseForm.quantity || 0)
  const u = Number(purchaseForm.unitPrice || 0)
  return (q * u).toFixed(2)
})

/** 选中原料的单位（数量后面显示用） */
const pickedMaterialUnit = computed(
  () => materials.value.find((m) => m.id === purchaseForm.materialId)?.unit || ''
)

/** 提交采购记录 */
const savePurchase = async () => {
  if (!purchaseForm.supplierId) return ElMessage.warning('请选择供应商')
  if (!purchaseForm.materialId) return ElMessage.warning('请选择原材料')
  if (!purchaseForm.quantity || Number(purchaseForm.quantity) <= 0) return ElMessage.warning('请填写采购数量')
  if (!purchaseForm.unitPrice || Number(purchaseForm.unitPrice) < 0) return ElMessage.warning('请填写正确单价')
  purchaseSaving.value = true
  try {
    const { data: res } = await addPurchaseAPI({ ...purchaseForm, remark: purchaseForm.remark.trim() || null })
    if (res.code === 0) {
      ElMessage.success('采购记录已登记')
      purchaseVisible.value = false
      loadPurchases()
      loadOverview()
      loadSuppliers()
    }
  } finally {
    purchaseSaving.value = false
  }
}

// ============================================================
// Tab4：月底对账
// ============================================================
const statementMonth = ref(nowStr().slice(0, 7)) // 默认本月 yyyy-MM
const statementList = ref<any[]>([])

/** 加载某月对账数据 */
const loadStatement = async () => {
  const { data: res } = await getMonthStatementAPI(statementMonth.value)
  if (res.code === 0) statementList.value = res.data || []
}

/** 表格底部合计：合计送货单数与应付总金额 */
const getSummaries = (param: any) => {
  const { columns, data } = param
  return columns.map((col: any, index: number) => {
    if (index === 0) return '本月合计'
    if (col.property === 'monthCount') {
      return data.reduce((s: number, r: any) => s + Number(r.monthCount), 0) + ' 单'
    }
    if (col.property === 'monthAmount') {
      return fmtMoney(data.reduce((s: number, r: any) => s + Number(r.monthAmount), 0))
    }
    return ''
  })
}

onMounted(async () => {
  await loadSuppliers()
  await loadMaterials()
  loadPurchases()
  loadOverview()
  loadStatement()
})
</script>

<template>
  <div class="supply-page">
    <!-- ===== 顶部概览卡片 ===== -->
    <div class="overview-row">
      <div class="overview-card ov-blue">
        <div class="ov-label">今日采购金额</div>
        <div class="ov-value">{{ fmtMoney(overview.todayAmount) }}</div>
        <div class="ov-sub">共 {{ overview.todayCount }} 笔</div>
      </div>
      <div class="overview-card ov-green">
        <div class="ov-label">本月采购金额</div>
        <div class="ov-value">{{ fmtMoney(overview.monthAmount) }}</div>
        <div class="ov-sub">共 {{ overview.monthCount }} 笔</div>
      </div>
      <div class="overview-card ov-orange">
        <div class="ov-label">供应商数量</div>
        <div class="ov-value">{{ suppliers.length }} <span class="ov-unit">家</span></div>
        <div class="ov-sub">原料 {{ materials.length }} 种</div>
      </div>
    </div>

    <el-tabs type="border-card">
      <!-- ========== Tab1：供应商 ========== -->
      <el-tab-pane label="供应商管理" name="supplier">
        <div class="toolbar">
          <el-button type="primary" @click="openSupplierCreate">＋ 新增供应商</el-button>
        </div>
        <el-table :data="suppliers" stripe>
          <el-table-column label="供应商名称" prop="name" width="170" />
          <el-table-column label="联系人" prop="contactPerson" width="100" />
          <el-table-column label="电话" prop="phone" width="130" />
          <el-table-column label="主营品类" prop="mainCategory" width="110" />
          <el-table-column label="地址" prop="address" min-width="180" show-overflow-tooltip />
          <el-table-column label="本月单数" prop="monthCount" width="90" align="center" />
          <el-table-column label="本月金额" width="110" align="right">
            <template #default="{ row }">
              <b style="color: #e6532b">{{ fmtMoney(row.monthAmount) }}</b>
            </template>
          </el-table-column>
          <el-table-column label="准时率" width="110" align="center">
            <template #default="{ row }">
              <el-progress
                v-if="row.monthCount"
                :percentage="Number(row.onTimeRate || 0)"
                :stroke-width="8"
                :status="Number(row.onTimeRate) >= 90 ? 'success' : Number(row.onTimeRate) >= 60 ? '' : 'exception'"
              />
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="合格率" width="110" align="center">
            <template #default="{ row }">
              <el-progress
                v-if="row.monthCount"
                :percentage="Number(row.qualifiedRate || 0)"
                :stroke-width="8"
                :status="Number(row.qualifiedRate) >= 90 ? 'success' : Number(row.qualifiedRate) >= 60 ? '' : 'exception'"
              />
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" round>
                {{ row.status === 1 ? '合作' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openSupplierEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="removeSupplier(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ========== Tab2：原材料 ========== -->
      <el-tab-pane label="原材料管理" name="material">
        <div class="toolbar">
          <el-button type="primary" @click="openMaterialCreate">＋ 新增原材料</el-button>
        </div>
        <el-table :data="materials" stripe>
          <el-table-column label="原材料名称" prop="name" width="160" />
          <el-table-column label="分类" prop="category" width="100">
            <template #default="{ row }">
              <el-tag size="small" type="warning" round>{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="单位" prop="unit" width="80" align="center" />
          <el-table-column label="参考单价" width="110" align="right">
            <template #default="{ row }">
              <b>{{ Number(row.referencePrice).toFixed(2) }}</b>
              <span class="muted"> /{{ row.unit }}</span>
            </template>
          </el-table-column>
          <el-table-column label="对应供应商" min-width="180">
            <template #default="{ row }">{{ supplierName(row.supplierId) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" round>
                {{ row.status === 1 ? '在用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openMaterialEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="removeMaterial(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ========== Tab3：采购记录 ========== -->
      <el-tab-pane label="采购记录" name="purchase">
        <div class="toolbar">
          <el-select v-model="purchaseQuery.supplierId" placeholder="按供应商筛选" clearable style="width: 200px">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <el-select v-model="purchaseQuery.materialId" placeholder="按原材料筛选" clearable style="width: 200px">
            <el-option v-for="m in materials" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
          <el-button @click="resetPurchaseFilter">重置</el-button>
          <el-button @click="loadPurchases">查询</el-button>
          <el-button type="primary" style="margin-left: auto" @click="openPurchaseCreate">
            ＋ 登记采购
          </el-button>
        </div>
        <el-table :data="purchaseList" stripe>
          <el-table-column label="到货时间" prop="purchaseTime" width="170" />
          <el-table-column label="供应商" prop="supplierName" width="160" />
          <el-table-column label="原材料" prop="materialName" width="130" />
          <el-table-column label="数量" width="110" align="right">
            <template #default="{ row }">
              {{ Number(row.quantity) }} {{ row.materialUnit }}
            </template>
          </el-table-column>
          <el-table-column label="单价" width="100" align="right">
            <template #default="{ row }">{{ Number(row.unitPrice).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="合计金额" width="120" align="right">
            <template #default="{ row }">
              <b style="color: #e6532b">{{ fmtMoney(row.totalAmount) }}</b>
            </template>
          </el-table-column>
          <el-table-column label="准时" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isOnTime === 1 ? 'success' : 'danger'" size="small" round>
                {{ row.isOnTime === 1 ? '准时' : '延迟' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="合格" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isQualified === 1 ? 'success' : 'danger'" size="small" round>
                {{ row.isQualified === 1 ? '合格' : '不合格' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip />
        </el-table>
        <div class="pager">
          <el-pagination
            layout="prev, pager, next, total"
            :total="purchaseQuery.total"
            :page-size="purchaseQuery.pageSize"
            :current-page="purchaseQuery.page"
            @current-change="onPageChange"
          />
        </div>
      </el-tab-pane>

      <!-- ========== Tab4：月底对账 ========== -->
      <el-tab-pane label="月底对账" name="statement">
        <div class="toolbar">
          <span class="muted">选择对账月份：</span>
          <el-date-picker
            v-model="statementMonth"
            type="month"
            value-format="YYYY-MM"
            placeholder="选择月份"
            :clearable="false"
          />
          <el-button type="primary" @click="loadStatement">生成对账单</el-button>
        </div>
        <el-table :data="statementList" stripe show-summary :summary-method="getSummaries">
          <el-table-column label="供应商" prop="name" width="180" />
          <el-table-column label="联系人" prop="contactPerson" width="100" />
          <el-table-column label="电话" prop="phone" width="130" />
          <el-table-column label="送货单数" prop="monthCount" width="100" align="center" />
          <el-table-column label="应付金额" prop="monthAmount" width="140" align="right">
            <template #default="{ row }">
              <b style="color: #e6532b; font-size: 15px">{{ fmtMoney(row.monthAmount) }}</b>
            </template>
          </el-table-column>
          <el-table-column label="准时率" prop="onTimeRate" width="110" align="center">
            <template #default="{ row }">{{ Number(row.onTimeRate).toFixed(2) }}%</template>
          </el-table-column>
          <el-table-column label="合格率" prop="qualifiedRate" width="110" align="center">
            <template #default="{ row }">{{ Number(row.qualifiedRate).toFixed(2) }}%</template>
          </el-table-column>
        </el-table>
        <el-alert
          class="statement-tip"
          type="info"
          :closable="false"
          title="对账说明：按到货时间归属月份；金额为该供应商当月全部采购记录的合计，店主按“应付金额”与供应商结算即可。"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- ===== 供应商弹窗 ===== -->
    <el-dialog v-model="supplierVisible" :title="supplierForm.id ? '编辑供应商' : '新增供应商'" width="560px">
      <el-form label-position="top">
        <el-form-item label="供应商名称（必填）">
          <el-input v-model="supplierForm.name" maxlength="64" placeholder="如：绿源蔬菜批发部" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="联系人">
            <el-input v-model="supplierForm.contactPerson" maxlength="32" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="supplierForm.phone" maxlength="11" placeholder="11位手机号" />
          </el-form-item>
        </div>
        <el-form-item label="地址">
          <el-input v-model="supplierForm.address" maxlength="255" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="主营品类">
            <el-select v-model="supplierForm.mainCategory" filterable allow-create style="width: 100%">
              <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="supplierForm.status" :active-value="1" :inactive-value="0"
              active-text="合作中" inactive-text="已停用" />
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input v-model="supplierForm.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supplierVisible = false">取消</el-button>
        <el-button type="primary" :loading="supplierSaving" @click="saveSupplier">保存</el-button>
      </template>
    </el-dialog>

    <!-- ===== 原材料弹窗 ===== -->
    <el-dialog v-model="materialVisible" :title="materialForm.id ? '编辑原材料' : '新增原材料'" width="560px">
      <el-form label-position="top">
        <el-form-item label="原材料名称（必填）">
          <el-input v-model="materialForm.name" maxlength="64" placeholder="如：时令青菜" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="分类">
            <el-select v-model="materialForm.category" style="width: 100%">
              <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="单位">
            <el-select v-model="materialForm.unit" filterable allow-create style="width: 100%">
              <el-option label="斤" value="斤" />
              <el-option label="个" value="个" />
              <el-option label="块" value="块" />
              <el-option label="袋" value="袋" />
              <el-option label="瓶" value="瓶" />
              <el-option label="箱" value="箱" />
              <el-option label="升" value="升" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="参考单价（元/单位）">
            <el-input-number v-model="materialForm.referencePrice" :min="0" :precision="2"
              :step="0.5" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="对应供应商">
            <el-select v-model="materialForm.supplierId" filterable style="width: 100%">
              <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="状态">
          <el-switch v-model="materialForm.status" :active-value="1" :inactive-value="0"
            active-text="在用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="materialVisible = false">取消</el-button>
        <el-button type="primary" :loading="materialSaving" @click="saveMaterial">保存</el-button>
      </template>
    </el-dialog>

    <!-- ===== 登记采购弹窗 ===== -->
    <el-dialog v-model="purchaseVisible" title="登记采购" width="520px">
      <el-form label-position="top">
        <el-form-item label="供应商（先选）">
          <el-select v-model="purchaseForm.supplierId" filterable style="width: 100%">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="原材料（只显示该供应商家的料）">
          <el-select
            v-model="purchaseForm.materialId"
            filterable
            style="width: 100%"
            @change="onMaterialPicked"
          >
            <el-option
              v-for="m in materialsOfPickedSupplier"
              :key="m.id"
              :label="`${m.name}（参考价 ${Number(m.referencePrice).toFixed(2)}元/${m.unit}）`"
              :value="m.id"
            />
          </el-select>
          <div v-if="purchaseForm.supplierId && !materialsOfPickedSupplier.length" class="form-warn">
            该供应商还没有绑定原材料，请先到"原材料管理"添加
          </div>
        </el-form-item>
        <div class="form-row">
          <el-form-item :label="`数量（${pickedMaterialUnit}）`">
            <el-input-number v-model="purchaseForm.quantity" :min="0" :precision="2"
              controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="采购单价（元）">
            <el-input-number v-model="purchaseForm.unitPrice" :min="0" :precision="2"
              controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="到货时间">
          <el-date-picker
            v-model="purchaseForm.purchaseTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="是否准时到货">
            <el-switch v-model="purchaseForm.isOnTime" :active-value="1" :inactive-value="0"
              active-text="准时" inactive-text="延迟" />
          </el-form-item>
          <el-form-item label="质量是否合格">
            <el-switch v-model="purchaseForm.isQualified" :active-value="1" :inactive-value="0"
              active-text="合格" inactive-text="不合格" />
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input v-model="purchaseForm.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
        <div class="purchase-total-bar">
          本次合计：<b>{{ purchaseTotal }}</b> 元
        </div>
      </el-form>
      <template #footer>
        <el-button @click="purchaseVisible = false">取消</el-button>
        <el-button type="primary" :loading="purchaseSaving" @click="savePurchase">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.supply-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.overview-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.overview-card {
  flex: 1;
  min-width: 200px;
  border-radius: 14px;
  padding: 18px 22px;
  color: #fff;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.12);
}
.ov-blue {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}
.ov-green {
  background: linear-gradient(135deg, #10b981, #059669);
}
.ov-orange {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}
.ov-label {
  font-size: 13px;
  opacity: 0.9;
}
.ov-value {
  font-size: 28px;
  font-weight: 800;
  margin: 4px 0;
}
.ov-unit {
  font-size: 14px;
  font-weight: 400;
}
.ov-sub {
  font-size: 12px;
  opacity: 0.85;
}
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.muted {
  color: #9ca3af;
  font-size: 13px;
}
.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
.form-row {
  display: flex;
  gap: 14px;
  width: 100%;
}
.form-row .el-form-item {
  flex: 1;
}
.form-warn {
  color: #e6a23c;
  font-size: 12px;
  margin-top: 4px;
}
.purchase-total-bar {
  text-align: right;
  padding: 10px 16px;
  background: #fff7ed;
  border-radius: 10px;
  color: #909399;
}
.purchase-total-bar b {
  color: #e6532b;
  font-size: 22px;
}
.statement-tip {
  margin-top: 14px;
}
</style>
