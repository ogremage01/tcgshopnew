"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { isAxiosError } from "axios";
import { toast } from "sonner";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api.client";
import { useRouter } from "@/i18n/navigation";
import type { UserOrderDetailDto } from "@/types/order";
import {useTranslations} from "next-intl";

export default function GuestOrderCheckPage() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const [submitting, setSubmitting] = useState(false);
    const t = useTranslations();
    const defaultOrderId = searchParams?.get("orderId") ?? "";
    const defaultGuestCode = searchParams?.get("guestCode") ?? "";

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const formData = new FormData(e.target as HTMLFormElement);
        const orderIdRaw = (formData.get("orderId") as string)?.trim();
        const guestCode = (formData.get("guestCode") as string)?.trim();

        const orderId = Number(orderIdRaw);
        if (!orderIdRaw || Number.isNaN(orderId) || orderId <= 0) {
            toast.error(t("guestOrderCheck.error.orderId"));
            return;
        }
        if (!guestCode) {
            toast.error(t("guestOrderCheck.error.guestCode"));
            return;
        }

        setSubmitting(true);
        try {
            await api.post<UserOrderDetailDto>("/api/guest/orders/lookup", {
                orderId,
                verificationCode: guestCode,
            });
            router.push(
                `/guest-order-check/detail/${orderId}?guestCode=${encodeURIComponent(guestCode)}`,
            );
        } catch (err) {
            if (isAxiosError(err) && err.response?.status === 404) {
                toast.error(t("guestOrderCheck.error.notFound"));
                return;
            }
            toast.error(t("guestOrderCheck.error.failed"));
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="flex flex-col gap-4 my-4 max-w-2xl mx-auto">
            <Card>
                <CardHeader>
                    <CardTitle>{t("guestOrderCheck.title")}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">
                        {t("guestOrderCheck.description")}
                    </p>
                    <form className="flex flex-col sm:flex-row gap-2" onSubmit={handleSubmit}>
                        <Input
                            name="orderId"
                            type="text"
                            inputMode="numeric"
                            placeholder={t("guestOrderCheck.placeholder.orderId")}
                            defaultValue={defaultOrderId}
                            required
                            disabled={submitting}
                        />
                        <Input
                            name="guestCode"
                            type="text"
                            placeholder={t("guestOrderCheck.placeholder.guestCode")}
                            defaultValue={defaultGuestCode}
                            required
                            disabled={submitting}
                        />
                        <Button type="submit" variant="outline" disabled={submitting}>
                            {submitting ? t("guestOrderCheck.button.submitting") : t("guestOrderCheck.button.submit")}
                        </Button>
                    </form>
                </CardContent>
            </Card>
        </div>
    );
}
