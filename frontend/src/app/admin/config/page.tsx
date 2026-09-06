"use client"

import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import { BannerConfigTab } from "./_components/banner-config-tab"
import { MainHeaderTab } from "./_components/main-header-tab"
import { MainPageContentTab } from "./_components/main-page-content-tab"
import { SetBannerConfigTab } from "./_components/SetBannerConfigTab"

export default function AdminConfigPage() {
  const bannerTabs = [
    { value: "home", label: "메인" },
    { value: "mtg", label: "MTG" },
    { value: "fab", label: "FAB" },
    { value: "lorcana", label: "Lorcana" },
    { value: "starwars", label: "Star Wars" },
    { value: "rift", label: "riftbound" },
    { value: "supplies", label: "서플라이" },
  ] as const

  return (
    <div>
      <h1 className="text-2xl font-bold">설정</h1>
      <p className="text-sm text-muted-foreground">사이트 설정을 관리합니다.</p>
      <Tabs defaultValue="mainPageContent">
        <TabsList>
          <TabsTrigger value="mainPageContent">메인 콘텐츠</TabsTrigger>
          <TabsTrigger value="mainHeader">헤더 메뉴</TabsTrigger>
          {bannerTabs.map((tab) => (
            <TabsTrigger key={tab.value} value={tab.value}>
              {tab.label} 배너
            </TabsTrigger>
          ))}
          <TabsTrigger value="gameBanner">게임별배너</TabsTrigger>
        </TabsList>

        <TabsContent value="mainPageContent">
          <MainPageContentTab />
        </TabsContent>

        <TabsContent value="mainHeader">
          <MainHeaderTab />
        </TabsContent>

        {bannerTabs.map((tab) => (
          <TabsContent key={tab.value} value={tab.value}>
            <BannerConfigTab target={tab.value} title={tab.label} />
          </TabsContent>
        ))}
        <TabsContent value="gameBanner">
          <SetBannerConfigTab />
        </TabsContent>
      </Tabs>
    </div>
  )
}
