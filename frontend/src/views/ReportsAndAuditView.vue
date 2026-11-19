<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">Executive Financial Reporting & Immutable Audit Trail</h1>
    </div>

    <!-- Project Financial Summary Section -->
    <el-card shadow="never" style="margin-bottom: 20px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <strong>Project Financial Commitment, Certification & Retainage Summary</strong>
          <el-select
            v-model="selectedProjectId"
            placeholder="Select Construction Project"
            style="width: 340px"
            @change="loadFinancialSummary"
          >
            <el-option
              v-for="p in projects"
              :key="p.id"
              :label="`${p.projectCode} — ${p.name}`"
              :value="p.id"
            />
          </el-select>
        </div>
      </template>

      <div v-if="summary" v-loading="loadingSummary">
        <el-row :gutter="16" style="margin-bottom: 16px">
          <el-col :span="6">
            <div class="report-card">
              <div class="report-label">Approved Project Budget</div>
              <div class="report-value">{{ formatCurrency(summary.approvedBudget, summary.currency) }}</div>
              <div class="report-sub">Status: {{ summary.projectStatus }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="report-card">
              <div class="report-label">Current Committed Gross</div>
              <div class="report-value">{{ formatCurrency(summary.totalCurrentCommittedGross, summary.currency) }}</div>
              <div class="report-sub">
                Original: {{ formatCurrency(summary.totalOriginalContractGross, summary.currency) }} |
                Variations: {{ formatCurrency(summary.totalApprovedChangeOrdersGross, summary.currency) }}
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="report-card">
              <div class="report-label">Remaining Uncommitted Budget</div>
              <div class="report-value" :style="{ color: summary.remainingUncommittedBudget >= 0 ? '#16a34a' : '#dc2626' }">
                {{ formatCurrency(summary.remainingUncommittedBudget, summary.currency) }}
              </div>
              <div class="report-sub">
                Total Contracts: {{ summary.totalContracts }} (Active: {{ summary.activeContracts }}, Settled/Closed: {{ summary.settledOrClosedContracts }})
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="report-card">
              <div class="report-label">Certified Gross / Disbursed Net</div>
              <div class="report-value">{{ formatCurrency(summary.totalCertifiedPaymentsGross, summary.currency) }}</div>
              <div class="report-sub">
                Paid Net: {{ formatCurrency(summary.totalPaidNetAmount, summary.currency) }} |
                Retainage Held: {{ formatCurrency(summary.totalRetainageHeld, summary.currency) }}
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- Immutable Audit Trail Explorer -->
    <el-card v-if="canViewAuditLogs" shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <strong>Immutable Enterprise Audit Trail Explorer</strong>
          <div style="display: flex; gap: 10px">
            <el-select v-model="auditFilter.module" placeholder="All Modules" clearable size="small" style="width: 160px">
              <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
            </el-select>
            <el-input
              v-model="auditFilter.actorUsername"
              placeholder="Filter by actor username"
              clearable
              size="small"
              style="width: 190px"
            />
            <el-button size="small" type="primary" @click="loadAuditLogs">Search Logs</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loadingAudit" :data="auditLogs" border stripe size="small" style="width: 100%">
        <el-table-column prop="occurredAt" label="Timestamp (UTC)" width="200" />
        <el-table-column prop="actorUsername" label="Actor" width="130" />
        <el-table-column prop="module" label="Module" width="130">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Entity" width="175">
          <template #default="{ row }">
            {{ row.entityType }} #{{ row.entityId }}
          </template>
        </el-table-column>
        <el-table-column prop="action" label="Action" width="240" />
        <el-table-column prop="summary" label="Audit Summary" min-width="280" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http, extractErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { formatCurrency } from '../utils/money'
import type {
  AuditLogEntry,
  PageResponse,
  Project,
  ProjectFinancialSummary
} from '../types/api'

const auth = useAuthStore()

const canViewAuditLogs = computed(() =>
  auth.hasAnyRole('ROLE_SYSTEM_ADMIN', 'ROLE_AUDITOR', 'ROLE_EXECUTIVE_APPROVER')
)

const modules = [
  'IAM',
  'PROJECT',
  'PARTNER',
  'CONTRACT',
  'APPROVAL',
  'DOCUMENT',
  'CHANGE_ORDER',
  'PAYMENT',
  'SETTLEMENT'
]

const projects = ref<Project[]>([])
const selectedProjectId = ref<number | undefined>(undefined)
const summary = ref<ProjectFinancialSummary | null>(null)
const loadingSummary = ref(false)

const auditLogs = ref<AuditLogEntry[]>([])
const loadingAudit = ref(false)
const auditFilter = reactive({
  module: '',
  actorUsername: ''
})

async function loadProjects() {
  try {
    const { data } = await http.get<PageResponse<Project>>('/projects', {
      params: { page: 0, size: 100 }
    })
    projects.value = data.content
    if (data.content.length > 0) {
      selectedProjectId.value = data.content[0].id
      await loadFinancialSummary()
    }
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  }
}

async function loadFinancialSummary() {
  if (!selectedProjectId.value) return
  loadingSummary.value = true
  try {
    const { data } = await http.get<ProjectFinancialSummary>(
      `/reports/projects/${selectedProjectId.value}/financial-summary`
    )
    summary.value = data
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    loadingSummary.value = false
  }
}

async function loadAuditLogs() {
  if (!canViewAuditLogs.value) return
  loadingAudit.value = true
  try {
    const params: Record<string, unknown> = { page: 0, size: 50 }
    if (auditFilter.module) params.module = auditFilter.module
    if (auditFilter.actorUsername) params.actorUsername = auditFilter.actorUsername
    const { data } = await http.get<PageResponse<AuditLogEntry>>('/audit-logs', { params })
    auditLogs.value = data.content
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    loadingAudit.value = false
  }
}

onMounted(async () => {
  await loadProjects()
  if (canViewAuditLogs.value) {
    await loadAuditLogs()
  }
})
</script>

<style scoped>
.report-card {
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background-color: #f8fafc;
}
.report-label {
  font-size: 12px;
  color: #64748b;
  text-transform: uppercase;
}
.report-value {
  font-size: 20px;
  font-weight: 700;
  margin: 6px 0;
  color: #0f172a;
}
.report-sub {
  font-size: 12px;
  color: #64748b;
}
</style>
