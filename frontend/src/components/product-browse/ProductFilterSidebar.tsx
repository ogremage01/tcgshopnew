"use client";

import { useMemo, useState } from "react";
import { usePathname as useAppPathname } from "@/i18n/navigation";
import { Checkbox } from "@/components/ui/checkbox";
import { Separator } from "@/components/ui/separator";
import type { SearchInitResponseDto } from "@/types/product";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { FilterIcon } from "lucide-react";
import { useTranslations, useLocale } from "next-intl";
import { capitalizeFirstLetter } from "@/lib/utils";
import { isGameSealedBrowsePath, isGameSetBrowsePath } from "@/lib/product-browse";
import { formatManualCategoryLabel } from "@/lib/manual-category-label";
import {
  getSuppliesTypeDisplayName,
  getSuppliesTypeFilterValue,
} from "@/lib/supplies-type-label";
import { useProductFilterParams } from "./hooks/useProductFilterParams";
import FoilFilterSection from "./filters/FoilFilterSection";

/**
 * 
 * 레어리티 표시 순서. 대소문자가 섞여있을 때를 대비해 소문자도 넣었다.
 * 필요시 추가하면 된다.
 */
const RARITY_PRIORITY = [
  "Gold",
  "Fabled",
  "Marvel",
  "Legendary",
  "Majestic",
  "Mythic",
  "mythic",
  "Super Rare",
  "Rare",
  "rare",
  "Uncommon",
  "uncommon",
  "Common",
  "common",
  "Promo",
  "Token",
] as const;

type Props = {
  filters: SearchInitResponseDto;
  selectedGames: string[];
  selectedProductTypes: string[];
  selectedSuppliesTypes: string[];
  selectedManualCategories: string[];
  selectedRarities: string[];
  selectedSetNames: string[];
  isFoil: boolean | null;
  isInStock: boolean;
  showFoilFilter?: boolean;
};

