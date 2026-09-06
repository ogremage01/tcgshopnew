"use client";

import { useEffect, useMemo, useState } from "react";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import AdminCardProductListTable from "./AdminCardProductListTable";
import type { CardProductManagementResponseDto } from "@/types/product";
import { useCardProductMetadata } from "../_hooks/useCardProductMetadata";
import { useCardProductPatch } from "../_hooks/useCardProductPatch";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectGroup,
  SelectLabel,
  SelectItem,
} from "@/components/ui/select";
import {
  Combobox,
  ComboboxInput,
  ComboboxContent,
  ComboboxList,
  ComboboxItem,
  ComboboxEmpty,
} from "@/components/ui/combobox";
import { Button } from "@/components/ui/button";
import type {
  UnionPriceGameSetFacet,
  UnionPriceSearchFilters,
} from "../_hooks/useUnionPriceSearch";

const ALL_FILTER_VALUE = "all";

type SetOption = {
  value: string;
  label: string;
};

type AdminCardProductTableProps = {
  cardList: CardProductManagementResponseDto[];
  facets: UnionPriceGameSetFacet[];
  filters: UnionPriceSearchFilters;
  isLoading?: boolean;
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  onApplyFilters: (filters: UnionPriceSearchFilters) => void;
  onProductDeleted?: (id: number) => void;
};

export default function AdminCardProductTable({
  cardList,
  facets,
  filters,
  isLoading = false,
  currentPage,
  totalPages,
  onPageChange,
  onApplyFilters,
  onProductDeleted,
}: AdminCardProductTableProps) {
  const { storageList } = useCardProductMetadata();
  const { savePatch, schedulePatch, flushPatch, deleteCard } =
    useCardProductPatch({
      onDeleted: onProductDeleted,
    });

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

  return (
    <Card>
      <CardHeader className="flex flex-row flex-wrap items-center gap-2">
        <div className="flex flex-col gap-3">
          <CardTitle>등록 카드 목록</CardTitle>
          <div className="flex flex-row flex-wrap items-center gap-2">
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
      </CardHeader>
      <CardContent className="space-y-4">
        <AdminCardProductListTable
          cardList={cardList}
          storageList={storageList}
          isLoading={isLoading}
          schedulePatch={schedulePatch}
          flushPatch={flushPatch}
          savePatch={savePatch}
          onDelete={deleteCard}
        />
        <PaginationRangeAsync
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={onPageChange}
        />
      </CardContent>
    </Card>
  );
}
