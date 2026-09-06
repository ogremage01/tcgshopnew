"use client";

import { useParams, useSearchParams } from "next/navigation";
import GuestOrderDetail from "../../_components/GuestOrderDetail";

export default function GuestOrderDetailPage() {
    const params = useParams<{ id: string }>();
    const searchParams = useSearchParams();
    const orderId = params?.id as string;
    const guestCode = searchParams?.get("guestCode") ?? undefined;

    return <GuestOrderDetail orderId={orderId} guestCode={guestCode} />;
}
