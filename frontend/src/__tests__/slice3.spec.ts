import { describe, expect, it } from 'vitest'
import { previewPaymentApplication } from '../utils/money'

describe('Slice 3 Payment Application & Retainage Calculations', () => {
  it('calculates current period gross, retainage withheld, and net payable accurately', () => {
    const ipc1 = previewPaymentApplication(1000000, 0, 0.05, 50000, 0)
    expect(ipc1.currentPeriodGross).toBe(1000000)
    expect(ipc1.retainageWithheld).toBe(50000)
    expect(ipc1.netPayableAmount).toBe(900000)

    const ipc2 = previewPaymentApplication(2000000, 1000000, 0.05, 0, 0)
    expect(ipc2.currentPeriodGross).toBe(1000000)
    expect(ipc2.retainageWithheld).toBe(50000)
    expect(ipc2.netPayableAmount).toBe(950000)
  })
})
