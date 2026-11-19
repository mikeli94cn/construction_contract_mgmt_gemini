<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">Construction Contracts Register</h1>
      <el-button
        v-if="canManageContracts"
        type="primary"
        @click="router.push('/contracts/new')"
      >
        + Draft New Contract
      </el-button>
    </div>

    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form :inline="true" @submit.prevent="loadContracts">
        <el-form-item label="Keyword">
          <el-input
            v-model="filters.keyword"
            placeholder="Contract No. or title"
            clearable
            @clear="loadContracts"
          />
        </el-form-item>
        <el-form-item label="Category">
          <el-select v-model="filters.category" placeholder="All Categories" clearable style="width: 185px">
            <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="filters.status" placeholder="All Statuses" clearable style="width: 150px">
            <el-option v-for="st in statuses" :key="st" :label="st" :value="st" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadContracts">Search</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="contracts" border stripe style="width: 100%">
        <el-table-column prop="contractNo" label="Contract No." width="160" />
        <el-table-column prop="title" label="Contract Title" min-width="230" />
        <el-table-column label="Project" width="180">
          <template #default="{ row }">
            <div><strong>{{ row.projectCode }}</strong></div>
            <div style="font-size: 12px; color: #64748b">{{ row.projectName }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="partnerName" label="Business Partner" width="200" />
        <el-table-column prop="category" label="Category" width="160" />
        <el-table-column label="Original Gross" width="160" align="right">
          <template #default="{ row }">
            {{ formatCurrency(row.originalAmountGross, row.currency) }}
          </template>
        </el-table-column>
        <el-table-column label="Current Gross Value" width="170" align="right">
          <template #default="{ row }">
            <strong>{{ formatCurrency(row.currentAmountGross, row.currency) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="130">
          <template #default="{ row }">
            <el-tag :type="statusBadge(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="router.push(`/contracts/${row.id}`)">View</el-button>
            <el-button
              v-if="canManageContracts && row.status === 'DRAFT'"
              size="small"
              type="primary"
              plain
              @click="router.push(`/contracts/${row.id}/edit`)"
            >
              Edit Draft
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http, extractErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { formatCurrency } from '../utils/money'
import type { Contract, ContractCategory, ContractStatus, PageResponse } from '../types/api'

const router = useRouter()
const auth = useAuthStore()

const canManageContracts = computed(() =>
  auth.hasAnyRole('ROLE_SYSTEM_ADMIN', 'ROLE_PROJECT_MANAGER', 'ROLE_CONTRACT_MANAGER')
)

const categories: ContractCategory[] = [
  'MAIN_CONTRACT',
  'SUBCONTRACT',
  'SUPPLY_CONTRACT',
  'CONSULTING_CONTRACT'
]
const statuses: ContractStatus[] = [
  'DRAFT',
  'IN_APPROVAL',
  'APPROVED',
  'ACTIVE',
  'SETTLED',
  'CLOSED',
  'TERMINATED'
]

const loading = ref(false)
const contracts = ref<Contract[]>([])
const filters = reactive({
  keyword: '',
  category: '' as ContractCategory | '',
  status: '' as ContractStatus | ''
})

function statusBadge(status: ContractStatus) {
  switch (status) {
    case 'ACTIVE':
    case 'APPROVED':
      return 'success'
    case 'IN_APPROVAL':
      return 'warning'
    case 'DRAFT':
      return 'info'
    case 'TERMINATED':
      return 'danger'
    default:
      return 'primary'
  }
}

async function loadContracts() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: 0, size: 50 }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.category) params.category = filters.category
    if (filters.status) params.status = filters.status
    const { data } = await http.get<PageResponse<Contract>>('/contracts', { params })
    contracts.value = data.content
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadContracts()
})
</script>
