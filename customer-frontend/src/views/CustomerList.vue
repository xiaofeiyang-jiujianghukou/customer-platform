<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { queryCustomers, getCustomer, updateCustomer, generateTestData } from '../api/customer'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const generating = ref(false)
const detailVisible = ref(false)
const editVisible = ref(false)
const currentCustomer = ref({})

const queryForm = reactive({
  customerNo: '',
  name: '',
  phone: '',
  status: '',
  source: '',
  channel: '',
  bu: '',
  product: '',
  sortField: 'id',
  sortOrder: 'desc'
})

const editForm = reactive({
  name: '',
  phone: '',
  source: '',
  channel: '',
  bu: '',
  product: '',
  salesId: null,
  plannerId: null
})

const sourceOptions = ['', '官网', 'APP', '小程序', '电话', '门店']
const channelOptions = ['', '直销', '渠道A', '渠道B', '渠道C']
const buOptions = ['', 'BU1', 'BU2', 'BU3']
const productOptions = ['', '产品A', '产品B', '产品C', '产品D', '产品E']
const statusOptions = [
  { label: '全部', value: '' },
  { label: '有效', value: 0 },
  { label: '已删', value: 1 }
]

async function fetchData() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      sortField: queryForm.sortField,
      sortOrder: queryForm.sortOrder
    }
    if (queryForm.customerNo) params.customerNo = queryForm.customerNo
    if (queryForm.name) params.name = queryForm.name
    if (queryForm.phone) params.phone = queryForm.phone
    if (queryForm.status !== '' && queryForm.status !== undefined) params.status = queryForm.status
    if (queryForm.source) params.source = queryForm.source
    if (queryForm.channel) params.channel = queryForm.channel
    if (queryForm.bu) params.bu = queryForm.bu
    if (queryForm.product) params.product = queryForm.product

    const res = await queryCustomers(params)
    tableData.value = res.data.data || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('查询失败: ' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchData()
}

function handleReset() {
  queryForm.customerNo = ''
  queryForm.name = ''
  queryForm.phone = ''
  queryForm.status = ''
  queryForm.source = ''
  queryForm.channel = ''
  queryForm.bu = ''
  queryForm.product = ''
  currentPage.value = 1
  fetchData()
}

function handlePageChange(page) {
  currentPage.value = page
  fetchData()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  fetchData()
}

