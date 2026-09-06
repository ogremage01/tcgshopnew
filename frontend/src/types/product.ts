export interface ProductSuggestItem {
  id: number;
  productName: string;
}

export interface ProductSuggestResponse {
  items: ProductSuggestItem[];
}

export interface Product {
  id: string;
  name: string;
  description?: string;
  price: number;
  imageUrl?: string;
}

export interface ProductSingleCard {
  id: string;
  name: string;
  description?: string;
  price: number;
  imageUrl?: string;
  condition: "NM" | "EX" | "VG" | "G";
}

export interface TcgPSyncGameDto {
  id: number;
  productLineId: number;
  productLineName: string;
  regexForCodeNumber: string;
}

export interface PriceConfigDto {
  configKey: string;
  /** GameEnum.game 풀네임 (예: "Magic: The Gathering") */
  configGame: string;
  configValue: number;
  updatedAt?: string;
}
export interface GradePricingPolicyDto {
  id: number;
  grade: string;
  percentage: number;
}
export interface StorageDto {
  id: number;
  storageName: string;
  description?: string;
  isDefault?: boolean;
}
/** 검색·목록 헤더용 슬림 카드 정보 (백엔드 UnionPriceSlimDto) */
export interface UnionPrice {
  id?: number;
  game?: string;
  productType?: string;
  printType?: string;
  printing?: string;
  setCode?: string;
  setNumber?: number;
  cardName?: string;
  cardNameK?: string;
  imageSource?: string;
  imageUrl?: string;
  checkCodeRefined?: string;
  rarity?: string;
  price?: number;
}

export type UnionPriceSlimDto = UnionPrice;

/** 싱글카드 관리 행 (백엔드 CardProductManagementResponseDto) */
export interface CardProductManagementResponseDto {
  id: number;
  name?: string;
  imageUrl?: string;
  productId: number;
  unionPrice?: UnionPrice | null;
  condition?: string;
  printType?: string;
  language?: string;
  isVisible?: boolean;
  isDeleted?: boolean;
  currentVisibleStock?: number;
  maxVisibleStock?: number;
  totalStock?: number;
  isAutoUpdatedStock?: boolean;
  storageId: number;
  isPriceLinked?: boolean;
  pricingRate?: number;
  price?: number;
  memo?: string;
}

export interface CardProductRegister {
  cardName: string;
  productId: number;
  productType: string;
  condition: string;
  printType: string;
  language: string;
  isVisible: boolean;
  currentVisibleStock: number;
  totalStock: number;
  maxVisibleStock: number;
  isAutoUpdatedStock: boolean;
  storageId: number;
  isPriceLinked: boolean;
  pricingRate: number;
  price: number;
  memo: string;
  unionPriceId: number;
}

export interface CardProductPatchRequest {
  isVisible?: boolean;
  isAutoUpdatedStock?: boolean;
  isPriceLinked?: boolean;
  storageId?: number;
  currentVisibleStock?: number;
  maxVisibleStock?: number;
  totalStock?: number;
  pricingRate?: number;
  price?: number;
  memo?: string;
}

export interface ExcelUploadRequestDto {
  gameId: number;
  setCode: string;
  language: string;
  storageId: number;
  isVisible: boolean;
  condition: string;
  maxVisibleStock: number;
}

export interface ExcelUploadResultDto {
  successCount: number;
  failCount: number;
  updatedCount: number;
  errors: ExcelRowErrorDto[];
}
export interface ExcelRowErrorDto {
  rowNumber: number;
  rowData: Record<string, any>;
  fieldErrors: ExcelRowErrorFieldDto[];
}
export interface ExcelRowErrorFieldDto {
  field: string;
  message: string;
}
export interface SearchBySetDto {
  game: string;
  set: string;
  printTypeFilter: "all" | "foil" | "normal";
  storageId: string;
}
export interface MakerDto {
  id: number;
  name: string;
}
export interface MakerCreateRequestDto {
  name: string;
}
export interface SupplyTypeDto {
  id: number;
  nameEn: string;
  nameKo: string;
}
export interface SupplyTypeCreateRequestDto {
  nameEn: string;
  nameKo: string;
}

export interface SupplyDto {
  id: number;
  publicId?: string;
  nameEn: string;
  nameKo: string;
  description?: string;
  price: number;
  stock: number;
  supplyType: string;
  maker: string;
  makerId?: number | null;
  supplyTypeId?: number | null;
  imgUrl?: string;
  isVisible?: boolean;
  isDeleted?: boolean;
  offlineProductId?: string;
}

export interface CardProductSaleDto {
  // 등록 ID(CardProduct.ID) — UnionPrice-only 참고 행에는 없을 수 있음
  id?: number | null;
  // 검색 매핑 ID(ProductSearchMap.ID)
  searchMapId?: number | null;
  // 카드 상태
  condition: string;
  // 표시 재고
  currentVisibleStock: number;
  // 표시 가격
  showingPrice: number;
  // 표시 가격-달러
  showingPriceUsd: number;
}

export interface SealedProductSaleDto {
  id?: number | null;
  searchMapId?: number | null;
  language: string;
  currentVisibleStock: number;
  showingPrice: number;
}

export interface SealedProductInfoDto {
  game: string;
  publicId: string;
  sealedProductSaleDtoMap: Record<string, SealedProductSaleDto[]>;
  gameSalesInfo: GameSalesInfoDto;
}

export interface SupplySaleDto {
  id: number;
  suppliesType: string;
  currentVisibleStock: number;
  showingPrice: number;
}

export interface CardProductInfoDto {
  game: string;
  cardProductSaleDtoMap: Record<string, CardProductSaleDto[]>;
  rarity: string;
  printType: string;
  printing: string;
  unionPriceId?: number;
  publicId: string;
  setCode: string;
  setNumber: string;
  gameSalesInfo: GameSalesInfoDto;
  setName: string;
}

