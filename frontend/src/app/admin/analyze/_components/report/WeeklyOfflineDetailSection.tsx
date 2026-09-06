"use client";

import { useState } from "react";
import { Separator } from "@/components/ui/separator";
import {
  AdminManualProductSaleDto,
  AdminSealedProductSalesSummaryDto,
  AdminSupplyProductSaleDto,
  AdminWeeklyReportDto,
} from "@/types/order";
import { formatCurrency } from "@/app/admin/offline/sales/_lib/payment-source-type";
import {
  amountCell,
  amountHead,
  countCell,
  countHead,
  detailLabelCell,
  detailLabelHead,
  formatCount,
  pctCell,
  pctHead,
} from "./report-table-styles";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";

type ProductType = "sealed" | "supply" | "manual";

const ALL_VALUE = "__all__";

interface Props {
  report: AdminWeeklyReportDto;
}

interface QueryState {
  productType: ProductType;
  detailValue: string;
}

function SalesRowsEmpty() {
  return (
    <tr>
      <td colSpan={5} className="text-center">
        매출 데이터가 없습니다.
      </td>
    </tr>
  );
}

function SealedGameTable({
  gameSummary,
}: {
  gameSummary: AdminSealedProductSalesSummaryDto;
}) {
  return (
    <div className="flex flex-col gap-2">
      <h3 className="text-left text-lg font-bold">{gameSummary.game}</h3>
      <table className="w-full border-collapse border border-black table-fixed">
        <thead>
          <tr>
            <th className={detailLabelHead}>세트</th>
            <th className={countHead}>종수</th>
            <th className={countHead}>수량</th>
            <th className={amountHead}>매출액</th>
            <th className={pctHead}>매출 비중</th>
          </tr>
        </thead>
        <tbody>
          {gameSummary.sealedProductSaleDtoList.length > 0 ? (
            gameSummary.sealedProductSaleDtoList.map((item) => (
              <tr key={item.setName}>
                <td className={detailLabelCell}>{item.setName}</td>
                <td className={countCell}>
                  {formatCount(item.totalSalesCount)}
                </td>
                <td className={countCell}>
                  {formatCount(item.totalSalesQuantity)}
                </td>
                <td className={amountCell}>
                  {formatCurrency(item.totalSalesAmount)}
                </td>
                <td className={pctCell}>
                  {item.totalSalesPercentage != null
                    ? `${item.totalSalesPercentage.toFixed(1)}%`
                    : "-"}
                </td>
              </tr>
            ))
          ) : (
            <SalesRowsEmpty />
          )}
        </tbody>
      </table>
    </div>
  );
}

function CategorySalesTable({
  labelHeadText,
  rows,
}: {
  labelHeadText: string;
  rows: {
    key: string;
    label: string;
    totalSalesCount: number | null;
    totalSalesQuantity: number | null;
    totalSalesAmount: number | null;
    totalSalesPercentage: number | null;
  }[];
}) {
  return (
    <table className="w-full border-collapse border border-black table-fixed">
      <thead>
        <tr>
          <th className={detailLabelHead}>{labelHeadText}</th>
          <th className={countHead}>종수</th>
          <th className={countHead}>수량</th>
          <th className={amountHead}>매출액</th>
          <th className={pctHead}>매출 비중</th>
        </tr>
      </thead>
      <tbody>
        {rows.length > 0 ? (
          rows.map((item) => (
            <tr key={item.key}>
              <td className={detailLabelCell}>{item.label}</td>
              <td className={countCell}>
                {formatCount(item.totalSalesCount)}
              </td>
              <td className={countCell}>
                {formatCount(item.totalSalesQuantity)}
              </td>
              <td className={amountCell}>
                {formatCurrency(item.totalSalesAmount)}
              </td>
              <td className={pctCell}>
                {item.totalSalesPercentage != null
                  ? `${item.totalSalesPercentage.toFixed(1)}%`
                  : "-"}
              </td>
            </tr>
          ))
        ) : (
          <SalesRowsEmpty />
        )}
      </tbody>
    </table>
  );
}

function toSupplyRows(list: AdminSupplyProductSaleDto[]) {
  return list.map((item) => ({
    key: item.supplyType,
    label: item.supplyType,
    totalSalesCount: item.totalSalesCount,
    totalSalesQuantity: item.totalSalesQuantity,
    totalSalesAmount: item.totalSalesAmount,
    totalSalesPercentage: item.totalSalesPercentage,
  }));
}

function toManualRows(list: AdminManualProductSaleDto[]) {
  return list.map((item) => ({
    key: item.manualType,
    label: item.manualType,
    totalSalesCount: item.totalSalesCount,
    totalSalesQuantity: item.totalSalesQuantity,
    totalSalesAmount: item.totalSalesAmount,
    totalSalesPercentage: item.totalSalesPercentage,
  }));
}

