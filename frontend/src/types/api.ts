export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface UserProfile {
  id: number
  username: string
  email: string
  fullName: string
  organizationId: number
  organizationCode: string
  organizationName: string
  enabled: boolean
  roles: string[]
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresAt: string
  user: UserProfile
}

export interface Organization {
  id: number
  code: string
  name: string
  parentId: number | null
  active: boolean
}

export type ProjectStatus = 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CLOSED'
export type ProjectRole = 'PROJECT_MANAGER' | 'CONTRACT_MANAGER' | 'COST_ENGINEER' | 'FINANCE_OFFICER' | 'VIEWER'

export interface Project {
  id: number
  projectCode: string
  name: string
  organizationId: number
  organizationCode: string
  organizationName: string
  location: string
  currency: string
  approvedBudget: number
  startDate: string
  plannedEndDate: string
  status: ProjectStatus
  description: string | null
  version: number
  createdAt: string
  updatedAt: string
  createdBy: string
}

export interface ProjectMember {
  id: number
  projectId: number
  userId: number
  username: string
  fullName: string
  email: string
  projectRole: ProjectRole
  assignedAt: string
  assignedBy: string
}

export type PartnerType = 'OWNER' | 'GENERAL_CONTRACTOR' | 'SUBCONTRACTOR' | 'SUPPLIER' | 'CONSULTANT'
export type PartnerStatus = 'ACTIVE' | 'SUSPENDED' | 'BLACKLISTED'

export interface BusinessPartner {
  id: number
  partnerCode: string
  name: string
  partnerType: PartnerType
  taxRegistrationNo: string
  qualificationLevel: string | null
  contactPerson: string
  contactPhone: string
  contactEmail: string
  bankName: string | null
  bankAccountNo: string | null
  status: PartnerStatus
  version: number
  createdAt: string
  updatedAt: string
  createdBy: string
}

export type ContractCategory = 'MAIN_CONTRACT' | 'SUBCONTRACT' | 'SUPPLY_CONTRACT' | 'CONSULTING_CONTRACT'
export type ContractStatus = 'DRAFT' | 'IN_APPROVAL' | 'APPROVED' | 'ACTIVE' | 'SETTLED' | 'CLOSED' | 'TERMINATED'

export interface ContractLineItem {
  id?: number
  itemCode: string
  description: string
  unit: string
  quantity: number
  unitPriceNet: number
  totalPriceNet?: number
  sortOrder?: number
}

export interface Contract {
  id: number
  contractNo: string
  title: string
  projectId: number
  projectCode: string
  projectName: string
  partnerId: number
  partnerCode: string
  partnerName: string
  parentContractId: number | null
  parentContractNo: string | null
  category: ContractCategory
  currency: string
  originalAmountNet: number
  taxRate: number
  originalAmountTax: number
  originalAmountGross: number
  approvedChangeNet: number
  approvedChangeGross: number
  currentAmountNet: number
  currentAmountGross: number
  retainageRate: number
  advancePaymentRate: number
  signedDate: string | null
  startDate: string
  endDate: string
  defectLiabilityMonths: number
  status: ContractStatus
  scopeSummary: string | null
  lineItems: ContractLineItem[]
  version: number
  createdAt: string
  updatedAt: string
  createdBy: string
}

export type ApprovalTargetType = 'CONTRACT' | 'CHANGE_ORDER' | 'PAYMENT_APPLICATION' | 'FINAL_SETTLEMENT'
export type ApprovalWorkflowStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export type ApprovalStepStatus = 'PENDING' | 'WAITING' | 'APPROVED' | 'REJECTED'

export interface ApprovalStep {
  id: number
  stepOrder: number
  stepName: string
  requiredRole: string
  status: ApprovalStepStatus
  actorUsername: string | null
  comments: string | null
  decidedAt: string | null
}

export interface ApprovalWorkflow {
  id: number
  targetType: ApprovalTargetType
  targetId: number
  projectId: number
  referenceNo: string
  title: string
  amountGross: number
  status: ApprovalWorkflowStatus
  currentStepOrder: number
  submittedBy: string
  submittedAt: string
  completedAt: string | null
  version: number
  steps: ApprovalStep[]
}