async function handleDetail(customerNo) {
  try {
    const res = await getCustomer(customerNo)
    currentCustomer.value = res.data
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

function handleEdit(row) {
  currentCustomer.value = { ...row }
  editForm.name = row.name || ''
  editForm.phone = row.phone || ''
  editForm.source = row.source || ''
  editForm.channel = row.channel || ''
  editForm.bu = row.bu || ''
  editForm.product = row.product || ''
  editForm.salesId = row.salesId
  editForm.plannerId = row.plannerId
  editVisible.value = true
}

async function handleSaveEdit() {
  try {
    await updateCustomer(currentCustomer.value.customerNo, {
      name: editForm.name,
      phone: editForm.phone,
      source: editForm.source,
      channel: editForm.channel,
      bu: editForm.bu,
      product: editForm.product,
      salesId: editForm.salesId,
      plannerId: editForm.plannerId
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message))
  }
}

async function handleGenerateData() {
  try {
    await ElMessageBox.confirm('将生成 1000 条测试数据操作（40%新增 + 40%更新 + 20%逻辑删除），确定继续？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  generating.value = true
  try {
    const res = await generateTestData()
    const data = res.data
    ElMessage.success(`完成！新增 ${data.inserted} 条，更新 ${data.updated} 条，删除 ${data.deleted} 条，耗时 ${data.elapsedMs}ms`)
    fetchData()
  } catch (e) {
    ElMessage.error('操作失败: ' + (e.response?.data?.message || e.message))
  } finally {
    generating.value = false
  }
}

function formatDateTime(val) {
  return val ? val.replace('T', ' ') : '-'
}

onMounted(fetchData)
</script>

<template>
  <div style="padding: 20px; max-width: 1400px; margin: 0 auto;">
    <h2 style="margin-bottom: 16px;">客户管理</h2>

    <!-- 操作按钮 -->
    <div style="margin-bottom: 12px; display: flex; gap: 8px;">
      <el-button type="primary" @click="fetchData" :loading="loading">刷新</el-button>
      <el-button type="warning" @click="handleGenerateData" :loading="generating">
        批量生成测试数据
      </el-button>
    </div>

    <!-- 查询表单 -->
    <el-card style="margin-bottom: 16px;">
      <el-form :inline="true" :model="queryForm" size="small">
        <el-form-item label="客户编号">
          <el-input v-model="queryForm.customerNo" placeholder="精确匹配" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="queryForm.name" placeholder="模糊搜索" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="queryForm.phone" placeholder="模糊搜索" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" style="width: 90px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="queryForm.source" placeholder="全部" clearable style="width: 100px">
            <el-option v-for="o in sourceOptions" :key="o" :label="o || '全部'" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="queryForm.channel" placeholder="全部" clearable style="width: 100px">
            <el-option v-for="o in channelOptions" :key="o" :label="o || '全部'" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="BU">
          <el-select v-model="queryForm.bu" placeholder="全部" clearable style="width: 100px">
            <el-option v-for="o in buOptions" :key="o" :label="o || '全部'" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="产品">
          <el-select v-model="queryForm.product" placeholder="全部" clearable style="width: 100px">
            <el-option v-for="o in productOptions" :key="o" :label="o || '全部'" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%" size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="customerNo" label="客户编号" width="170" show-overflow-tooltip />
        <el-table-column prop="name" label="姓名" width="130" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="source" label="来源" width="80" />
        <el-table-column prop="channel" label="渠道" width="80" />
        <el-table-column prop="bu" label="BU" width="80" />
        <el-table-column prop="product" label="产品" width="100" />
        <el-table-column prop="salesId" label="销售ID" width="80" />
        <el-table-column prop="plannerId" label="企划ID" width="80" />
        <el-table-column label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">
              {{ row.status === 1 ? '已删' : '有效' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleDetail(row.customerNo)">详情</el-button>
            <el-button size="small" type="success" link @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="客户详情" width="600px">
      <el-descriptions border :column="2" size="small">
        <el-descriptions-item label="ID">{{ currentCustomer.id }}</el-descriptions-item>
        <el-descriptions-item label="客户编号">{{ currentCustomer.customerNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ currentCustomer.name }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ currentCustomer.phone }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ currentCustomer.source }}</el-descriptions-item>
        <el-descriptions-item label="渠道">{{ currentCustomer.channel }}</el-descriptions-item>
        <el-descriptions-item label="BU">{{ currentCustomer.bu }}</el-descriptions-item>
        <el-descriptions-item label="产品">{{ currentCustomer.product }}</el-descriptions-item>
        <el-descriptions-item label="销售ID">{{ currentCustomer.salesId }}</el-descriptions-item>
        <el-descriptions-item label="企划ID">{{ currentCustomer.plannerId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentCustomer.status === 1 ? 'danger' : 'success'" size="small">
            {{ currentCustomer.status === 1 ? '已删' : '有效' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(currentCustomer.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(currentCustomer.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑客户" width="520px" @close="fetchData">
      <el-form :model="editForm" label-width="80px" size="small">
        <el-form-item label="姓名">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="editForm.source" style="width: 100%">
            <el-option v-for="o in sourceOptions.filter(s => s)" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="editForm.channel" style="width: 100%">
            <el-option v-for="o in channelOptions.filter(s => s)" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="BU">
          <el-select v-model="editForm.bu" style="width: 100%">
            <el-option v-for="o in buOptions.filter(s => s)" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="产品">
          <el-select v-model="editForm.product" style="width: 100%">
            <el-option v-for="o in productOptions.filter(s => s)" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="销售ID">
          <el-input-number v-model="editForm.salesId" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="企划ID">
          <el-input-number v-model="editForm.plannerId" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
