export interface CartItemDto {
    id: number;
    cartItemUpdatedAt?: string | null;
    searchMapId: number;
    price: number;
    priceUsd?: number | null;
    currentVisibleStock: number;
    quantity: number;
    imageUrl: string;
    productNameEn: string;
    productNameKo: string;
    cardProduct?: CardProductDto | null;
    suppliesProduct?: SuppliesProductDto | null;
    sealedProduct?: SealedProductDto | null;
}
export interface CardProductDto {
    game: string;
    condition: string;
    language: string;
    printType: string;
    printing?: string | null;
    setCode: string;
    setNumber: string;
    setName: string;
}

export interface SuppliesProductDto {
    suppliesType: string;
    table: string;
    tableId: number;
    maker: string;
    productIp: string;
}

export interface SealedProductDto {
    game: string;
    language: string;
}
export interface CartUpdateRequest {
    quantity: number;
}
export interface CartDto {
    id: string;
    cartItems: CartItemDto[];
    createdAt: string;
    updatedAt: string;
    userId: number;
    guestId: string;
}