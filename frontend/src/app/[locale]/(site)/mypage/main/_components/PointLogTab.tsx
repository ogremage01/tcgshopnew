"use client";

import { Card, CardHeader } from "@/components/ui/card";
import { Table, TableHeader, TableBody, TableRow, TableCell, TableHead } from "@/components/ui/table";
import { useCallback, useEffect, useState } from "react";
import { api, getApiErrorMessage } from "@/lib/api";
import { PointLogUserDto } from "@/types/user";
import { useAuthStore } from "@/stores/auth-store";
import { useLocale, useTranslations } from "next-intl";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import { Page } from "@/types/pagination";

const PAGE_SIZE = 10;
const TM = "mypage.main";

export default function PointLogTab() {
    const { user } = useAuthStore();
    const [pointLogs, setPointLogs] = useState<PointLogUserDto[]>([]);
    const t = useTranslations();
    const locale = useLocale();
    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [loading, setLoading] = useState(true);

    const loadPointLogs = useCallback(() => {
        setLoading(true);
        api.get<Page<PointLogUserDto>>("/api/user/point-log", undefined, {
            params: {
                page: currentPage - 1,
                size: PAGE_SIZE,
                sort: "actionDate,desc",
            },
        })
            .then((response) => {
                setPointLogs(response.content ?? []);
                setTotalPages(Math.max(1, response.totalPages ?? 1));
            })
            .catch((error) => {
                alert(getApiErrorMessage(error) ?? t("common.error"));
            })
            .finally(() => setLoading(false));
    }, [currentPage, t]);

    useEffect(() => {
        loadPointLogs();
    }, [loadPointLogs]);

    return (
        <div className="flex flex-col gap-4">
            <Card>
                <CardHeader>
                    <span className="text-2xl font-bold">{t(`${TM}.pointLog.title`)}</span>
                    <span>
                        {t(`${TM}.pointLog.currentPoint`, {
                            point: user?.point.toLocaleString(locale) ?? "0",
                        })}
                    </span>
                </CardHeader>
                {loading ? (
                    <p className="px-6 pb-6 text-muted-foreground">{t("products.loading")}</p>
                ) : pointLogs.length > 0 ? (
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>{t(`${TM}.pointLog.columns.date`)}</TableHead>
                                <TableHead>{t(`${TM}.pointLog.columns.changedPoint`)}</TableHead>
                                <TableHead>{t(`${TM}.pointLog.columns.beforeAfter`)}</TableHead>
                                <TableHead>{t(`${TM}.pointLog.columns.executor`)}</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {pointLogs.map((pointLog) => (
                                <TableRow key={pointLog.id}>
                                    <TableCell>{new Date(pointLog.actionDate).toLocaleString(locale)}</TableCell>
                                    <TableCell>{pointLog.changedPoint.toLocaleString(locale)}</TableCell>
                                    <TableCell>{pointLog.beforePoint.toLocaleString(locale)} → {pointLog.afterPoint.toLocaleString(locale)}</TableCell>
                                    <TableCell>{pointLog.executorDisplay}</TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                ) : (
                    <p className="px-6 pb-6 text-muted-foreground">{t(`${TM}.pointLog.empty`)}</p>
                )}
                <div className="px-6 pb-6">
                    <PaginationRangeAsync
                        totalPages={totalPages}
                        currentPage={currentPage}
                        siblingCount={2}
                        onPageChange={setCurrentPage}
                    />
                </div>
            </Card>
        </div>
    );
}
