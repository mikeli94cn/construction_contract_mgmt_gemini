<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">Multi-Step Approval Workflow Inbox</h1>
    </div>

    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form :inline="true" @submit.prevent="loadWorkflows">
        <el-form-item label="Target Type">
          <el-select v-model="filters.targetType" placeholder="All Target Types" clearable style="width: 210px">
            <el-option v-for="t in targetTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="Workflow Status">
          <el-select v-model="filters.status" placeholder="All Statuses" clearable style="width: 160px">
            <el-option v-for="st in workflowStatuses" :key="st" :label="st" :value="st" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadWorkflows">Filter</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="workflows" border stripe style="width: 100%">
        <el-table-column prop="referenceNo" label="Reference No." width="160" />
        <el-table-column prop="targetType" label="Entity Type" width="170" />
        <el-table-column prop="title" label="Subject / Title" min-width="230" />
        <el-table-column label="Gross Amount" width="160" align="right">
          <template #default="{ row }">
            <strong>{{ formatCurrency(row.amountGross) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="Current Active Step" min-width="230">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <div>
                <strong>Step {{ row.currentStepOrder }}:</strong>
                {{ activeStep(row)?.stepName }}
              </div>
              <el-tag size="small" type="warning">
                Requires {{ activeStep(row)?.requiredRole.replace('ROLE_', '') }}
              </el-tag>
            </template>
            <span v-else style="color: #64748b">Completed</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="120">
          <template #default="{ row }">
            <el-tag :type="statusBadge(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submittedBy" label="Submitted By" width="130" />
        <el-table-column label="Actions" width="190" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDecisionModal(row)">
              {{ canDecide(row) ? 'Review & Decide' : 'Timeline' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="`Approval Workflow — ${selectedWorkflow?.referenceNo || ''}`"
      width="700px"
    >
      <template v-if="selectedWorkflow">
        <el-descriptions :column="2" border style="margin-bottom: 18px">
          <el-descriptions-item label="Target Type">{{ selectedWorkflow.targetType }}</el-descriptions-item>
          <el-descriptions-item label="Reference No.">{{ selectedWorkflow.referenceNo }}</el-descriptions-item>
          <el-descriptions-item label="Title" :span="2">{{ selectedWorkflow.title }}</el-descriptions-item>
          <el-descriptions-item label="Gross Amount">
            <strong>{{ formatCurrency(selectedWorkflow.amountGross) }}</strong>
          </el-descriptions-item>
          <el-descriptions-item label="Workflow Status">
            <el-tag :type="statusBadge(selectedWorkflow.status)" size="small">
              {{ selectedWorkflow.status }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <h4 style="margin: 0 0 12px 0">Approval Step Timeline</h4>
        <el-timeline>
          <el-timeline-item
            v-for="step in selectedWorkflow.steps"
            :key="step.id"
            :type="stepTimelineType(step.status)"
            :timestamp="step.decidedAt || 'Pending decision'"
          >
            <div>
              <strong>Step {{ step.stepOrder }}: {{ step.stepName }}</strong>
              <el-tag size="small" style="margin-left: 8px">{{ step.status }}</el-tag>
            </div>
            <div style="font-size: 12px; color: #64748b; margin-top: 4px">
              Required Role: {{ step.requiredRole.replace('ROLE_', '') }}
              <span v-if="step.actorUsername"> | Decided by: <strong>{{ step.actorUsername }}</strong></span>
            </div>
            <div v-if="step.comments" style="font-size: 13px; margin-top: 4px; color: #334155">
              "{{ step.comments }}"
            </div>
          </el-timeline-item>
        </el-timeline>

        <div v-if="canDecide(selectedWorkflow)" class="decision-box">
          <h4 style="margin: 0 0 10px 0">Submit Decision for Step {{ selectedWorkflow.currentStepOrder }}</h4>
          <el-input
            v-model="decisionComments"
            type="textarea"
            :rows="3"
            placeholder="Enter approval remarks or mandatory rejection reason..."
          />
          <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 12px">
            <el-button
              type="danger"
              plain
              :loading="submitting"
              @click="submitDecision('REJECT')"
            >
              Reject & Return
            </el-button>
            <el-button
              type="success"
              :loading="submitting"
              @click="submitDecision('APPROVE')"
            >
              Approve Step
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http, extractErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { formatCurrency } from '../utils/money'
import type {
  ApprovalStep,
  ApprovalStepStatus,
  ApprovalTargetType,
  ApprovalWorkflow,
  ApprovalWorkflowStatus,
  PageResponse
} from '../types/api'

const auth = useAuthStore()

const targetTypes: ApprovalTargetType[] = [
  'CONTRACT',
  'CHANGE_ORDER',
  'PAYMENT_APPLICATION',
  'FINAL_SETTLEMENT'
]
const workflowStatuses: ApprovalWorkflowStatus[] = ['PENDING', 'APPROVED', 'REJECTED']

const loading = ref(false)
const submitting = ref(false)
const workflows = ref<ApprovalWorkflow[]>([])

const filters = reactive({
  targetType: '' as ApprovalTargetType | '',
  status: '' as ApprovalWorkflowStatus | ''
})

const dialogVisible = ref(false)
const selectedWorkflow = ref<ApprovalWorkflow | null>(null)
const decisionComments = ref('')

function activeStep(wf: ApprovalWorkflow): ApprovalStep | undefined {
  return wf.steps.find((s) => s.stepOrder === wf.currentStepOrder)
}

function canDecide(wf: ApprovalWorkflow): boolean {
  if (wf.status !== 'PENDING') return false
  const step = activeStep(wf)
  if (!step) return false
  return auth.hasAnyRole('ROLE_SYSTEM_ADMIN', step.requiredRole)
}

function statusBadge(status: ApprovalWorkflowStatus) {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

function stepTimelineType(status: ApprovalStepStatus) {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'PENDING') return 'warning'
  return 'info'
}

async function loadWorkflows() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: 0, size: 50 }
    if (filters.targetType) params.targetType = filters.targetType
    if (filters.status) params.status = filters.status
    const { data } = await http.get<PageResponse<ApprovalWorkflow>>('/approvals', { params })
    workflows.value = data.content
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    loading.value = false
  }
}

function openDecisionModal(wf: ApprovalWorkflow) {
  selectedWorkflow.value = wf
  decisionComments.value = ''
  dialogVisible.value = true
}

async function submitDecision(action: 'APPROVE' | 'REJECT') {
  if (!selectedWorkflow.value) return
  if (action === 'REJECT' && !decisionComments.value.trim()) {
    ElMessage.warning('Comments are required when rejecting an approval step')
    return
  }
  submitting.value = true
  try {
    const { data } = await http.post<ApprovalWorkflow>(
      `/approvals/${selectedWorkflow.value.id}/decision`,
      {
        action,
        comments: decisionComments.value
      }
    )
    selectedWorkflow.value = data
    ElMessage.success(`Step ${action === 'APPROVE' ? 'approved' : 'rejected'}`)
    await loadWorkflows()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadWorkflows()
})
</script>

<style scoped>
.decision-box {
  margin-top: 16px;
  padding: 16px;
  background-color: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
}
</style>
