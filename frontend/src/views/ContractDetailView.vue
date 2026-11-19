<template>
  <div v-loading="loading">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ contract?.contractNo }} — {{ contract?.title }}</h1>
        <div style="color: #64748b; font-size: 13px; margin-top: 4px">
          Project: <strong>{{ contract?.projectCode }} ({{ contract?.projectName }})</strong> |
          Counterparty: <strong>{{ contract?.partnerCode }} ({{ contract?.partnerName }})</strong>
        </div>
      </div>
      <div style="display: flex; gap: 10px">
        <el-button @click="router.push('/contracts')">Back to Register</el-button>
        <el-button
          v-if="contract?.status === 'DRAFT' && canManageContract"
          type="primary"
          plain
          @click="router.push(`/contracts/${contract.id}/edit`)"
        >
          Edit Draft Contract
        </el-button>
        <el-button
          v-if="contract?.status === 'DRAFT' && canManageContract"
          type="warning"
          :loading="actionLoading"
          @click="submitForApproval"
        >
          Submit for Multi-Step Approval
        </el-button>
        <el-button
          v-if="contract?.status === 'APPROVED' && canManageContract"
          type="success"
          :loading="actionLoading"
          @click="activateContract"
        >
          Activate Approved Contract
        </el-button>
      </div>
    </div>

    <template v-if="contract">
      <!-- Financial KPI Cards -->
      <el-row :gutter="16" style="margin-bottom: 18px">
        <el-col :span="6">
          <el-card shadow="never">
            <div class="kpi-label">Original Gross Amount</div>
            <div class="kpi-value">{{ formatCurrency(contract.originalAmountGross, contract.currency) }}</div>
            <div class="kpi-sub">
              Net: {{ formatCurrency(contract.originalAmountNet, contract.currency) }}
              (Tax {{ formatPercent(contract.taxRate) }})
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="kpi-label">Approved Change Orders (Gross)</div>
            <div class="kpi-value">{{ formatCurrency(contract.approvedChangeGross, contract.currency) }}</div>
            <div class="kpi-sub">Net Changes: {{ formatCurrency(contract.approvedChangeNet, contract.currency) }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="kpi-label">Current Contract Value (Gross)</div>
            <div class="kpi-value kpi-highlight">{{ formatCurrency(contract.currentAmountGross, contract.currency) }}</div>
            <div class="kpi-sub">Retainage Rate: {{ formatPercent(contract.retainageRate) }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="kpi-label">Cumulative Certified / Retainage Held</div>
            <div class="kpi-value">{{ formatCurrency(certifiedTotalGross, contract.currency) }}</div>
            <div class="kpi-sub">Retainage Held: {{ formatCurrency(retainageHeldTotal, contract.currency) }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Contract Governance Metadata -->
      <el-card shadow="never" style="margin-bottom: 18px">
        <template #header>
          <strong>Contract Terms & Lifecycle Status</strong>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="Contract No.">{{ contract.contractNo }}</el-descriptions-item>
          <el-descriptions-item label="Category">{{ contract.category }}</el-descriptions-item>
          <el-descriptions-item label="Lifecycle Status">
            <el-tag size="small">{{ contract.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Signed Date">{{ contract.signedDate || 'Pending Signature' }}</el-descriptions-item>
          <el-descriptions-item label="Start Date">{{ contract.startDate }}</el-descriptions-item>
          <el-descriptions-item label="Planned End Date">{{ contract.endDate }}</el-descriptions-item>
          <el-descriptions-item label="Retainage Rate">{{ formatPercent(contract.retainageRate) }}</el-descriptions-item>
          <el-descriptions-item label="Advance Payment Rate">{{ formatPercent(contract.advancePaymentRate) }}</el-descriptions-item>
          <el-descriptions-item label="Defect Liability Period">{{ contract.defectLiabilityMonths }} months</el-descriptions-item>
          <el-descriptions-item label="Optimistic Lock Version">v{{ contract.version }}</el-descriptions-item>
          <el-descriptions-item label="Created By">{{ contract.createdBy }}</el-descriptions-item>
          <el-descriptions-item label="Scope Summary">{{ contract.scopeSummary || '—' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Bill of Quantities Table -->
      <el-card shadow="never" style="margin-bottom: 18px">
        <template #header>
          <strong>Bill of Quantities (BoQ) / Schedule of Values ({{ contract.lineItems.length }} items)</strong>
        </template>
        <el-table :data="contract.lineItems" border stripe style="width: 100%">
          <el-table-column prop="sortOrder" label="#" width="60" />
          <el-table-column prop="itemCode" label="Item Code" width="150" />
          <el-table-column prop="description" label="Work Item Description" min-width="260" />
          <el-table-column prop="unit" label="Unit" width="100" />
          <el-table-column prop="quantity" label="Quantity" width="150" align="right" />
          <el-table-column label="Unit Price (Net)" width="180" align="right">
            <template #default="{ row }">
              {{ formatCurrency(row.unitPriceNet, contract.currency) }}
            </template>
          </el-table-column>
          <el-table-column label="Total Price (Net)" width="190" align="right">
            <template #default="{ row }">
              <strong>{{ formatCurrency(row.totalPriceNet, contract.currency) }}</strong>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- Change Orders Register -->
      <el-card shadow="never" style="margin-bottom: 18px">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center">
            <strong>Change Orders / Contract Variations ({{ changeOrders.length }})</strong>
            <el-button
              v-if="canManageVariations && (contract.status === 'ACTIVE' || contract.status === 'APPROVED')"
              size="small"
              type="primary"
              @click="coDialogVisible = true"
            >
              + Draft Change Order
            </el-button>
          </div>
        </template>

        <el-table :data="changeOrders" border stripe size="small" style="width: 100%">
          <el-table-column prop="changeOrderNo" label="Change Order No." width="150" />
          <el-table-column prop="changeType" label="Variation Type" width="165" />
          <el-table-column prop="title" label="Title" min-width="200" />
          <el-table-column label="Net Delta" width="150" align="right">
            <template #default="{ row }">
              {{ formatCurrency(row.amountDeltaNet, contract.currency) }}
            </template>
          </el-table-column>
          <el-table-column label="Gross Delta" width="150" align="right">
            <template #default="{ row }">
              <strong>{{ formatCurrency(row.amountDeltaGross, contract.currency) }}</strong>
            </template>
          </el-table-column>
          <el-table-column prop="scheduleExtensionDays" label="Ext. Days" width="100" align="center" />
          <el-table-column prop="status" label="Status" width="125">
            <template #default="{ row }">
              <el-tag size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Action" width="160" align="center">
            <template #default="{ row }">
              <el-button
                v-if="canManageVariations && (row.status === 'DRAFT' || row.status === 'REJECTED')"
                size="small"
                type="warning"
                plain
                @click="submitChangeOrderApproval(row.id)"
              >
                Submit Approval
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- Progress Payment Applications (IPC) & Retainage Ledger -->
      <el-card shadow="never" style="margin-bottom: 18px">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center">
            <strong>Progress Payment Applications (IPC) & Retainage Withholding Ledger ({{ payments.length }})</strong>
            <el-button
              v-if="canManageVariations && contract.status === 'ACTIVE'"
              size="small"
              type="primary"
              @click="openPaymentDialog"
            >
              + New Payment Application (IPC)
            </el-button>
          </div>
        </template>

        <el-table :data="payments" border stripe size="small" style="width: 100%">
          <el-table-column prop="applicationNo" label="IPC No." width="130" />
          <el-table-column label="Period" width="195">
            <template #default="{ row }">
              {{ row.periodStart }} &rarr; {{ row.periodEnd }}
            </template>
          </el-table-column>
          <el-table-column label="Cumulative Gross" width="150" align="right">
            <template #default="{ row }">
              {{ formatCurrency(row.cumulativeCompletedGross, contract.currency) }}
            </template>
          </el-table-column>
          <el-table-column label="Current Period Gross" width="160" align="right">
            <template #default="{ row }">
              {{ formatCurrency(row.currentPeriodGross, contract.currency) }}
            </template>
          </el-table-column>
          <el-table-column label="Retainage Held" width="145" align="right">
            <template #default="{ row }">
              {{ formatCurrency(row.retainageWithheld, contract.currency) }}
            </template>
          </el-table-column>
          <el-table-column label="Net Payable" width="150" align="right">
            <template #default="{ row }">
              <strong>{{ formatCurrency(row.netPayableAmount, contract.currency) }}</strong>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="Status" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="paymentReference" label="Bank Ref." width="150" />
          <el-table-column label="Action" width="170" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="canManageVariations && (row.status === 'DRAFT' || row.status === 'REJECTED')"
                size="small"
                type="warning"
                plain
                @click="submitPaymentApproval(row.id)"
              >
                Submit Approval
              </el-button>
              <el-button
                v-if="canDisbursePayment && row.status === 'APPROVED'"
                size="small"
                type="success"
                @click="openMarkPaidModal(row)"
              >
                Mark Paid
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- Final Settlement & Warranty Retainage Release -->
      <el-card shadow="never" style="margin-bottom: 18px">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center">
            <strong>Final Settlement & Warranty Retainage Release</strong>
            <div style="display: flex; gap: 10px">
              <el-button
                v-if="!settlement && canManageVariations && contract.status === 'ACTIVE'"
                size="small"
                type="primary"
                @click="openSettlementDialog"
              >
                + Initiate Final Settlement
              </el-button>
              <el-button
                v-if="settlement && canManageVariations && (settlement.status === 'DRAFT' || settlement.status === 'REJECTED')"
                size="small"
                type="warning"
                :loading="actionLoading"
                @click="submitSettlementApproval"
              >
                Submit Settlement for 3-Step Approval
              </el-button>
              <el-button
                v-if="settlement && canDisbursePayment && settlement.status === 'APPROVED'"
                size="small"
                type="success"
                :loading="actionLoading"
                @click="releaseWarrantyRetainage"
              >
                Release Warranty Retainage & Close Contract
              </el-button>
            </div>
          </div>
        </template>

        <div v-if="!settlement" style="color: #64748b; font-size: 13px">
          Final settlement has not been initiated for this contract.
        </div>
        <el-descriptions v-else :column="3" border size="small">
          <el-descriptions-item label="Settlement No.">{{ settlement.settlementNo }}</el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag size="small">{{ settlement.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Completion / Warranty Expiry">
            {{ settlement.completionDate }} &rarr; {{ settlement.warrantyExpiryDate }}
          </el-descriptions-item>
          <el-descriptions-item label="Final Measured Net">
            {{ formatCurrency(settlement.finalMeasuredNet, contract.currency) }}
          </el-descriptions-item>
          <el-descriptions-item label="Final Tax Amount">
            {{ formatCurrency(settlement.finalTaxAmount, contract.currency) }}
          </el-descriptions-item>
          <el-descriptions-item label="Final Contract Gross">
            <strong>{{ formatCurrency(settlement.finalContractGross, contract.currency) }}</strong>
          </el-descriptions-item>
          <el-descriptions-item label="Previous Certified Gross">
            {{ formatCurrency(settlement.totalPreviousCertifiedGross, contract.currency) }}
          </el-descriptions-item>
          <el-descriptions-item label="Total Retainage Held">
            {{ formatCurrency(settlement.totalRetainageHeld, contract.currency) }}
          </el-descriptions-item>
          <el-descriptions-item label="Retainage Released">
            {{ formatCurrency(settlement.retainageReleasedNow, contract.currency) }}
          </el-descriptions-item>
          <el-descriptions-item label="Warranty Retainage Held">
            {{ formatCurrency(settlement.warrantyRetainageHeld, contract.currency) }}
          </el-descriptions-item>
          <el-descriptions-item label="Deductions & Claims">
            {{ formatCurrency(settlement.deductionsAndClaims, contract.currency) }}
          </el-descriptions-item>
          <el-descriptions-item label="Final Closing Payable">
            <strong style="color: #0284c7">{{ formatCurrency(settlement.finalClosingPayable, contract.currency) }}</strong>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-row :gutter="18">
        <!-- Approval Workflow History -->
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center">
                <strong>Contract Approval Workflow Timeline</strong>
                <el-button size="small" text type="primary" @click="router.push('/approvals')">
                  Go to Approvals Inbox &rarr;
                </el-button>
              </div>
            </template>
            <div v-if="workflows.length === 0" style="color: #64748b; font-size: 13px">
              No approval workflows submitted yet.
            </div>
            <div
              v-for="wf in workflows"
              :key="wf.id"
              style="margin-bottom: 16px; border-bottom: 1px solid #e2e8f0; padding-bottom: 12px"
            >
              <div style="display: flex; justify-content: space-between; margin-bottom: 8px">
                <strong>Workflow #{{ wf.id }} (Submitted by {{ wf.submittedBy }})</strong>
                <el-tag size="small">{{ wf.status }}</el-tag>
              </div>
              <el-timeline>
                <el-timeline-item
                  v-for="step in wf.steps"
                  :key="step.id"
                  :timestamp="step.decidedAt || 'Pending'"
                >
                  <div>
                    <strong>Step {{ step.stepOrder }}: {{ step.stepName }}</strong>
                    <el-tag size="small" style="margin-left: 6px">{{ step.status }}</el-tag>
                  </div>
                  <div style="font-size: 12px; color: #64748b">
                    Role: {{ step.requiredRole.replace('ROLE_', '') }}
                    <span v-if="step.actorUsername"> | Actor: {{ step.actorUsername }}</span>
                  </div>
                  <div v-if="step.comments" style="font-size: 12px; color: #334155">
                    "{{ step.comments }}"
                  </div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </el-card>
        </el-col>

        <!-- Versioned Document Attachments -->
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>
              <strong>Versioned Contract Documents & SHA-256 Registry</strong>
            </template>

            <div class="upload-bar">
              <el-select v-model="uploadDocType" size="small" style="width: 190px">
                <el-option label="SIGNED_CONTRACT" value="SIGNED_CONTRACT" />
                <el-option label="BOQ_ATTACHMENT" value="BOQ_ATTACHMENT" />
                <el-option label="CHANGE_ORDER_SUPPORT" value="CHANGE_ORDER_SUPPORT" />
                <el-option label="PAYMENT_CERTIFICATE" value="PAYMENT_CERTIFICATE" />
                <el-option label="GENERAL" value="GENERAL" />
              </el-select>
              <input ref="fileInput" type="file" style="font-size: 13px" />
              <el-button size="small" type="primary" :loading="uploading" @click="handleUpload">
                Upload Revision
              </el-button>
            </div>

            <el-table :data="documents" border stripe size="small" style="width: 100%; margin-top: 12px">
              <el-table-column prop="originalFilename" label="Filename" min-width="160" />
              <el-table-column prop="documentType" label="Type" width="140" />
              <el-table-column label="Ver." width="65" align="center">
                <template #default="{ row }">
                  <el-tag size="small" type="info">v{{ row.versionNo }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="SHA-256" width="120">
                <template #default="{ row }">
                  <code style="font-size: 11px">{{ row.sha256Checksum.slice(0, 10) }}...</code>
                </template>
              </el-table-column>
              <el-table-column label="" width="95" align="center">
                <template #default="{ row }">
                  <el-button size="small" type="primary" link @click="downloadDoc(row)">
                    Download
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <!-- Create Change Order Dialog -->
      <el-dialog v-model="coDialogVisible" title="Draft Contract Change Order" width="580px">
        <el-form :model="coForm" label-width="175px">
          <el-form-item label="Change Order No." required>
            <el-input v-model="coForm.changeOrderNo" placeholder="e.g. CO-2026-001" />
          </el-form-item>
          <el-form-item label="Variation Type" required>
            <el-select v-model="coForm.changeType" style="width: 100%">
              <el-option label="SCOPE_ADDITION" value="SCOPE_ADDITION" />
              <el-option label="SCOPE_DEDUCTION" value="SCOPE_DEDUCTION" />
              <el-option label="PRICE_ADJUSTMENT" value="PRICE_ADJUSTMENT" />
              <el-option label="TIME_EXTENSION" value="TIME_EXTENSION" />
            </el-select>
          </el-form-item>
          <el-form-item label="Title" required>
            <el-input v-model="coForm.title" placeholder="Summary of variation" />
          </el-form-item>
          <el-form-item label="Net Amount Delta" required>
            <el-input-number
              v-model="coForm.amountDeltaNet"
              :precision="2"
              :step="10000"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="Schedule Ext. (Days)">
            <el-input-number v-model="coForm.scheduleExtensionDays" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="Engineering Reason" required>
            <el-input v-model="coForm.reason" type="textarea" :rows="3" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="coDialogVisible = false">Cancel</el-button>
          <el-button type="primary" :loading="actionLoading" @click="createChangeOrder">
            Create Change Order
          </el-button>
        </template>
      </el-dialog>

      <!-- Create Payment Application Dialog -->
      <el-dialog v-model="payDialogVisible" title="New Progress Payment Application (IPC)" width="620px">
        <el-form :model="payForm" label-width="210px">
          <el-form-item label="Application No. (IPC)" required>
            <el-input v-model="payForm.applicationNo" placeholder="e.g. IPC-001" />
          </el-form-item>
          <el-form-item label="Period Start Date" required>
            <el-date-picker
              v-model="payForm.periodStart"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="Period End Date" required>
            <el-date-picker
              v-model="payForm.periodEnd"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="Previous Certified Gross">
            <strong>{{ formatCurrency(certifiedTotalGross, contract.currency) }}</strong>
          </el-form-item>
          <el-form-item label="Cumulative Completed Gross" required>
            <el-input-number
              v-model="payForm.cumulativeCompletedGross"
              :min="certifiedTotalGross + 1"
              :max="Number(contract.currentAmountGross)"
              :precision="2"
              :step="50000"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="Advance Deduction">
            <el-input-number v-model="payForm.advanceDeduction" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="Other Deductions">
            <el-input-number v-model="payForm.otherDeductions" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="Remarks">
            <el-input v-model="payForm.remarks" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>

        <div class="ipc-preview-box">
          <div>
            Current Period Gross:
            <strong>{{ formatCurrency(ipcPreview.currentPeriodGross, contract.currency) }}</strong>
          </div>
          <div>
            Retainage Withheld ({{ formatPercent(contract.retainageRate) }}):
            <strong>{{ formatCurrency(ipcPreview.retainageWithheld, contract.currency) }}</strong>
          </div>
          <div>
            Net Payable Amount:
            <strong style="color: #0284c7">{{ formatCurrency(ipcPreview.netPayableAmount, contract.currency) }}</strong>
          </div>
        </div>

        <template #footer>
          <el-button @click="payDialogVisible = false">Cancel</el-button>
          <el-button type="primary" :loading="actionLoading" @click="createPaymentApplication">
            Create Payment Application
          </el-button>
        </template>
      </el-dialog>

      <!-- Mark Paid Dialog -->
      <el-dialog v-model="paidDialogVisible" title="Record Bank Disbursement Reference" width="460px">
        <el-form label-position="top">
          <el-form-item label="Bank Wire / Payment Reference No." required>
            <el-input v-model="paymentRefInput" placeholder="e.g. WIRE-20260405-9901" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="paidDialogVisible = false">Cancel</el-button>
          <el-button type="success" :loading="actionLoading" @click="confirmMarkPaid">
            Confirm Disbursement
          </el-button>
        </template>
      </el-dialog>

      <!-- Initiate Final Settlement Dialog -->
      <el-dialog v-model="settlementDialogVisible" title="Initiate Final Contract Settlement" width="600px">
        <el-form :model="settlementForm" label-width="210px">
          <el-form-item label="Settlement No." required>
            <el-input v-model="settlementForm.settlementNo" />
          </el-form-item>
          <el-form-item label="Final Measured Net Amount" required>
            <el-input-number
              v-model="settlementForm.finalMeasuredNet"
              :min="1"
              :precision="2"
              :step="50000"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="Warranty Retainage Held">
            <el-input-number
              v-model="settlementForm.warrantyRetainageHeld"
              :min="0"
              :max="retainageHeldTotal"
              :precision="2"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="Deductions & Claims">
            <el-input-number
              v-model="settlementForm.deductionsAndClaims"
              :min="0"
              :precision="2"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="Substantial Completion Date" required>
            <el-date-picker
              v-model="settlementForm.completionDate"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="Final Reconciliation Remarks">
            <el-input v-model="settlementForm.remarks" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="settlementDialogVisible = false">Cancel</el-button>
          <el-button type="primary" :loading="actionLoading" @click="createFinalSettlement">
            Create Final Settlement
          </el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http, extractErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { formatCurrency, formatPercent, previewPaymentApplication } from '../utils/money'
import type {
  ApprovalWorkflow,
  ChangeOrder,
  ChangeOrderType,
  Contract,
  DocumentAttachment,
  DocumentType,
  FinalSettlement,
  PaymentApplication
} from '../types/api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const actionLoading = ref(false)
const contract = ref<Contract | null>(null)
const workflows = ref<ApprovalWorkflow[]>([])
const documents = ref<DocumentAttachment[]>([])
const changeOrders = ref<ChangeOrder[]>([])
const payments = ref<PaymentApplication[]>([])
const settlement = ref<FinalSettlement | null>(null)

const settlementDialogVisible = ref(false)
const settlementForm = reactive({
  settlementNo: '',
  finalMeasuredNet: 0,
  warrantyRetainageHeld: 0,
  deductionsAndClaims: 0,
  completionDate: '2026-11-15',
  remarks: ''
})

const uploadDocType = ref<DocumentType>('SIGNED_CONTRACT')
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)

const canManageContract = computed(() =>
  auth.hasAnyRole('ROLE_SYSTEM_ADMIN', 'ROLE_PROJECT_MANAGER', 'ROLE_CONTRACT_MANAGER')
)
const canManageVariations = computed(() =>
  auth.hasAnyRole('ROLE_SYSTEM_ADMIN', 'ROLE_PROJECT_MANAGER', 'ROLE_CONTRACT_MANAGER', 'ROLE_COST_ENGINEER')
)
const canDisbursePayment = computed(() =>
  auth.hasAnyRole('ROLE_SYSTEM_ADMIN', 'ROLE_FINANCE_OFFICER')
)

const certifiedTotalGross = computed(() =>
  payments.value
    .filter((p) => p.status === 'APPROVED' || p.status === 'PAID')
    .reduce((acc, p) => acc + Number(p.currentPeriodGross), 0)
)
const retainageHeldTotal = computed(() =>
  payments.value
    .filter((p) => p.status === 'APPROVED' || p.status === 'PAID')
    .reduce((acc, p) => acc + Number(p.retainageWithheld), 0)
)

const coDialogVisible = ref(false)
const coForm = reactive({
  changeOrderNo: '',
  changeType: 'SCOPE_ADDITION' as ChangeOrderType,
  title: '',
  amountDeltaNet: 50000,
  scheduleExtensionDays: 0,
  reason: ''
})

const payDialogVisible = ref(false)
const payForm = reactive({
  applicationNo: '',
  periodStart: '2026-04-01',
  periodEnd: '2026-04-30',
  cumulativeCompletedGross: 100000,
  advanceDeduction: 0,
  otherDeductions: 0,
  remarks: ''
})

const ipcPreview = computed(() =>
  previewPaymentApplication(
    payForm.cumulativeCompletedGross,
    certifiedTotalGross.value,
    Number(contract.value?.retainageRate || 0),
    payForm.advanceDeduction,
    payForm.otherDeductions
  )
)

const paidDialogVisible = ref(false)
const selectedPaymentId = ref<number | null>(null)
const paymentRefInput = ref('')

async function loadContractWorkspace() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const { data } = await http.get<Contract>(`/contracts/${id}`)
    contract.value = data

    const [wfRes, docRes, coRes, payRes, setRes] = await Promise.all([
      http.get<ApprovalWorkflow[]>('/approvals/by-target', {
        params: { targetType: 'CONTRACT', targetId: id }
      }),
      http.get<DocumentAttachment[]>('/documents', {
        params: { projectId: data.projectId, ownerType: 'CONTRACT', ownerId: id }
      }),
      http.get<ChangeOrder[]>(`/contracts/${id}/change-orders`),
      http.get<PaymentApplication[]>(`/contracts/${id}/payments`),
      http.get<FinalSettlement>(`/contracts/${id}/settlement`)
    ])
    workflows.value = wfRes.data
    documents.value = docRes.data
    changeOrders.value = coRes.data
    payments.value = payRes.data
    settlement.value = setRes.status === 200 ? setRes.data : null
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    loading.value = false
  }
}

async function submitForApproval() {
  if (!contract.value) return
  actionLoading.value = true
  try {
    await http.post(`/contracts/${contract.value.id}/submit-approval`)
    ElMessage.success('Contract submitted for multi-step approval')
    await loadContractWorkspace()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

async function activateContract() {
  if (!contract.value) return
  actionLoading.value = true
  try {
    await http.post(`/contracts/${contract.value.id}/activate`)
    ElMessage.success('Contract is now ACTIVE')
    await loadContractWorkspace()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

async function createChangeOrder() {
  if (!contract.value) return
  actionLoading.value = true
  try {
    await http.post(`/contracts/${contract.value.id}/change-orders`, coForm)
    ElMessage.success('Change order created')
    coDialogVisible.value = false
    await loadContractWorkspace()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

async function submitChangeOrderApproval(coId: number) {
  actionLoading.value = true
  try {
    await http.post(`/change-orders/${coId}/submit-approval`)
    ElMessage.success('Change order submitted to Approval Inbox')
    await loadContractWorkspace()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

function openPaymentDialog() {
  const nextSeq = String(payments.value.length + 1).padStart(3, '0')
  payForm.applicationNo = `IPC-${contract.value?.contractNo}-${nextSeq}`
  payForm.cumulativeCompletedGross = Math.min(
    Number(contract.value?.currentAmountGross || 0),
    certifiedTotalGross.value + 100000
  )
  payForm.advanceDeduction = 0
  payForm.otherDeductions = 0
  payForm.remarks = ''
  payDialogVisible.value = true
}

async function createPaymentApplication() {
  if (!contract.value) return
  actionLoading.value = true
  try {
    await http.post(`/contracts/${contract.value.id}/payments`, payForm)
    ElMessage.success('Progress payment application created')
    payDialogVisible.value = false
    await loadContractWorkspace()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

async function submitPaymentApproval(paymentId: number) {
  actionLoading.value = true
  try {
    await http.post(`/payments/${paymentId}/submit-approval`)
    ElMessage.success('Payment application submitted for certification & approval')
    await loadContractWorkspace()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

function openMarkPaidModal(payment: PaymentApplication) {
  selectedPaymentId.value = payment.id
  paymentRefInput.value = ''
  paidDialogVisible.value = true
}

async function confirmMarkPaid() {
  if (!selectedPaymentId.value) return
  actionLoading.value = true
  try {
    await http.post(`/payments/${selectedPaymentId.value}/mark-paid`, {
      paymentReference: paymentRefInput.value
    })
    ElMessage.success('Payment marked as PAID')
    paidDialogVisible.value = false
    await loadContractWorkspace()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

function openSettlementDialog() {
  if (!contract.value) return
  settlementForm.settlementNo = `FS-${contract.value.contractNo}`
  settlementForm.finalMeasuredNet = Number(contract.value.currentAmountNet)
  settlementForm.warrantyRetainageHeld = 0
  settlementForm.deductionsAndClaims = 0
  settlementForm.completionDate = contract.value.endDate
  settlementForm.remarks = ''
  settlementDialogVisible.value = true
}

async function createFinalSettlement() {
  if (!contract.value) return
  actionLoading.value = true
  try {
    await http.post(`/contracts/${contract.value.id}/settlement`, settlementForm)
    ElMessage.success('Final settlement created')
    settlementDialogVisible.value = false
    await loadContractWorkspace()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

async function submitSettlementApproval() {
  if (!settlement.value) return
  actionLoading.value = true
  try {
    await http.post(`/settlements/${settlement.value.id}/submit-approval`)
    ElMessage.success('Final settlement submitted for 3-step approval')
    await loadContractWorkspace()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

async function releaseWarrantyRetainage() {
  if (!settlement.value) return
  actionLoading.value = true
  try {
    await http.post(`/settlements/${settlement.value.id}/release-warranty-retainage`)
    ElMessage.success('Warranty retainage released and contract CLOSED')
    await loadContractWorkspace()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

async function handleUpload() {
  if (!contract.value) return
  const file = fileInput.value?.files?.[0]
  if (!file) {
    ElMessage.warning('Please select a file to upload')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('projectId', String(contract.value.projectId))
    formData.append('ownerType', 'CONTRACT')
    formData.append('ownerId', String(contract.value.id))
    formData.append('documentType', uploadDocType.value)
    formData.append('file', file)

    await http.post('/documents', formData)
    ElMessage.success('Document uploaded & checksum verified')
    if (fileInput.value) fileInput.value.value = ''
    await loadContractWorkspace()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    uploading.value = false
  }
}

async function downloadDoc(doc: DocumentAttachment) {
  try {
    const res = await http.get(`/documents/${doc.id}/download`, { responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', doc.originalFilename)
    document.body.appendChild(link)
    link.click()
    link.remove()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  }
}

onMounted(() => {
  loadContractWorkspace()
})
</script>

<style scoped>
.kpi-label {
  font-size: 12px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.kpi-value {
  font-size: 22px;
  font-weight: 700;
  margin: 6px 0;
  color: #0f172a;
}
.kpi-highlight {
  color: #0284c7;
}
.kpi-sub {
  font-size: 12px;
  color: #64748b;
}
.upload-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.ipc-preview-box {
  margin-top: 12px;
  padding: 12px 16px;
  background-color: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}
</style>
