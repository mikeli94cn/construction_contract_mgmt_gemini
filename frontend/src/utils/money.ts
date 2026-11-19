import type { ContractLineItem } from '../types/api'

/**
 * Deterministic 4-decimal rounding helper for client-side BoQ previews.
 * Final authoritative calculation is enforced on the server using Java BigDecimal.
 */
export function round4(val: number): number {
  if (!Number.isFinite(val)) return 0
  return Math.round((val + Number.EPSILON) * 10000) / 10000
}

export function calculateLineItemTotal(quantity: number, unitPriceNet: number): number {
  return round4(Number(quantity || 0) * Number(unitPriceNet || 0))
}

export function calculateContractTotals(lineItems: ContractLineItem[], taxRate: number) {
  const net = round4(
    lineItems.reduce((acc, item) => acc + calculateLineItemTotal(item.quantity, item.unitPriceNet), 0)
  )
  const tax = round4(net * Number(taxRate || 0))
  const gross = round4(net + tax)
  return { net, tax, gross }
}

export function formatCurrency(amount: number | string | undefined | null, currency = 'USD'): string {
  const numeric = Number(amount ?? 0)
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency || 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(numeric)
}

export function formatPercent(rate: number | string | undefined | null): string {
  const numeric = Number(rate ?? 0) * 100
  return `${numeric.toFixed(2)}%`
}

export function previewPaymentApplication(
  cumulativeCompletedGross: number,
  previousCertifiedGross: number,
  retainageRate: number,
  advanceDeduction: number,
  otherDeductions: number
) {
  const currentPeriodGross = round4(Number(cumulativeCompletedGross || 0) - Number(previousCertifiedGross || 0))
  const retainageWithheld = round4(currentPeriodGross * Number(retainageRate || 0))
  const netPayableAmount = round4(
    currentPeriodGross - retainageWithheld - Number(advanceDeduction || 0) - Number(otherDeductions || 0)
  )
  return {
    currentPeriodGross,
    retainageWithheld,
    netPayableAmount
  }
}

export function previewFinalSettlement(
  finalMeasuredNet: number,
  taxRate: number,
  totalPreviousCertifiedGross: number,
  totalRetainageHeld: number,
  warrantyRetainageHeld: number,
  deductionsAndClaims: number
) {
  const finalTaxAmount = round4(Number(finalMeasuredNet || 0) * Number(taxRate || 0))
  const finalContractGross = round4(Number(finalMeasuredNet || 0) + finalTaxAmount)
  const retainageReleasedNow = round4(Number(totalRetainageHeld || 0) - Number(warrantyRetainageHeld || 0))
  const finalClosingPayable = round4(
    finalContractGross -
      Number(totalPreviousCertifiedGross || 0) +
      retainageReleasedNow -
      Number(deductionsAndClaims || 0)
  )
  return {
    finalTaxAmount,
    finalContractGross,
    retainageReleasedNow,
    finalClosingPayable
  }
}