export function WeeklyOfflineDetailSection({ report }: Props) {
  const offline = report.offlineSalesSummaryDto;

  const [productType, setProductType] = useState<ProductType | "">("");
  const [detailValue, setDetailValue] = useState("");
  const [query, setQuery] = useState<QueryState | null>(null);

  const sealedList = offline?.sealedProductSalesSummaryList ?? [];
  const supplyList = offline?.supplyProductSaleDtoList ?? [];
  const manualList = offline?.manualProductSaleDtoList ?? [];

  const detailOptions: { value: string; label: string }[] = (() => {
    if (productType === "sealed") {
      return [
        { value: ALL_VALUE, label: "전체" },
        ...sealedList.map((item) => ({ value: item.game, label: item.game })),
      ];
    }
    if (productType === "supply") {
      return [
        { value: ALL_VALUE, label: "전체" },
        ...supplyList.map((item) => ({
          value: item.supplyType,
          label: item.supplyType,
        })),
      ];
    }
    if (productType === "manual") {
      return [
        { value: ALL_VALUE, label: "전체" },
        ...manualList.map((item) => ({
          value: item.manualType,
          label: item.manualType,
        })),
      ];
    }
    return [];
  })();

  const canQuery = productType !== "" && detailValue !== "";

  const handleProductTypeChange = (value: ProductType) => {
    setProductType(value);
    setDetailValue(ALL_VALUE);
    setQuery(null);
  };

  const handleQuery = () => {
    if (productType === "" || detailValue === "") return;
    setQuery({ productType, detailValue });
  };

  const renderResult = () => {
    if (query == null) {
      return (
        <p className="text-center text-muted-foreground">
          상품 유형과 항목을 선택한 뒤 조회하세요.
        </p>
      );
    }

    if (query.productType === "sealed") {
      const summaries =
        query.detailValue === ALL_VALUE
          ? sealedList
          : sealedList.filter((item) => item.game === query.detailValue);

      if (summaries.length === 0) {
        return (
          <div className="flex flex-col gap-2">
            <h3 className="text-center">매출 데이터가 없습니다.</h3>
          </div>
        );
      }

      return (
        <div className="flex flex-col gap-4">
          <h2 className="text-center text-lg font-bold">밀봉 제품</h2>
          {summaries.map((gameSummary) => (
            <SealedGameTable key={gameSummary.game} gameSummary={gameSummary} />
          ))}
        </div>
      );
    }

    if (query.productType === "supply") {
      const rows =
        query.detailValue === ALL_VALUE
          ? toSupplyRows(supplyList)
          : toSupplyRows(
              supplyList.filter((item) => item.supplyType === query.detailValue),
            );

      return (
        <div className="flex flex-col gap-4">
          <h2 className="text-center text-lg font-bold">서플라이</h2>
          <CategorySalesTable labelHeadText="카테고리" rows={rows} />
        </div>
      );
    }

    const rows =
      query.detailValue === ALL_VALUE
        ? toManualRows(manualList)
        : toManualRows(
            manualList.filter((item) => item.manualType === query.detailValue),
          );

    return (
      <div className="flex flex-col gap-4">
        <h2 className="text-center text-lg font-bold">수동 상품</h2>
        <CategorySalesTable labelHeadText="카테고리" rows={rows} />
      </div>
    );
  };

  return (
    <>
      <Separator />
      <h1 className="text-center text-lg font-bold">오프라인 매출 세부</h1>
      <Separator />

      <div className="flex flex-row flex-wrap items-center justify-center gap-2">
        <Select
          value={productType || undefined}
          onValueChange={(value) =>
            handleProductTypeChange(value as ProductType)
          }
        >
          <SelectTrigger className="w-[160px]">
            <SelectValue placeholder="상품 유형" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="sealed">밀봉 제품</SelectItem>
            <SelectItem value="supply">서플라이</SelectItem>
            <SelectItem value="manual">수동 상품</SelectItem>
          </SelectContent>
        </Select>

        <Select
          value={detailValue || undefined}
          onValueChange={setDetailValue}
          disabled={productType === ""}
        >
          <SelectTrigger className="w-[200px]">
            <SelectValue placeholder="항목 선택" />
          </SelectTrigger>
          <SelectContent>
            {detailOptions.length > 0 ? (
              detailOptions.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))
            ) : (
              <SelectItem value="__empty" disabled>
                목록 없음
              </SelectItem>
            )}
          </SelectContent>
        </Select>

        <Button type="button" onClick={handleQuery} disabled={!canQuery}>
          조회
        </Button>
      </div>

      <div className="mt-4">{renderResult()}</div>
    </>
  );
}
