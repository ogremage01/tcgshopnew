"use client";
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
import CardStockTable from "../../_components/CardStockTable";
export default function AdminAnalyzeStockPage() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>제품 재고 현황</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 gap-4">
        <CardStockTable />
        <Card>
          <CardHeader>
            <CardTitle>밀봉 제품 재고 현황</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 gap-4">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>게임</TableHead>
                  <TableHead>제품 이름</TableHead>
                  <TableHead className="text-right">재고 수량</TableHead>
                  <TableHead className="text-right">재고 금액</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow>
                  <TableCell>게임</TableCell>
                  <TableCell>제품 이름</TableCell>
                  <TableCell className="text-right">재고 수량</TableCell>
                  <TableCell className="text-right">재고 금액</TableCell>
                </TableRow>
              </TableBody>
              <TableFooter>
                <TableRow>
                  <TableCell>게임명</TableCell>
                  <TableCell>총계</TableCell>
                  <TableCell className="text-right">총계</TableCell>
                  <TableCell className="text-right">총계</TableCell>
                </TableRow>
              </TableFooter>
            </Table>
          </CardContent>
        </Card>
      </CardContent>
    </Card>
  );
}
