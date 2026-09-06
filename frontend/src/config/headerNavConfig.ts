export type HeaderNavSubpage = {
  id: number;
  href: string;
  /** API 등에서 오는 동적 세트명 / DB extras 자유 텍스트 */
  name?: string;
  /** 정적 메뉴 라벨 (headerNav namespace 기준) */
  labelKey?: string;
};

export type HeaderNavPage = {
  id: number;
  href: string;
  /** DB extras 등 자유 텍스트 라벨 (있으면 i18n보다 우선) */
  title?: string;
  /** headerNav namespace 기준 라벨 키 (예: games.mtg) — 게임 메뉴용 */
  labelKey?: string;
  subpages: HeaderNavSubpage[];
  /** MTG/SWU 등 세트 허브 링크 (있을 때만 “All Sets” 추가 링크로 사용) */
  allSetsHref?: string;
  /** 전체 상품 링크 (있을 때만 “All Products” 추가 링크로 사용) */
  allProductsHref?: string;
  /** true면 API 최신 N개 세트를 붙이지 않고 config subpages만 사용 */
  skipLatestSets?: boolean;
};

/** 게임 메뉴만 하드코딩. Preorder/Event Ticket 등은 DB main_header extras. */
export const headerNavPages: HeaderNavPage[] = [
  {
    id: 1,
    labelKey: "games.mtg",
    href: "/mtg",
    subpages: [
      { id: 1, labelKey: "allSets", href: "/game/mtg" },
      { id: 2, labelKey: "sealedProducts", href: "/game/mtg/sealed" },
      {
        id: 3,
        labelKey: "preorder",
        href: "/special/products?manualCategories=Preorder%20Products&page=0&entryState=UPDATED",
      },
    ],
    allSetsHref: "/game/mtg",
  },
  {
    id: 2,
    labelKey: "games.fab",
    href: "/fab",
    subpages: [
      { id: 1, labelKey: "allSets", href: "/game/fab" },
      { id: 2, labelKey: "sealedProducts", href: "/game/fab/sealed" },
      {
        id: 3,
        labelKey: "preorder",
        href: "/special/products?manualCategories=Preorder%20Products&page=0&entryState=UPDATED",
      },
    ],
    allSetsHref: "/game/fab",
  },
  // 임시 - LORC 제거, 추후 게임 추가 시 아래 형식 참고
  {
    id: 3,
    labelKey: "games.swu",
    href: "/swu",
    subpages: [
      { id: 1, labelKey: "allSets", href: "/game/swu" },
      { id: 2, labelKey: "sealedProducts", href: "/game/swu/sealed" },
      {
        id: 3,
        labelKey: "preorder",
        href: "/special/products?manualCategories=Preorder%20Products&page=0&entryState=UPDATED",
      },
    ],
    allSetsHref: "/game/swu",
  },
  // {
  //   id: 4,
  //   labelKey: "games.lorc",
  //   href: "/lorc",
  //   subpages: [{ id: 1, labelKey: "allSets", href: "/game/lorc" }],
  //   allSetsHref: "/game/lorc",
  // },
  {
    id: 5,
    labelKey: "games.rift",
    href: "/rift",
    skipLatestSets: true,
    subpages: [
      { id: 1, name: "Origins", href: "/game/rift/origins" },
      { id: 2, name: "Origins: Proving Grounds", href: "/game/rift/origins-proving-grounds" },
      { id: 3, name: "Riftbound Promotional Cards", href: "/game/rift/riftbound-promotional-cards" },
      { id: 4, labelKey: "allSets", href: "/game/rift" },
      { id: 5, labelKey: "sealedProducts", href: "/game/rift/sealed" },
      {
        id: 6,
        labelKey: "preorder",
        href: "/special/products?manualCategories=Preorder%20Products&page=0&entryState=UPDATED",
      },
    ],
    allSetsHref: "/game/rift",
  },
  // {
  //   id: 6,
  //   labelKey: "supplies.title",
  //   href: "/supplies",
  //   subpages: [
  //     { id: 1, labelKey: "playmat", href: "/supplies/category/playmat" },
  //     { id: 2, labelKey: "deckbox", href: "/supplies/category/deckbox" },
  //     { id: 3, labelKey: "protecter", href: "/supplies/category/protecter" },
  //     { id: 4, labelKey: "binder", href: "/supplies/category/binder" },
  //   ],
  //   allProductsHref: "/supplies/allproducts",
  // },
];
