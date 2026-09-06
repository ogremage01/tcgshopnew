"use client";

import { Fragment, useEffect, useMemo, useState } from "react";
import { ChevronDown, ChevronRight } from "lucide-react";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
  TableHead,
} from "@/components/ui/table";
import { OfflineSalesSummaryItemDto } from "@/types/sales";

function groupItemsByCategory(items: OfflineSalesSummaryItemDto[]) {
  const grouped = new Map<string, OfflineSalesSummaryItemDto[]>();

  for (const item of items) {
    const category = item.category ?? "미분류";
    const categoryItems = grouped.get(category) ?? [];
    categoryItems.push(item);
    grouped.set(category, categoryItems);
  }

  return Array.from(grouped.entries()).sort(([left], [right]) =>
    left.localeCompare(right, "ko"),
  );
}

interface SalesSummaryItemsTableProps {
  items: OfflineSalesSummaryItemDto[];
}

export default function SalesSummaryItemsTable({
  items,
}: SalesSummaryItemsTableProps) {
  const groupedItems = useMemo(() => groupItemsByCategory(items), [items]);
  const [expandedCategories, setExpandedCategories] = useState<
    Record<string, boolean>
  >({});

  useEffect(() => {
    setExpandedCategories(
      Object.fromEntries(groupedItems.map(([category]) => [category, false])),
    );
  }, [groupedItems]);

  const toggleCategory = (category: string) => {
    setExpandedCategories((prev) => ({
      ...prev,
      [category]: !prev[category],
    }));
  };

  if (groupedItems.length === 0) {
    return (
      <div className="flex h-16 items-center justify-center text-muted-foreground">
        집계된 상품 데이터가 없습니다.
      </div>
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>카테고리 / 상품명</TableHead>
          <TableHead className="text-right">수량</TableHead>
          <TableHead className="text-right">금액</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {groupedItems.map(([category, categoryItems]) => {
          const isExpanded = expandedCategories[category] ?? false;
          const categoryQuantity = categoryItems.reduce(
            (sum, item) => sum + item.totalQuantity,
            0,
          );
          const categoryAmount = categoryItems.reduce(
            (sum, item) => sum + item.totalPriceValue,
            0,
          );

          return (
            <Fragment key={category}>
              <TableRow
                className="cursor-pointer bg-muted/40 hover:bg-muted/60"
                onClick={() => toggleCategory(category)}
              >
                <TableCell>
                  <div className="flex items-center gap-2 font-medium">
                    {isExpanded ? (
                      <ChevronDown className="h-4 w-4 shrink-0" />
                    ) : (
                      <ChevronRight className="h-4 w-4 shrink-0" />
                    )}
                    <span>{category}</span>
                    <span className="text-xs text-muted-foreground">
                      ({categoryItems.length}종)
                    </span>
                  </div>
                </TableCell>
                <TableCell className="text-right font-medium">
                  {categoryQuantity.toLocaleString()}
                </TableCell>
                <TableCell className="text-right font-medium">
                  ₩{categoryAmount.toLocaleString()}
                </TableCell>
              </TableRow>
              {isExpanded &&
                categoryItems.map((item, itemIndex) => (
                  <TableRow
                    key={`${category}-${item.title}-${itemIndex}`}
                    className="hover:bg-muted/30"
                  >
                    <TableCell className="pl-10 text-muted-foreground">
                      {item.title}
                    </TableCell>
                    <TableCell className="text-right">
                      {item.totalQuantity.toLocaleString()}
                    </TableCell>
                    <TableCell className="text-right">
                      ₩{item.totalPriceValue.toLocaleString()}
                    </TableCell>
                  </TableRow>
                ))}
            </Fragment>
          );
        })}
      </TableBody>
    </Table>
  );
}
