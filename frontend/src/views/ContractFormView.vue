<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">
        {{ isEditing ? `Edit Draft Contract — ${form.contractNo}` : 'Draft New Construction Contract' }}
      </h1>
      <el-button @click="router.push('/contracts')">Back to Contracts</el-button>
    </div>

    <el-card shadow="never" style="margin-bottom: 18px">
      <template #header>
        <strong>1. Contract Header & Financial Terms</strong>
      </template>

      <el-form :model="form" label-width="180px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Contract Number" required>
              <el-input
                v-model="form.contractNo"
                :disabled="isEditing"
                placeholder="e.g. CNT-2026-001"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Contract Category" required>
              <el-select v-model="form.category" style="width: 100%">
                <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Contract Title" required>
          <el-input v-model="form.title" placeholder="Descriptive title of works / scope" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Construction Project" required>
              <el-select
                v-model="form.projectId"
                :disabled="isEditing"
                placeholder="Select active project"
                style="width: 100%"
              >
                <el-option
                  v-for="p in projects"
                  :key="p.id"
                  :label="`${p.projectCode} — ${p.name} (${p.currency})`"
                  :value="p.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Business Partner" required>
              <el-select
                v-model="form.partnerId"
                placeholder="Select active business partner"
                style="width: 100%"
              >
                <el-option
                  v-for="bp in activePartners"
                  :key="bp.id"
                  :label="`${bp.partnerCode} — ${bp.name} [${bp.partnerType}]`"
                  :value="bp.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="Tax Rate (0 - 1)" required>
              <el-input-number
                v-model="form.taxRate"
                :min="0"
                :max="1"
                :step="0.01"
                :precision="4"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Retainage Rate (0 - 0.5)" required>
              <el-input-number
                v-model="form.retainageRate"
                :min="0"
                :max="0.5"
                :step="0.01"
                :precision="4"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Advance Rate (0 - 1)">
              <el-input-number
                v-model="form.advancePaymentRate"
                :min="0"
                :max="1"
                :step="0.05"
                :precision="4"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="Signed Date">
              <el-date-picker
                v-model="form.signedDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Start Date" required>
              <el-date-picker
                v-model="form.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Completion Date" required>
              <el-date-picker
                v-model="form.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="Defect Liability (Months)">
              <el-input-number
                v-model="form.defectLiabilityMonths"
                :min="0"
                :max="120"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="Scope Summary">
              <el-input v-model="form.scopeSummary" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-bottom: 18px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <strong>2. Bill of Quantities (BoQ) / Schedule of Values</strong>
          <el-button size="small" type="primary" plain @click="addLineItem">
            + Add BoQ Line Item
          </el-button>
        </div>
      </template>

      <el-table :data="form.lineItems" border style="width: 100%">
        <el-table-column label="Item Code" width="150">
          <template #default="{ row }">
            <el-input v-model="row.itemCode" placeholder="e.g. BOQ-101" />
          </template>
        </el-table-column>
        <el-table-column label="Description of Work / Material" min-width="250">
          <template #default="{ row }">
            <el-input v-model="row.description" placeholder="Work item description" />
          </template>
        </el-table-column>
        <el-table-column label="Unit" width="120">
          <template #default="{ row }">
            <el-input v-model="row.unit" placeholder="m3 / ton / set" />
          </template>
        </el-table-column>
        <el-table-column label="Quantity" width="170">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="0.0001"
              :precision="4"
              :step="10"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column label="Unit Price (Net)" width="180">
          <template #default="{ row }">
            <el-input-number
              v-model="row.unitPriceNet"
              :min="0.0001"
              :precision="4"
              :step="100"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column label="Line Total (Net)" width="170" align="right">
          <template #default="{ row }">
            <strong>{{ formatCurrency(calculateLineItemTotal(row.quantity, row.unitPriceNet), selectedCurrency) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="" width="90" align="center">
          <template #default="{ $index }">
            <el-button
              type="danger"
              size="small"
              plain
              :disabled="form.lineItems.length <= 1"
              @click="removeLineItem($index)"
            >
              Remove
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="financial-summary-panel">
        <div>
          <span>Original Net Amount:</span>
          <strong>{{ formatCurrency(totals.net, selectedCurrency) }}</strong>
        </div>
        <div>
          <span>Tax Amount ({{ (form.taxRate * 100).toFixed(2) }}%):</span>
          <strong>{{ formatCurrency(totals.tax, selectedCurrency) }}</strong>
        </div>
        <div class="gross-total">
          <span>Original Contract Gross Amount:</span>
          <strong>{{ formatCurrency(totals.gross, selectedCurrency) }}</strong>
        </div>
      </div>
    </el-card>

    <div style="display: flex; justify-content: flex-end; gap: 12px">
      <el-button @click="router.push('/contracts')">Cancel</el-button>
      <el-button type="primary" :loading="saving" @click="submitForm">
        {{ isEditing ? 'Save Draft Contract Changes' : 'Create Draft Contract' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http, extractErrorMessage } from '../api/http'
import {
  calculateContractTotals,
  calculateLineItemTotal,
  formatCurrency
} from '../utils/money'
import type {
  BusinessPartner,
  Contract,
  ContractCategory,
  ContractLineItem,
  PageResponse,
  Project
} from '../types/api'

const route = useRoute()
const router = useRouter()

const contractId = computed(() => (route.params.id ? Number(route.params.id) : null))
const isEditing = computed(() => Boolean(contractId.value))

const categories: ContractCategory[] = [
  'MAIN_CONTRACT',
  'SUBCONTRACT',
  'SUPPLY_CONTRACT',
  'CONSULTING_CONTRACT'
]

const projects = ref<Project[]>([])
const activePartners = ref<BusinessPartner[]>([])
const saving = ref(false)

const form = reactive({
  contractNo: '',
  title: '',
  projectId: undefined as number | undefined,
  partnerId: undefined as number | undefined,
  parentContractId: null as number | null,
  category: 'MAIN_CONTRACT' as ContractCategory,
  taxRate: 0.08,
  retainageRate: 0.05,
  advancePaymentRate: 0.1,
  signedDate: '2026-03-01',
  startDate: '2026-03-15',
  endDate: '2027-09-30',
  defectLiabilityMonths: 12,
  scopeSummary: '',
  lineItems: [
    {
      itemCode: 'BOQ-001',
      description: 'Substructure & Foundation Works',
      unit: 'm3',
      quantity: 1000,
      unitPriceNet: 250
    }
  ] as ContractLineItem[]
})

const selectedCurrency = computed(() => {
  const proj = projects.value.find((p) => p.id === form.projectId)
  return proj?.currency || 'USD'
})

const totals = computed(() => calculateContractTotals(form.lineItems, form.taxRate))

function addLineItem() {
  const nextNo = String(form.lineItems.length + 1).padStart(3, '0')
  form.lineItems.push({
    itemCode: `BOQ-${nextNo}`,
    description: '',
    unit: 'unit',
    quantity: 1,
    unitPriceNet: 1000
  })
}

function removeLineItem(index: number) {
  if (form.lineItems.length > 1) {
    form.lineItems.splice(index, 1)
  }
}

async function loadReferenceData() {
  try {
    const [projRes, partnerRes] = await Promise.all([
      http.get<PageResponse<Project>>('/projects', { params: { page: 0, size: 100 } }),
      http.get<PageResponse<BusinessPartner>>('/partners', {
        params: { status: 'ACTIVE', page: 0, size: 100 }
      })
    ])
    projects.value = projRes.data.content
    activePartners.value = partnerRes.data.content
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  }
}

async function loadContractForEdit(id: number) {
  try {
    const { data } = await http.get<Contract>(`/contracts/${id}`)
    if (data.status !== 'DRAFT') {
      ElMessage.warning('Only DRAFT contracts can be edited directly')
      router.push(`/contracts/${id}`)
      return
    }
    form.contractNo = data.contractNo
    form.title = data.title
    form.projectId = data.projectId
    form.partnerId = data.partnerId
    form.parentContractId = data.parentContractId
    form.category = data.category
    form.taxRate = Number(data.taxRate)
    form.retainageRate = Number(data.retainageRate)
    form.advancePaymentRate = Number(data.advancePaymentRate)
    form.signedDate = data.signedDate || ''
    form.startDate = data.startDate
    form.endDate = data.endDate
    form.defectLiabilityMonths = data.defectLiabilityMonths
    form.scopeSummary = data.scopeSummary || ''
    form.lineItems = data.lineItems.map((li) => ({
      itemCode: li.itemCode,
      description: li.description,
      unit: li.unit,
      quantity: Number(li.quantity),
      unitPriceNet: Number(li.unitPriceNet)
    }))
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  }
}

async function submitForm() {
  saving.value = true
  try {
    if (isEditing.value && contractId.value) {
      await http.put(`/contracts/${contractId.value}`, {
        title: form.title,
        partnerId: form.partnerId,
        parentContractId: form.parentContractId,
        category: form.category,
        taxRate: form.taxRate,
        retainageRate: form.retainageRate,
        advancePaymentRate: form.advancePaymentRate,
        signedDate: form.signedDate || null,
        startDate: form.startDate,
        endDate: form.endDate,
        defectLiabilityMonths: form.defectLiabilityMonths,
        scopeSummary: form.scopeSummary,
        lineItems: form.lineItems
      })
      ElMessage.success('Draft contract updated')
      router.push(`/contracts/${contractId.value}`)
    } else {
      const { data } = await http.post<Contract>('/contracts', {
        ...form,
        signedDate: form.signedDate || null
      })
      ElMessage.success(`Draft contract ${data.contractNo} created`)
      router.push(`/contracts/${data.id}`)
    }
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadReferenceData()
  if (contractId.value) {
    await loadContractForEdit(contractId.value)
  }
})
</script>

<style scoped>
.financial-summary-panel {
  margin-top: 16px;
  padding: 16px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  display: flex;
  justify-content: flex-end;
  gap: 32px;
  font-size: 14px;
}
.financial-summary-panel span {
  color: #64748b;
  margin-right: 8px;
}
.gross-total strong {
  color: #0284c7;
  font-size: 16px;
}
</style>
