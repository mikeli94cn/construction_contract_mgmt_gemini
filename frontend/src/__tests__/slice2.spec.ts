import { describe, expect, it } from 'vitest'
import type { ApprovalWorkflow } from '../types/api'

function canUserDecideActiveStep(wf: ApprovalWorkflow, userRoles: string[]): boolean {
  if (wf.status !== 'PENDING') return false
  const active = wf.steps.find((s) => s.stepOrder === wf.currentStepOrder)
  if (!active) return false
  return userRoles.includes('ROLE_SYSTEM_ADMIN') || userRoles.includes(active.requiredRole)
}

describe('Slice 2 Approval Workflow Step Role Gate', () => {
  const sampleWorkflow: ApprovalWorkflow = {
    id: 10,
    targetType: 'CONTRACT',
    targetId: 1,
    projectId: 1,
    referenceNo: 'CNT-TUN-2026-001',
    title: 'TBM Boring Package',
    amountGross: 11000000,
    status: 'PENDING',
    currentStepOrder: 1,
    submittedBy: 'cm.elena',
    submittedAt: '2026-11-18T10:00:00Z',
    completedAt: null,
    version: 0,
    steps: [
      {
        id: 1,
        stepOrder: 1,
        stepName: 'Cost Engineer Commercial Review',
        requiredRole: 'ROLE_COST_ENGINEER',
        status: 'PENDING',
        actorUsername: null,
        comments: null,
        decidedAt: null
      },
      {
        id: 2,
        stepOrder: 2,
        stepName: 'Project Manager Endorsement',
        requiredRole: 'ROLE_PROJECT_MANAGER',
        status: 'WAITING',
        actorUsername: null,
        comments: null,
        decidedAt: null
      }
    ]
  }

  it('allows Cost Engineer to decide Step 1 and blocks Project Manager until Step 2', () => {
    expect(canUserDecideActiveStep(sampleWorkflow, ['ROLE_COST_ENGINEER'])).toBe(true)
    expect(canUserDecideActiveStep(sampleWorkflow, ['ROLE_PROJECT_MANAGER'])).toBe(false)
    expect(canUserDecideActiveStep(sampleWorkflow, ['ROLE_SYSTEM_ADMIN'])).toBe(true)
  })
})