export interface GameSalesInfoDto {
  brand: string;
  company: string;
  game: string;
  origin: string;
  recommendedAge: string;
}
/** Spring Data `Page<T>` JSON (백엔드 `org.springframework.data.domain.Page`) */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty?: boolean;
}

export interface SuppliesTypeFacetDto {
  nameEn: string;
  nameKo: string;
}

export interface SearchInitResponseDto {
  products: PageResponse<ProductItemDto>;
  games: string[];
  productTypes: string[];
  suppliesTypes: SuppliesTypeFacetDto[];
  manualCategories: string[];
  printTypes: string[];
  rarities: string[];
  setNames: string[];
}

export interface SupplyProductInfoDto {
  searchMapId: number;
  publicId: string;
  suppliesType: string;
  suppliesTypeNameEn?: string;
  suppliesTypeNameKo?: string;
  table?: string;
  tableId: number;
  maker?: string;
  productIp?: string;
  description?: string;
}

export interface ProductItemDto {
  //----------------공통 정보--------------------------------
  productType: "Cards" | "SealedProducts" | "Supplies" | "ManualProducts";
  productNameEn: string;
  productNameKo: string;
  imageUrlEn: string;
  imageUrlKo?: string;
  languageImageUrlMap?: Record<string, string | null | undefined>;
  isDoubleSided: boolean;
  backImageUrlEn?: string;
  backImageUrlKo?: string;
  languageBackImageUrlMap?: Record<string, string | null | undefined>;
  price: number;

  //----------------세트 정보(카드/밀봉 공용)--------------------------------
  setName: string;
  setCode: string;
  //----------------카드 상품일 때--------------------------------
  card?: CardProductInfoDto;
  //----------------밀봉 상품일 때--------------------------------
  sealedProductInfoDto?: SealedProductInfoDto;

  //----------------supplies 상품일 때--------------------------------
  supplyProductInfoDto?: SupplyProductInfoDto;
  currentVisibleStock: number;

  //----------------manual 상품일 때--------------------------------
  manualProductInfoDto?: ManualProductInfoDto;

  // manual/sealed 등 공통 확장 필드
  tableId?: number;
  productIp?: string;
  table?: string;

  //----------------적립금--------------------------------
  rewardPercentage?: number | null;
  saveAmount?: number | null;
  matchedRuleId?: number | null;
}

export interface CardProductLanguageDto {
  code: string;
  displayName: string;
  displayNameKo: string;
}
export interface ManualProductInfoDto {
  productType: string;
  searchMapId: number;
  publicId: string;
  description: string;
}

export interface ProductCategoryDto {
  id: number;
  nameEn: string;
  nameKo: string;
}

export interface ProductIpDto {
  id: number;
  nameEn: string;
  nameKo: string;
}

export interface ManualProductDto {
  id: number;
  nameEn: string;
  nameKo: string;
  description?: string;
  price: number;
  stock: number;
  /** ProductCategory 마스터 nameEn */
  productType: string;
  categoryNameEn?: string;
  categoryNameKo?: string;
  /** ProductIp 마스터 nameEn */
  productIp: string;
  productIpNameEn?: string;
  productIpNameKo?: string;
  imgUrl?: string;
  isDeleted?: boolean;
  isVisible?: boolean;
  publicId?: string;
  offlineProductId?: string | null;
}

export type ManualProductPatchRequest = Partial<
  Pick<ManualProductDto, "price" | "stock" | "isVisible">
>;

export interface SealedProductAdminDto {
  id: number;
  productNameEn: string;
  productNameKo: string;
  game: string;
  setName?: string;
  setCode?: string;
  price: number;
  currentVisibleStock: number;
  totalStock: number;
  maxVisibleStock: number;
  imageUrl?: string;
  isActive?: boolean;
  isDeleted?: boolean;
  publicId?: string;
  language: string;
  offlineProductId?: string | null;
}

export interface AddManualProductDto {
  nameEn: string;
  nameKo: string;
  description: string;
  price: number;
  stock: number;
  productType: string;
  productIp: string;
  imageFile: File;
  isVisible: boolean;
  offlineProductId?: string;
}

export interface OfflineProductDto {
  id: number;
  productId: string;
  categoryId: string;
  categoryTitle: string;
  title: string;
  priceUnit: number;
  priceValue: number;
  barcode: string;
  createdAt: string;
  updatedAt: string;
  linkTableName?: string;
  linkId?: number;
  receivingQuantity?: number;
  shippingQuantity?: number;
  stockQuantity?: number;
}

export interface SimpleRegisterOfflineProductDto {
  offlineProductId: number;
  linkTableName: string;
}

export interface OfflineProductReceivingItemRequest {
  productId: number;
  receivingQuantity: number;
}

export interface OfflineProductReceivingRequest {
  items: OfflineProductReceivingItemRequest[];
}

export interface OfflineProductReceivingItemDto {
  id: number;
  productId: number;
  tossProductId?: string | null;
  title?: string | null;
  receivingQuantity: number;
}

export interface OfflineProductReceivingHistoryDto {
  id: number;
  createdAt: string;
  receivingManager: string;
  itemCount: number;
  items: OfflineProductReceivingItemDto[];
}

export interface PackagingUnitDto {
  id: number;
  pieceId: number;
  pieceTitle?: string | null;
  pieceProductId?: string | null;
  packagingId: number;
  packagingTitle?: string | null;
  packagingProductId?: string | null;
  unitCount: number;
}

export interface PackagingUnitRequest {
  pieceId: number;
  packagingId: number;
  unitCount: number;
}