export type DocumentOwnerType = 'PROJECT' | 'CONTRACT' | 'CHANGE_ORDER' | 'PAYMENT_APPLICATION' | 'FINAL_SETTLEMENT'
export type DocumentType =
  | 'SIGNED_CONTRACT'
  | 'BOQ_ATTACHMENT'
  | 'CHANGE_ORDER_SUPPORT'
  | 'PAYMENT_CERTIFICATE'
  | 'SETTLEMENT_STATEMENT'
  | 'GENERAL'

export interface DocumentAttachment {
  id: number
  projectId: number
  ownerType: DocumentOwnerType
  ownerId: number
  documentType: DocumentType
  originalFilename: string
  contentType: string
  fileSize: number
  sha256Checksum: string
  versionNo: number
  uploadedBy: string
  uploadedAt: string
}

export type ChangeOrderType = 'SCOPE_ADDITION' | 'SCOPE_DEDUCTION' | 'PRICE_ADJUSTMENT' | 'TIME_EXTENSION'
export type ChangeOrderStatus = 'DRAFT' | 'IN_APPROVAL' | 'APPROVED' | 'REJECTED'

export interface ChangeOrder {
  id: number
  contractId: number
  contractNo: string
  projectId: number
  changeOrderNo: string
  changeType: ChangeOrderType
  title: string
  reason: string
  amountDeltaNet: number
  taxRate: number
  amountDeltaTax: number
  amountDeltaGross: number
  scheduleExtensionDays: number
  status: ChangeOrderStatus
  approvedAt: string | null
  version: number
  createdAt: string
  createdBy: string
}

export type PaymentApplicationStatus = 'DRAFT' | 'IN_APPROVAL' | 'APPROVED' | 'REJECTED' | 'PAID'

export interface PaymentApplication {
  id: number
  contractId: number
  contractNo: string
  projectId: number
  applicationNo: string
  periodStart: string
  periodEnd: string
  cumulativeCompletedGross: number
  previousCertifiedGross: number
  currentPeriodGross: number
  retainageRate: number
  retainageWithheld: number
  advanceDeduction: number
  otherDeductions: number
  netPayableAmount: number
  status: PaymentApplicationStatus
  approvedAt: string | null
  paidAt: string | null
  paymentReference: string | null
  remarks: string | null
  version: number
  createdAt: string
  createdBy: string
}

export type FinalSettlementStatus =
  | 'DRAFT'
  | 'IN_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'WARRANTY_RELEASED'

export interface FinalSettlement {
  id: number
  contractId: number
  contractNo: string
  projectId: number
  settlementNo: string
  finalMeasuredNet: number
  taxRate: number
  finalTaxAmount: number
  finalContractGross: number
  totalPreviousCertifiedGross: number
  totalRetainageHeld: number
  retainageReleasedNow: number
  warrantyRetainageHeld: number
  deductionsAndClaims: number
  finalClosingPayable: number
  completionDate: string
  warrantyExpiryDate: string
  status: FinalSettlementStatus
  approvedAt: string | null
  warrantyReleasedAt: string | null
  remarks: string | null
  version: number
  createdAt: string
  createdBy: string
}

export interface ProjectFinancialSummary {
  projectId: number
  projectCode: string
  projectName: string
  currency: string
  projectStatus: string
  approvedBudget: number
  totalContracts: number
  activeContracts: number
  settledOrClosedContracts: number
  totalOriginalContractGross: number
  totalApprovedChangeOrdersGross: number
  totalCurrentCommittedGross: number
  remainingUncommittedBudget: number
  totalCertifiedPaymentsGross: number
  totalPaidNetAmount: number
  totalRetainageHeld: number
}

export interface AuditLogEntry {
  id: number
  occurredAt: string
  actorUserId: number | null
  actorUsername: string
  module: string
  entityType: string
  entityId: string
  action: string
  summary: string
  payloadJson: string | null
}



