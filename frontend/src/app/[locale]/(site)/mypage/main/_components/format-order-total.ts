export function formatOrderTotal(orderTotal: number, _paymentCurrency?: string) {
    return `₩ ${orderTotal.toLocaleString()}`;
}
