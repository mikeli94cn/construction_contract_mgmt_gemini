<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">Business Partners Directory</h1>
      <el-button
        v-if="canManagePartners"
        type="primary"
        @click="openCreateDialog"
      >
        + Register Business Partner
      </el-button>
    </div>

    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form :inline="true" @submit.prevent="loadPartners">
        <el-form-item label="Search">
          <el-input
            v-model="filters.keyword"
            placeholder="Code, legal name, or tax ID"
            clearable
            @clear="loadPartners"
          />
        </el-form-item>
        <el-form-item label="Type">
          <el-select v-model="filters.partnerType" placeholder="All Types" clearable style="width: 190px">
            <el-option v-for="pt in partnerTypes" :key="pt" :label="pt" :value="pt" />
          </el-select>
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="filters.status" placeholder="All Statuses" clearable style="width: 150px">
            <el-option v-for="st in partnerStatuses" :key="st" :label="st" :value="st" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadPartners">Filter</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="partners" border stripe style="width: 100%">
        <el-table-column prop="partnerCode" label="Partner Code" width="140" />
        <el-table-column prop="name" label="Legal Entity Name" min-width="220" />
        <el-table-column prop="partnerType" label="Category" width="180" />
        <el-table-column prop="taxRegistrationNo" label="Tax Reg. No." width="160" />
        <el-table-column prop="qualificationLevel" label="Qualification" width="160" />
        <el-table-column prop="contactPerson" label="Contact Person" width="150" />
        <el-table-column prop="contactPhone" label="Phone" width="140" />
        <el-table-column prop="status" label="Status" width="130">
          <template #default="{ row }">
            <el-tag :type="statusBadge(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canManagePartners" label="Actions" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openEditDialog(row)">Edit</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? 'Edit Business Partner' : 'Register Business Partner'"
      width="620px"
    >
      <el-form :model="partnerForm" label-width="170px">
        <el-form-item label="Partner Code" required>
          <el-input v-model="partnerForm.partnerCode" :disabled="Boolean(editingId)" placeholder="e.g. BP-SUB-101" />
        </el-form-item>
        <el-form-item label="Legal Entity Name" required>
          <el-input v-model="partnerForm.name" placeholder="Full registered corporate name" />
        </el-form-item>
        <el-form-item label="Partner Type" required>
          <el-select v-model="partnerForm.partnerType" style="width: 100%">
            <el-option v-for="pt in partnerTypes" :key="pt" :label="pt" :value="pt" />
          </el-select>
        </el-form-item>
        <el-form-item label="Tax Registration No." required>
          <el-input v-model="partnerForm.taxRegistrationNo" :disabled="Boolean(editingId)" placeholder="Unique tax ID / EIN" />
        </el-form-item>
        <el-form-item label="Qualification Level">
          <el-input v-model="partnerForm.qualificationLevel" placeholder="e.g. Grade-A Civil & Structural" />
        </el-form-item>
        <el-form-item label="Contact Person" required>
          <el-input v-model="partnerForm.contactPerson" />
        </el-form-item>
        <el-form-item label="Contact Phone" required>
          <el-input v-model="partnerForm.contactPhone" />
        </el-form-item>
        <el-form-item label="Contact Email" required>
          <el-input v-model="partnerForm.contactEmail" />
        </el-form-item>
        <el-form-item label="Bank Name">
          <el-input v-model="partnerForm.bankName" />
        </el-form-item>
        <el-form-item label="Bank Account No.">
          <el-input v-model="partnerForm.bankAccountNo" />
        </el-form-item>
        <el-form-item label="Status" required>
          <el-select v-model="partnerForm.status" style="width: 100%">
            <el-option v-for="st in partnerStatuses" :key="st" :label="st" :value="st" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="savePartner">Save Partner</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http, extractErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import type { BusinessPartner, PageResponse, PartnerStatus, PartnerType } from '../types/api'

const auth = useAuthStore()
const canManagePartners = computed(() =>
  auth.hasAnyRole('ROLE_SYSTEM_ADMIN', 'ROLE_CONTRACT_MANAGER', 'ROLE_PROJECT_MANAGER')
)

const partnerTypes: PartnerType[] = [
  'OWNER',
  'GENERAL_CONTRACTOR',
  'SUBCONTRACTOR',
  'SUPPLIER',
  'CONSULTANT'
]
const partnerStatuses: PartnerStatus[] = ['ACTIVE', 'SUSPENDED', 'BLACKLISTED']

const loading = ref(false)
const saving = ref(false)
const partners = ref<BusinessPartner[]>([])

const filters = reactive({
  keyword: '',
  partnerType: '' as PartnerType | '',
  status: '' as PartnerStatus | ''
})

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const partnerForm = reactive({
  partnerCode: '',
  name: '',
  partnerType: 'SUBCONTRACTOR' as PartnerType,
  taxRegistrationNo: '',
  qualificationLevel: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  bankName: '',
  bankAccountNo: '',
  status: 'ACTIVE' as PartnerStatus
})

function statusBadge(status: PartnerStatus) {
  if (status === 'ACTIVE') return 'success'
  if (status === 'SUSPENDED') return 'warning'
  return 'danger'
}

async function loadPartners() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: 0, size: 50 }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.partnerType) params.partnerType = filters.partnerType
    if (filters.status) params.status = filters.status
    const { data } = await http.get<PageResponse<BusinessPartner>>('/partners', { params })
    partners.value = data.content
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingId.value = null
  partnerForm.partnerCode = ''
  partnerForm.name = ''
  partnerForm.partnerType = 'SUBCONTRACTOR'
  partnerForm.taxRegistrationNo = ''
  partnerForm.qualificationLevel = ''
  partnerForm.contactPerson = ''
  partnerForm.contactPhone = ''
  partnerForm.contactEmail = ''
  partnerForm.bankName = ''
  partnerForm.bankAccountNo = ''
  partnerForm.status = 'ACTIVE'
  dialogVisible.value = true
}

function openEditDialog(row: BusinessPartner) {
  editingId.value = row.id
  partnerForm.partnerCode = row.partnerCode
  partnerForm.name = row.name
  partnerForm.partnerType = row.partnerType
  partnerForm.taxRegistrationNo = row.taxRegistrationNo
  partnerForm.qualificationLevel = row.qualificationLevel || ''
  partnerForm.contactPerson = row.contactPerson
  partnerForm.contactPhone = row.contactPhone
  partnerForm.contactEmail = row.contactEmail
  partnerForm.bankName = row.bankName || ''
  partnerForm.bankAccountNo = row.bankAccountNo || ''
  partnerForm.status = row.status
  dialogVisible.value = true
}

async function savePartner() {
  saving.value = true
  try {
    if (editingId.value) {
      await http.put(`/partners/${editingId.value}`, partnerForm)
      ElMessage.success('Business partner updated')
    } else {
      await http.post('/partners', partnerForm)
      ElMessage.success('Business partner registered')
    }
    dialogVisible.value = false
    await loadPartners()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadPartners()
})
</script>