export default function ProductFilterSidebar({
  filters,
  selectedGames,
  selectedProductTypes,
  selectedSuppliesTypes,
  selectedManualCategories,
  selectedRarities,
  selectedSetNames,
  isFoil,
  isInStock,
  showFoilFilter = false,
}: Props) {
  const pathname = useAppPathname();
  const hideGameSetMeta = isGameSetBrowsePath(pathname);
  const hideGameFilter = hideGameSetMeta || isGameSealedBrowsePath(pathname);
  const {
    toggleArrayValue,
    setBooleanParam,
    setNullableBooleanParam,
    resetFilters,
  } = useProductFilterParams();
  const [isOpen, setIsOpen] = useState(false);
  const [fullSetListOpen, setFullSetListOpen] = useState(false);
  const t = useTranslations("game.searchFilterSidebar");
  const tCommon = useTranslations("common");
  const locale = useLocale();

  const rarityOptions = useMemo(
    () =>
      filters.rarities.filter(
        (r) => typeof r === "string" && r.trim().length > 0,
      ),
    [filters.rarities],
  );
  const sortedRarityOptions = useMemo(() => {
    const rarityPrioritySet = new Set<string>(RARITY_PRIORITY);
    const prioritizedRarities = RARITY_PRIORITY.filter((rarity) =>
      rarityOptions.includes(rarity),
    );
    const remainingRarities = rarityOptions.filter(
      (rarity) => !rarityPrioritySet.has(rarity),
    );

    return [...prioritizedRarities, ...remainingRarities];
  }, [rarityOptions]);

  return (
    <div className={`flex flex-col w-full lg:w-1/6 m-2`}>
      <Button
        variant="outline"
        className="w-full lg:hidden justify-start"
        size="sm"
        onClick={() => setIsOpen(!isOpen)}
      >
        <FilterIcon className="w-4 h-4" />{" "}
        {isOpen ? t("collapse") : t("expand")}
      </Button>
      <div
        className={`lg:block ${isOpen ? "flex" : "hidden"} flex-col gap-2 p-4`}
      >
        <div className="flex flex-row gap-2 items-center justify-between">
          <span className="text-lg font-bold">{t("filter")}</span>
          <div className="flex flex-row gap-2 items-center">
            <Button variant="outline" size="sm" onClick={resetFilters}>
              {t("reset")}
            </Button>
          </div>
        </div>
        <Separator className="my-2" />
        {showFoilFilter && (
          <>
            <FoilFilterSection
              isFoil={isFoil}
              setNullableBooleanParam={setNullableBooleanParam}
            />
            <Separator className="my-2" />
          </>
        )}
        <div className="flex flex-row gap-2 items-center">
          <Checkbox
            checked={isInStock}
            onCheckedChange={(checked) =>
              setBooleanParam("isInStock", checked === true)
            }
          />
          <Label className="text-sm">{t("inStockOnly")}</Label>
        </div>
        <Separator className="my-2" />
        {!hideGameFilter && filters.games.length > 0 && (
          <>
            <Separator className="my-2" />
            <div className="text-xs text-gray-500 space-y-1 flex flex-col gap-2">
              <span className="font-bold">{tCommon("game")}</span>
              {filters.games.map((game) => (
                <div key={game} className="flex flex-row gap-2 items-center">
                  <Checkbox
                    checked={selectedGames.includes(game)}
                    onCheckedChange={(checked) =>
                      toggleArrayValue(
                        "games",
                        selectedGames,
                        game,
                        checked === true,
                      )
                    }
                  />
                  <Label>{game}</Label>
                </div>
              ))}
            </div>
            <Separator className="my-2" />
          </>
        )}
        {filters.productTypes.length > 1 && (
          <>
            <div className="text-xs text-gray-500 space-y-1 flex flex-col gap-2">
              <span className="font-bold">{t("productType")}</span>
              {filters.productTypes.map((productType) => (
                <div
                  key={productType}
                  className="flex flex-row gap-2 items-center"
                >
                  <Checkbox
                    checked={selectedProductTypes.includes(productType)}
                    onCheckedChange={(checked) =>
                      toggleArrayValue(
                        "productTypes",
                        selectedProductTypes,
                        productType,
                        checked === true,
                      )
                    }
                  />
                  <span>{productType}</span>
                </div>
              ))}
            </div>
            <Separator className="my-2" />
          </>
        )}

        {filters.suppliesTypes.length > 0 && (
          <>
            <div className="text-xs text-gray-500 space-y-1 flex flex-col gap-2">
              <span className="font-bold">{t("suppliesType")}</span>
              {filters.suppliesTypes.map((suppliesType) => {
                const filterValue = getSuppliesTypeFilterValue(suppliesType);
                return (
                <div
                  key={filterValue}
                  className="flex flex-row gap-2 items-center"
                >
                  <Checkbox
                    checked={selectedSuppliesTypes.includes(filterValue)}
                    onCheckedChange={(checked) =>
                      toggleArrayValue(
                        "suppliesTypes",
                        selectedSuppliesTypes,
                        filterValue,
                        checked === true,
                      )
                    }
                  />
                  <Label>
                    {getSuppliesTypeDisplayName(suppliesType, locale)}
                  </Label>
                </div>
              )})}
            </div>
            <Separator className="my-2" />
          </>
        )}

        {(filters.manualCategories ?? []).length > 0 && (
          <>
            <div className="text-xs text-gray-500 space-y-1 flex flex-col gap-2">
              <span className="font-bold">{t("manualCategory")}</span>
              {(filters.manualCategories ?? []).map((manualCategory) => (
                <div
                  key={manualCategory}
                  className="flex flex-row gap-2 items-center"
                >
                  <Checkbox
                    checked={selectedManualCategories.includes(manualCategory)}
                    onCheckedChange={(checked) =>
                      toggleArrayValue(
                        "manualCategories",
                        selectedManualCategories,
                        manualCategory,
                        checked === true,
                      )
                    }
                  />
                  <Label>{formatManualCategoryLabel(manualCategory)}</Label>
                </div>
              ))}
            </div>
            <Separator className="my-2" />
          </>
        )}

        {sortedRarityOptions.length > 0 && (
          <>
            <div className="text-xs text-gray-500 space-y-1 flex flex-col gap-2">
              <span className="font-bold">{t("rarity")}</span>
              {sortedRarityOptions.map((rarity) => (
                <div key={rarity} className="flex flex-row gap-2 items-center">
                  <Checkbox
                    checked={selectedRarities.includes(rarity)}
                    onCheckedChange={(checked) =>
                      toggleArrayValue(
                        "rarities",
                        selectedRarities,
                        rarity,
                        checked === true,
                      )
                    }
                  />
                  <Label>{capitalizeFirstLetter(rarity)}</Label>
                </div>
              ))}
            </div>
            <Separator className="my-2" />
          </>
        )}
        {!hideGameSetMeta && filters.setNames.length > 0 && (
          <>
            <div className="text-xs text-gray-500 space-y-1 flex flex-col gap-2">
              <span className="font-bold">{t("setName")}</span>
              {filters.setNames.map((setName, index) => (
                <div
                  key={setName}
                  className={`flex flex-row gap-2 items-center ${!fullSetListOpen && index > 4 ? "hidden" : ""}`}
                >
                  <Checkbox
                    checked={selectedSetNames.includes(setName)}
                    onCheckedChange={(checked) =>
                      toggleArrayValue(
                        "setNames",
                        selectedSetNames,
                        setName,
                        checked === true,
                      )
                    }
                  />
                  <Label>{setName}</Label>
                </div>
              ))}
              <Button
                variant="outline"
                size="sm"
                className={`${filters.setNames.length > 5 ? "block" : "hidden"}`}
                onClick={() => setFullSetListOpen(!fullSetListOpen)}
              >
                {fullSetListOpen ? t("collapse") : t("expand")}
              </Button>
            </div>
            <Separator className="my-2" />
          </>
        )}
      </div>
    </div>
  );
}
