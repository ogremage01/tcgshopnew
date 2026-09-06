import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
  TableHead,
  TableFooter,
} from "@/components/ui/table";
import { AdminStockSummaryDto } from "@/types/order";
import { useState, useEffect } from "react";
import { api } from "@/lib/api";

export default function CardStockTable() {
  const [stockSummary, setStockSummary] = useState<AdminStockSummaryDto[]>([]);
  useEffect(() => {
    const fetchStockSummary = async () => {
      const response = await api.get<AdminStockSummaryDto[]>(
        "/api/admin/analyze/stock-summary",
      );
      setStockSummary(response);
    };
    fetchStockSummary();
  }, []);
  return (
    <Card>
      <CardHeader>
        <CardTitle>싱글 카드 재고 현황</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>게임</TableHead>
              <TableHead className="text-right">총 재고</TableHead>
              <TableHead className="text-right">총 금액</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {stockSummary.map((summary) => (
              <TableRow key={summary.gameName}>
                <TableCell>{summary.gameName}</TableCell>
                <TableCell className="text-right">
                  {summary.totalStockCount.toLocaleString()}
                </TableCell>
                <TableCell className="text-right">
                  ₩{summary.totalStockAmount.toLocaleString()}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
          <TableFooter>
            <TableRow>
              <TableCell>총계</TableCell>
              <TableCell className="text-right">
                {stockSummary
                  .reduce((acc, summary) => acc + summary.totalStockCount, 0)
                  .toLocaleString()}
              </TableCell>
              <TableCell className="text-right">
                ₩
                {stockSummary
                  .reduce((acc, summary) => acc + summary.totalStockAmount, 0)
                  .toLocaleString()}
              </TableCell>
            </TableRow>
          </TableFooter>
        </Table>
      </CardContent>
    </Card>
  );
}
