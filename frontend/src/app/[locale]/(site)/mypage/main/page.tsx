"use client";

import { useState, useEffect } from "react";
import { useAuthStore } from "@/stores/auth-store";
import { api } from "@/lib/api";
import type { UserResponseDto } from "@/types/user";
import { useTranslations } from "next-intl";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";

import MainInfo from "./_components/MainInfoTab";
import OrderTab from "./_components/OrderTab";
import PointLogTab from "./_components/PointLogTab";

export default function MyPageMain() {
  const [user, setUser] = useState<UserResponseDto | null>(null);
  const { isAuthenticated } = useAuthStore();
  const t = useTranslations();

  useEffect(() => {
    if (isAuthenticated) {
      api.get<UserResponseDto>("/api/user/me").then((userData) => {
        setUser(userData as UserResponseDto);
      }).catch(() => {
        useAuthStore.getState().logout();
      });
    }
  }, [isAuthenticated]);

  return (
    user && (

      <div className="my-2">
        <Tabs defaultValue="main">
          <TabsList>
            <TabsTrigger value="main">{t("mypage.main.tab.main")}</TabsTrigger>
            <TabsTrigger value="order">{t("mypage.main.tab.order")}</TabsTrigger>
            <TabsTrigger value="point">{t("mypage.main.tab.point")}</TabsTrigger>
          </TabsList>
          <TabsContent value="main">
            <MainInfo user={user} />
          </TabsContent>
          <TabsContent value="order">
            <OrderTab />
          </TabsContent>
          <TabsContent value="point">
            <PointLogTab />
          </TabsContent>
        </Tabs>
      </div>
    )
  );
}

