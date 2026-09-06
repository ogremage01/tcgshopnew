"use client";

import { useEffect, useMemo, useState } from "react";
import ProductImage from "@/components/product/ProductImage";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import type {
  GradePricingPolicyDto,
  StorageDto,
  UnionPrice,
} from "@/types/product";
import { Separator } from "@/components/ui/separator";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import CardProductAddModal from "./CardProductAddModal";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from "@/components/ui/combobox";
import type {
  UnionPriceGameSetFacet,
  UnionPriceSearchFilters,
} from "../_hooks/useUnionPriceSearch";
import { cn } from "@/lib/utils";
import { upperCase } from "lodash-es";

const ALL_FILTER_VALUE = "all";

type SetOption = {
  value: string;
  label: string;
};

type AdminCardSearchResultsProps = {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  searchedCardList: UnionPrice[];
  totalElements: number;
  facets: UnionPriceGameSetFacet[];
  storageList: StorageDto[];
  gradePolicyList: GradePricingPolicyDto[];
  filters: UnionPriceSearchFilters;
  onApplyFilters: (filters: UnionPriceSearchFilters) => void;
};

export default function AdminCardSearchResults({
  currentPage,
  totalPages,
  onPageChange,
  searchedCardList,
  totalElements,
  facets,
  storageList,
  gradePolicyList,
  filters,
  onApplyFilters,
}: AdminCardSearchResultsProps) {
  const [hoverCardId, setHoverCardId] = useState<number | null>(null);
  const [selectedGame, setSelectedGame] = useState(
    filters.game ?? ALL_FILTER_VALUE,
  );
  const [selectedSetCode, setSelectedSetCode] = useState<string | undefined>(
    filters.setCode,
  );

  useEffect(() => {
    setSelectedGame(filters.game ?? ALL_FILTER_VALUE);
    setSelectedSetCode(filters.setCode);
  }, [filters.game, filters.setCode]);

  const selectedGameFacet = useMemo(() => {
    if (selectedGame === ALL_FILTER_VALUE) return null;
    return facets.find((facet) => facet.game === selectedGame) ?? null;
  }, [facets, selectedGame]);

  const setOptions = useMemo<SetOption[]>(() => {
    if (!selectedGameFacet) return [];

    return selectedGameFacet.sets.map((set) => ({
      value: set.setCode,
      label: set.setName ? `${set.setCode} - ${set.setName}` : set.setCode,
    }));
  }, [selectedGameFacet]);

  const selectedSetOption = useMemo(() => {
    return (
      setOptions.find((option) => option.value === selectedSetCode) ?? null
    );
  }, [selectedSetCode, setOptions]);

  const handleGameChange = (game: string) => {
    setSelectedGame(game);
    setSelectedSetCode(undefined);
  };

  const applyFilters = () => {
    onApplyFilters({
      game: selectedGame === ALL_FILTER_VALUE ? undefined : selectedGame,
      setCode: selectedSetCode,
    });
  };

  if (
    searchedCardList.length === 0 &&
    totalElements === 0 &&
    !filters.game &&
    !filters.setCode
  ) {
    return null;
  }

  return (
    <Card>
      <Card className="my-4 mx-4">
        <CardHeader>
          <div className="flex flex-row flex-wrap items-center gap-2">
            <CardTitle>신규 카드 등록</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-4 my-1">
            총 {totalElements}개의 싱글카드가 검색되었습니다.
            <div className="flex flex-row items-center gap-2">
              <Select value={selectedGame} onValueChange={handleGameChange}>
                <SelectTrigger className="w-[240px]">
                  <SelectValue placeholder="게임 선택" />
                </SelectTrigger>
                <SelectContent className="w-[240px]">
                  <SelectGroup>
                    <SelectLabel>게임</SelectLabel>
                    <SelectItem value={ALL_FILTER_VALUE}>전체</SelectItem>
                    {facets.map((facet) => (
                      <SelectItem key={facet.game} value={facet.game}>
                        {facet.game}
                      </SelectItem>
                    ))}
                  </SelectGroup>
                </SelectContent>
              </Select>
              <Combobox
                items={setOptions}
                value={selectedSetOption}
                onValueChange={(option) =>
                  setSelectedSetCode(option?.value ?? undefined)
                }
                disabled={selectedGame === ALL_FILTER_VALUE}
                isItemEqualToValue={(a, b) => a.value === b.value}
                itemToStringLabel={(item) => item.label}
                itemToStringValue={(item) => item.value}
              >
                <ComboboxInput
                  className="w-[280px]"
                  placeholder="판본 검색/선택"
                  disabled={selectedGame === ALL_FILTER_VALUE}
                  showClear
                />
                <ComboboxContent className="bg-white p-0">
                  <ComboboxEmpty>검색 결과가 없습니다.</ComboboxEmpty>
                  <ComboboxList className="max-h-60 overflow-y-auto">
                    {(item) => (
                      <ComboboxItem key={item.value} value={item}>
                        {item.label}
                      </ComboboxItem>
                    )}
                  </ComboboxList>
                </ComboboxContent>
              </Combobox>
              <Button variant="outline" size="sm" onClick={applyFilters}>
                적용
              </Button>
            </div>
          </div>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3 lg:grid-cols-4 mb-2">
            {searchedCardList.length === 0 ? (
              <div className="col-span-full py-8 text-center text-sm text-muted-foreground">
                검색 결과가 없습니다.
              </div>
            ) : (
              searchedCardList.map((card) => (
                <CardProductAddModal
                  key={card.id}
                  onOpenChange={() => {}}
                  selectedCard={card}
                  imageUrl={card.imageUrl ?? ""}
                  storageList={storageList}
                  gradePolicyList={gradePolicyList}
                  trigger={
                    <div
                      className={`gap-2 cursor-pointer rounded-md border p-4 ${
                        hoverCardId === card.id ? "bg-muted" : ""
                      }`}
                      onMouseEnter={() => setHoverCardId(card.id ?? null)}
                      onMouseLeave={() => setHoverCardId(null)}
                    >
                      <div className="flex flex-col gap-2">
                        <div className="flex flex-row gap-2">
                          <ProductImage
                            className="w-1/2 object-contain"
                            src={card.imageUrl}
                            alt={card.cardName ?? ""}
                            width={250}
                            height={250}
                          />
                          <div className="flex flex-col gap-2">
                            <h2 className="text-sm font-bold">{card.game}</h2>
                            <Separator />
                            <h3 className="font-bold">{card.cardName}</h3>
                            {card.cardNameK ? (
                              <h3 className="font-bold">{card.cardNameK}</h3>
                            ) : (
                              ""
                            )}
                            <p className="text-sm">
                              {card.checkCodeRefined ??
                                card.setCode + "-" + card.printing}
                            </p>
                            <p
                              className={cn(
                                "text-sm",
                                upperCase(card.printType).includes("FOIL")
                                  ? "font-bold"
                                  : "text-muted-foreground",
                              )}
                            >
                              {card.printType}
                            </p>
                            <p className="text-sm">
                              가격: {card.price ?? "없음"}
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>
                  }
                />
              ))
            )}
          </div>
          <PaginationRangeAsync
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={onPageChange}
          />
        </CardContent>
      </Card>
    </Card>
  );
}
