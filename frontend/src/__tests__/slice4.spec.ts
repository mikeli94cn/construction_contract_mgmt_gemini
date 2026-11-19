import { describe, expect, it } from 'vitest'
import { previewFinalSettlement } from '../utils/money'

describe('Slice 4 Final Settlement Reconciliation Math', () => {
  it('computes final tax, final gross, released retainage, and closing payable accurately', () => {
    const res = previewFinalSettlement(1080000, 0.1, 1000000, 50000, 20000, 8000)
    expect(res.finalTaxAmount).toBe(108000)
    expect(res.finalContractGross).toBe(1188000)
    expect(res.retainageReleasedNow).toBe(30000)
    expect(res.finalClosingPayable).toBe(210000)
  })
})
