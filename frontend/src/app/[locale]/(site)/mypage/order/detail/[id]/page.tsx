"use client";

import { useParams } from "next/navigation";
import MypageOrderDetail from "../../_components/MypageOrderDetail";

export default function MypageOrderDetailPage() {
    const params = useParams<{ id: string }>();
    const id = params?.id as string;

    return <MypageOrderDetail orderId={id} />;
}
