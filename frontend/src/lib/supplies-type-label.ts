export type SuppliesTypeLabel = {
  nameEn: string;
  nameKo: string;
};

export function getSuppliesTypeDisplayName(
  suppliesType: SuppliesTypeLabel,
  locale: string,
): string {
  return locale.startsWith("ko") ? suppliesType.nameKo : suppliesType.nameEn;
}

export function getSuppliesTypeFilterValue(suppliesType: SuppliesTypeLabel): string {
  return suppliesType.nameKo;
}

export function getSupplyProductTypeDisplayName(
  supply: {
    suppliesType: string;
    suppliesTypeNameEn?: string;
    suppliesTypeNameKo?: string;
  },
  locale: string,
): string {
  return getSuppliesTypeDisplayName(
    {
      nameEn: supply.suppliesTypeNameEn ?? supply.suppliesType,
      nameKo: supply.suppliesTypeNameKo ?? supply.suppliesType,
    },
    locale,
  );
}
