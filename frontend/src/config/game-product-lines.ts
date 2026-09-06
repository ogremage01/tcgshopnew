/** TCGP product line IDs — game hub pages와 sitemap 세트 수집에 공통 사용 */
export const FAB_PRODUCT_LINE_ID = 62;
export const SWU_PRODUCT_LINE_ID = 79;
export const LORC_PRODUCT_LINE_ID = 71;
export const RIFT_PRODUCT_LINE_ID = 89;

export const TCGP_GAME_SET_SOURCES = [
  { game: "fab", productLineId: FAB_PRODUCT_LINE_ID },
  { game: "swu", productLineId: SWU_PRODUCT_LINE_ID },
  { game: "lorc", productLineId: LORC_PRODUCT_LINE_ID },
  { game: "rift", productLineId: RIFT_PRODUCT_LINE_ID },
] as const;
