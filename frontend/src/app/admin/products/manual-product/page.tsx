"use client";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ProductListTab } from "./_components/ProductListTab";
import { ProductCategoriesTabContent } from "./_components/ProductCategoriesTabContent";

export default function ManualProductPage() {
    return (
        <div>
            <h1 className="text-2xl font-bold">수동 상품 관리</h1>
            <p className="text-muted-foreground text-sm">수동 등록 상품을 관리합니다.</p>

            <Tabs defaultValue="products">
                <TabsList className="border-2 border-black">
                    <TabsTrigger value="products">상품</TabsTrigger>
                    <TabsTrigger value="categories">카테고리</TabsTrigger>
                </TabsList>
                <TabsContent value="products">
                    <ProductListTab />
                </TabsContent>
                <TabsContent value="categories">
                    <ProductCategoriesTabContent />
                </TabsContent>
            </Tabs>
        </div>
    );
}
