"use client"

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { SuppliesTabContent } from "./_components/SuppliesTabContent"
import { MakersTabContent } from "./_components/MakersTabContent"
import { SupplyTypesTabContent } from "./_components/SupplyTypesTabContent"

export default function SuppliesPage() {
    return (
        <div>
            <h1 className="text-2xl font-bold">서플라이 관리</h1>
            <p className="text-muted-foreground text-sm">서플라이 등록·수정·삭제를 관리합니다.</p>

            <Tabs defaultValue="supplies">
                <TabsList className="border-2 border-black">
                    <TabsTrigger value="supplies">서플라이</TabsTrigger>
                    <TabsTrigger value="makers">제조사</TabsTrigger>
                    <TabsTrigger value="supply-types">서플라이 분류</TabsTrigger>
                </TabsList>
                <TabsContent value="supplies">
                    <SuppliesTabContent />
                </TabsContent>
                <TabsContent value="makers">
                    <MakersTabContent />
                </TabsContent>
                <TabsContent value="supply-types">
                    <SupplyTypesTabContent />
                </TabsContent>
            </Tabs>
        </div>
    )
}