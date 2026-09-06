import type { CardProductSaleDto, ProductItemDto } from "@/types/product";
import CardProductRow from "./CardProductRow";
import { capitalizeFirstLetter } from "@/lib/utils";
import PrintTypeLabel from "@/components/product/PrintTypeLabel";
import { isGameSetBrowsePath } from "@/lib/product-browse";
import { usePathname } from "@/i18n/navigation";
import { useLocale } from "next-intl";
import { Separator } from "@/components/ui/separator";
import { useMemo, useState } from "react";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import Link from "next/link";
import GridProductImage from "./GridProductImage";
import {
  compareLanguageCode,
  languageDisplayName,
  languageImageCandidates,
  languageShortLabel,
  sortSaleMapEntries,
} from "@/lib/card-product-language";
import { useCardProductLanguages } from "@/hooks/use-card-product-languages";

type CardSaleWithLanguage = CardProductSaleDto & { languageCode: string };

const conditionPriority: Record<string, number> = {
  NM: 0,
  EX: 1,
  VG: 2,
  G: 3,
};

export default function CardProduct({
  productItemDto,
  handleAddToCart,
}: {
  productItemDto: ProductItemDto;
  handleAddToCart: (searchMapId: number, quantity: number) => void;
}) {
  const pathname = usePathname();
  const locale = useLocale();
  const { languages } = useCardProductLanguages();
  const hideGameSetMeta = isGameSetBrowsePath(pathname);
  const [selectedLanguage, setSelectedLanguage] = useState<string | null>(null);
  const [selectedSaleId, setSelectedSaleId] = useState<number | null>(null);

  const languageGroups = useMemo(
    () => sortSaleMapEntries(productItemDto.card?.cardProductSaleDtoMap),
    [productItemDto.card?.cardProductSaleDtoMap],
  );
  const salesByLanguage = useMemo(
    () =>
      Object.fromEntries(
        languageGroups.map((group) => [group.code, group.sales]),
      ),
    [languageGroups],
  );

  const defaultSale = useMemo<CardSaleWithLanguage | null>(
    () =>
      languageGroups
        .flatMap(({ code, sales }) =>
          sales.map((sale) => ({ ...sale, languageCode: code })),
        )
        .sort((a, b) => {
          const aHasStock = a.currentVisibleStock > 0 ? 0 : 1;
          const bHasStock = b.currentVisibleStock > 0 ? 0 : 1;
          if (aHasStock !== bHasStock) return aHasStock - bHasStock;

          const aConditionPriority =
            conditionPriority[a.condition] ?? Number.MAX_SAFE_INTEGER;
          const bConditionPriority =
            conditionPriority[b.condition] ?? Number.MAX_SAFE_INTEGER;
          if (aConditionPriority !== bConditionPriority)
            return aConditionPriority - bConditionPriority;

          return compareLanguageCode(a.languageCode, b.languageCode);
        })[0] ?? null,
    [languageGroups],
  );

  const activeLanguage = selectedLanguage ?? defaultSale?.languageCode ?? null;
  const salesForSelectedLanguage = useMemo(
    () => (activeLanguage ? (salesByLanguage[activeLanguage] ?? []) : []),
    [activeLanguage, salesByLanguage],
  );
  const selectedItem = useMemo(() => {
    if (selectedSaleId != null) {
      const selected = salesForSelectedLanguage.find(
        (sale) => sale.id === selectedSaleId,
      );
      if (selected) return selected;
    }
    if (activeLanguage === defaultSale?.languageCode) return defaultSale;
    return salesForSelectedLanguage[0] ?? null;
  }, [activeLanguage, defaultSale, salesForSelectedLanguage, selectedSaleId]);
  const imageCandidates = languageImageCandidates(
    productItemDto.languageImageUrlMap,
    activeLanguage,
    productItemDto.imageUrlEn,
  );

  const handleSelectLanguage = (language: string) => {
    if (!language) return;
    const sales = salesByLanguage[language];
    if (!sales || sales.length === 0) return;
    setSelectedLanguage(language);
    setSelectedSaleId(sales[0]?.id ?? null);
  };

  const handleSelectSale = (saleId: string) => {
    if (!saleId || !activeLanguage) return;
    const itemId = parseInt(saleId, 10);
    if (isNaN(itemId)) return;
    const sale = salesForSelectedLanguage.find((s) => s.id === itemId);
    if (!sale) return;
    setSelectedSaleId(itemId);
  };

  const isFoil = (productItemDto.card?.printType ?? "")
    .toLowerCase()
    .includes("foil");

  return (
    <div className="flex h-full flex-col">
      <div className="flex h-full flex-col gap-1">
        {!hideGameSetMeta && (
          <>
            <span className="text-gray-500 text-sm text-ellipsis overflow-hidden whitespace-nowrap">
              {productItemDto.card?.game}
            </span>
            <span className="text-gray-500 text-sm text-ellipsis overflow-hidden whitespace-nowrap">
              {productItemDto.setName}
            </span>
          </>
        )}
        <div className="image-container relative w-full overflow-hidden rounded-lg bg-gray-100 aspect-[63/88]">
          <Link href={`/products/${productItemDto.card?.publicId}`}>
            <GridProductImage
              src={imageCandidates[0]}
              fallbackSrcs={imageCandidates.slice(1)}
              alt={productItemDto.productNameEn}
            />
          </Link>
          <div className="absolute bottom-2 right-2">
            <PrintTypeLabel
              printing={
                productItemDto.card?.printing ?? productItemDto.card?.printType
              }
              foilHighlight="badge"
              className="text-sm"
            />
          </div>
        </div>
        <div className="flex flex-col flex-1 min-w-0 gap-2">
          <div className="flex flex-row gap-2 w-full justify-between items-center">
            <span className="text-gray-500 text-sm">
              {capitalizeFirstLetter(productItemDto.card?.rarity)}
            </span>
            <span className="text-gray-500 text-sm text-ellipsis overflow-hidden whitespace-nowrap">
              {activeLanguage
                ? languageDisplayName(activeLanguage, locale, languages)
                : null}
            </span>
          </div>
          <div className="flex flex-col flex-1 w-full">
            <div className="flex min-w-0 flex-row flex-nowrap items-center gap-2 overflow-x-auto">
              <ToggleGroup
                type="single"
                variant="outline"
                value={activeLanguage ?? ""}
                onValueChange={handleSelectLanguage}
                className="shrink-0 justify-start"
                size="sm"
              >
                {languageGroups.map(({ code }) => (
                  <ToggleGroupItem
                    key={code}
                    value={code}
                    className={`min-w-7 shrink-0 px-1 py-0 text-xs leading-4`}
                  >
                    {languageShortLabel(code)}
                  </ToggleGroupItem>
                ))}
              </ToggleGroup>
              <Separator orientation="vertical" className="h-4 shrink-0" />
              <ToggleGroup
                type="single"
                variant="outline"
                value={
                  selectedItem?.id != null
                    ? String(selectedItem.id)
                    : "unavailable"
                }
                onValueChange={handleSelectSale}
                className="shrink-0 justify-start"
                size="sm"
              >
                {salesForSelectedLanguage.map((sale, idx) => (
                  <ToggleGroupItem
                    key={sale.id ?? `unavailable-${idx}`}
                    value={sale.id != null ? String(sale.id) : "unavailable"}
                    disabled={sale.id == null}
                    className="min-w-7 shrink-0 px-1 py-0 text-xs leading-4"
                  >
                    {sale.condition}
                  </ToggleGroupItem>
                ))}
              </ToggleGroup>
            </div>
            <Separator className="my-1" />
            {selectedItem && (
              <CardProductRow
                sale={selectedItem}
                handleAddToCart={handleAddToCart}
                rewardPercentage={productItemDto.rewardPercentage}
              />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
