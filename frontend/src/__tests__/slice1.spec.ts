import { describe, expect, it } from 'vitest'
import {
  calculateContractTotals,
  calculateLineItemTotal,
  formatCurrency,
  formatPercent
} from '../utils/money'
import type { ContractLineItem } from '../types/api'

describe('Slice 1 Frontend Financial & BoQ Calculations', () => {
  it('computes exact line item totals with 4-decimal rounding', () => {
    expect(calculateLineItemTotal(1250.5, 40)).toBe(50020)
    expect(calculateLineItemTotal(800, 187.475)).toBe(149980)
  })

  it('computes contract net, tax, and gross totals across multiple BoQ line items', () => {
    const items: ContractLineItem[] = [
      {
        itemCode: 'BOQ-101',
        description: 'Excavation',
        unit: 'm3',
        quantity: 1250.5,
        unitPriceNet: 40
      },
      {
        itemCode: 'BOQ-102',
        description: 'Structural Concrete',
        unit: 'm3',
        quantity: 800,
        unitPriceNet: 187.475
      }
    ]
    const totals = calculateContractTotals(items, 0.09)
    expect(totals.net).toBe(200000)
    expect(totals.tax).toBe(18000)
    expect(totals.gross).toBe(218000)
  })

  it('formats currency and percentages consistently for construction displays', () => {
    expect(formatCurrency(16200000, 'USD')).toBe('$16,200,000.00')
    expect(formatPercent(0.05)).toBe('5.00%')
    expect(formatPercent(0.085)).toBe('8.50%')
  })
})
