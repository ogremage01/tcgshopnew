"use client";
import ProductImage from "@/components/product/ProductImage";
import type { MypageOrderProductLineDto } from "@/types/order";

export default function OrderItem({ item }: { item: MypageOrderProductLineDto }) {
    return (
        <div className="flex flex-row gap-2">
            <div className="flex flex-col gap-2">
                <span>{item.name}</span>
                <span>{item.price}</span>
                <span>{item.quantity}</span>
                <span>{item.total}</span>
            </div>
            <div className="flex flex-col gap-2">
                <ProductImage src={item.imageUrl} alt={item.name} className="w-20 h-20 object-cover" width={80} height={80} />
            </div>
        </div>
    );
}