"use client";

import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
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
import { Separator } from "@/components/ui/separator";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import { useEffect, useState, useMemo } from "react";
import { SearchBySetDto } from "@/types/product";
import { useCardProductPatch } from "@/app/admin/products/card/_hooks/useCardProductPatch";
import { useCardProductMetadata } from "@/app/admin/products/card/_hooks/useCardProductMetadata";
import AdminCardProductListTable from "@/app/admin/products/card/_components/AdminCardProductListTable";
import { useCheckBySetSearch } from "@/app/admin/products/card/checkbyset/_hooks/useCheckBySetSearch";
import { GAME_ENUM } from "@/config/gameEnum";
import {
  fetchSetListForGame,
  gameEntryByProductId,
  type GameSetInfoDto,
} from "@/lib/game-set-list";

type SetOption = {
  value: string;
  label: string;
};

export default function AdminProductsCardCheckbysetPage() {
  const [game, setGame] = useState<{
    productLineId: number;
    productLineName: string;
  }>({ productLineId: 0, productLineName: "" });
  const [set, setSet] = useState<string | null>(null);
  const [setList, setSetList] = useState<GameSetInfoDto[]>([]);
  const [printTypeFilter, setPrintTypeFilter] =
    useState<SearchBySetDto["printTypeFilter"]>("all");
  const [storage, setStorage] = useState<string>("0");
  const [searchCriteria, setSearchCriteria] = useState<SearchBySetDto | null>(
    null,
  );

  const games = GAME_ENUM.map((g) => ({
    productLineId: g.productId,
    productLineName: g.game,
  }));

  const { storageList } = useCardProductMetadata();
  const {
    data: cardList,
    currentPage,
    totalPages,
    totalElements,
    isLoading,
    handlePageChange,
    removeFromList,
  } = useCheckBySetSearch(searchCriteria);
  const { savePatch, schedulePatch, flushPatch, deleteCard } =
    useCardProductPatch({
      onDeleted: removeFromList,
    });

  useEffect(() => {
    const entry = gameEntryByProductId(game.productLineId);
    if (!entry) {
      setSetList([]);
      return;
    }
    void fetchSetListForGame(entry).then((res) => {
      setSetList(res);
    });
  }, [game]);

  const setOptions = useMemo<SetOption[]>(() => {
    return setList.map((item) => {
      const label = "name" in item ? item.name : item.setName;
      return {
        value: item.setCode,
        label: label ? `${item.setCode} - ${label}` : item.setCode,
      };
    });
  }, [setList]);

  const selectedSetOption = useMemo(() => {
    if (set === null) return null;
    return setOptions.find((option) => option.value === set) ?? null;
  }, [set, setOptions]);

  const handleSearch = () => {
    setSearchCriteria({
      game: game.productLineName,
      set: set ?? "",
      printTypeFilter,
      storageId: storage,
    });
  };

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">세트별 싱글 카드 현황</h2>
      <Card>
        <CardHeader>
          <CardTitle className="flex flex-row gap-2">
            <Select
              value={game.productLineName}
              onValueChange={(value) => {
                setSet(null);
                setSetList([]);
                setGame({
                  productLineId:
                    games.find((item) => item.productLineName === value)
                      ?.productLineId ?? 0,
                  productLineName: value,
                });
              }}
            >
              <SelectTrigger className="w-96">
                <SelectValue placeholder="게임 선택" />
              </SelectTrigger>
              <SelectContent className="bg-white">
                {games.map((item) => (
                  <SelectItem
                    key={item.productLineId}
                    value={item.productLineName}
                  >
                    {item.productLineName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Combobox
              items={setOptions}
              value={selectedSetOption}
              onValueChange={(option) => setSet(option?.value ?? null)}
              disabled={!game.productLineId}
              isItemEqualToValue={(a, b) => a.value === b.value}
              itemToStringLabel={(item) => item.label}
              itemToStringValue={(item) => item.value}
            >
              <ComboboxInput
                className="w-[320px]"
                placeholder="세트 선택"
                disabled={!game.productLineId}
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
            <Select
              value={printTypeFilter}
              onValueChange={(value) =>
                setPrintTypeFilter(value as SearchBySetDto["printTypeFilter"])
              }
            >
              <SelectTrigger className="w-48">
                <SelectValue placeholder="인쇄 형태" />
              </SelectTrigger>
              <SelectContent className="bg-white">
                <SelectItem value="all">전체</SelectItem>
                <SelectItem value="foil">포일만</SelectItem>
                <SelectItem value="normal">노멀만</SelectItem>
              </SelectContent>
            </Select>
            <Select value={storage} onValueChange={setStorage}>
              <SelectTrigger className="w-96">
                <SelectValue placeholder="보관소 선택" />
              </SelectTrigger>
              <SelectContent className="bg-white">
                <SelectItem value="0">전체</SelectItem>
                {storageList.map((item) => (
                  <SelectItem key={item.id} value={item.id.toString()}>
                    {item.storageName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button onClick={handleSearch}>검색</Button>
          </CardTitle>
          <Separator className="my-4" />
          <CardContent className="space-y-4">
            {searchCriteria && totalElements > 0 && (
              <p className="text-sm text-muted-foreground">
                총 {totalElements.toLocaleString()}건
              </p>
            )}
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
              onPageChange={handlePageChange}
            />
          </CardContent>
        </CardHeader>
      </Card>
    </div>
  );
}
