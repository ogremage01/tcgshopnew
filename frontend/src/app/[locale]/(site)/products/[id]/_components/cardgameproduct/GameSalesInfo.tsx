import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { GameSalesInfoDto } from "@/types/product";
import { useTranslations } from "next-intl";

export default function GameSalesInfo({ gameSalesInfo }: { gameSalesInfo?: GameSalesInfoDto }) {
    const t = useTranslations("productSalesInfo");
    return (
        <Table className="border-t-2 mt-4">
            <TableHeader>
                <TableRow>
                    <TableHead colSpan={4} className="text-left text-lg">
                        {t("productInfo")}
                    </TableHead>
                </TableRow>

            </TableHeader>
            <TableBody>
                <TableRow>
                    <TableHead className="text-xs">
                        {t("productStatus")}
                    </TableHead>
                    <TableCell colSpan={3} className="text-xs">
                        {t("newProduct")}
                    </TableCell>
                </TableRow>
                <TableRow>
                    <TableHead className="text-xs">
                        {t("company")}
                    </TableHead>
                    <TableCell colSpan={3} className="text-xs">
                        {gameSalesInfo?.company}
                    </TableCell>
                </TableRow>
                <TableRow>
                    <TableHead className="text-xs">
                        {t("game")}
                    </TableHead>
                    <TableCell colSpan={3} className="text-xs">
                        {gameSalesInfo?.game}
                    </TableCell>
                </TableRow>
                <TableRow>
                    <TableHead className="text-xs">
                        {t("origin")}
                    </TableHead>
                    <TableCell className="text-xs">
                        {gameSalesInfo?.origin}
                    </TableCell>
                    <TableHead className="text-xs">
                        {t("recommendedAge")}
                    </TableHead>
                    <TableCell className="text-xs">
                        {gameSalesInfo?.recommendedAge}
                    </TableCell>
                </TableRow>
                <TableRow>
                    <TableHead className="text-xs">
                        {t("receiptIssuance")}
                    </TableHead>
                    <TableCell colSpan={3} className="text-xs">
                        {t("creditCardReceipt/CashReceipt")}
                    </TableCell>
                </TableRow>
            </TableBody>
        </Table>
    )
}
