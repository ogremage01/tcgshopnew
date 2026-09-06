"use client";

import { useCallback, useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { Card, CardHeader } from "@/components/ui/card";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import { api, getApiErrorMessage } from "@/lib/api";
import type { Page } from "@/types/pagination";
import type { OrderSimpleDto } from "@/types/order";
import OrderSimpleList from "./OrderSimpleList";

const PAGE_SIZE = 10;

export default function OrderTab() {
    const to = useTranslations("order");
    const t = useTranslations();
    const [orders, setOrders] = useState<OrderSimpleDto[]>([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [loading, setLoading] = useState(true);

    const loadOrders = useCallback(() => {
        setLoading(true);
        api
            .get<Page<OrderSimpleDto>>("/api/user/orders", undefined, {
                params: {
                    page: currentPage - 1,
                    size: PAGE_SIZE,
                    sort: "paymentDate,desc",
                },
            })
            .then((page) => {
                setOrders(page.content ?? []);
                setTotalPages(Math.max(1, page.totalPages ?? 1));
            })
            .catch((err) => {
                alert(getApiErrorMessage(err) ?? t("common.error"));
            })
            .finally(() => setLoading(false));
    }, [currentPage, t]);

    useEffect(() => {
        loadOrders();
    }, [loadOrders]);

    return (
        <div className="flex flex-col gap-4">
            <Card>
                <CardHeader>{t("mypage.main.orderHistory.title")}</CardHeader>
                {loading ? (
                    <p className="px-6 pb-6 text-muted-foreground">{t("products.loading")}</p>
                ) : orders.length > 0 ? (
                    <OrderSimpleList orders={orders} mobileClassName="block p-0.5 md:hidden" />
                ) : (
                    <p className="px-6 pb-6 text-muted-foreground">{to("empty")}</p>
                )}
                <div className="px-6 pb-6">
                    <PaginationRangeAsync
                        currentPage={currentPage}
                        totalPages={totalPages}
                        siblingCount={2}
                        onPageChange={setCurrentPage}
                    />
                </div>
            </Card>
        </div>
    );
}
