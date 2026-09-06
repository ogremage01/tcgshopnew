/** product_category.name_en / search_map.manual_category 기준 */
export const MANUAL_CATEGORY_PREORDER = "Preorder Products";
export const MANUAL_CATEGORY_EVENT_TICKET = "Event Ticket";

const MANUAL_CATEGORY_LABEL: Record<string, string> = {
  [MANUAL_CATEGORY_PREORDER]: "프리오더",
  preorder: "프리오더",
  [MANUAL_CATEGORY_EVENT_TICKET]: "이벤트 티켓",
  "event-ticket": "이벤트 티켓",
  eventticket: "이벤트 티켓",
};

/** 로컬 시드명·예전 슬러그 → 운영 카테고리명 */
const MANUAL_CATEGORY_ALIASES: Record<string, string> = {
  preorder: MANUAL_CATEGORY_PREORDER,
  "Pre-order": MANUAL_CATEGORY_PREORDER,
  "Pre-Order": MANUAL_CATEGORY_PREORDER,
  [MANUAL_CATEGORY_PREORDER]: MANUAL_CATEGORY_PREORDER,
  "event-ticket": MANUAL_CATEGORY_EVENT_TICKET,
  eventticket: MANUAL_CATEGORY_EVENT_TICKET,
  [MANUAL_CATEGORY_EVENT_TICKET]: MANUAL_CATEGORY_EVENT_TICKET,
};

export function canonicalizeManualCategory(value: string): string {
  return MANUAL_CATEGORY_ALIASES[value] ?? value;
}

export function formatManualCategoryLabel(value: string): string {
  const canonical = canonicalizeManualCategory(value);
  return MANUAL_CATEGORY_LABEL[canonical] ?? canonical;
}
