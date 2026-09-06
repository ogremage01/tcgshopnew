"use client";

import { useProductConfigBootstrap } from "@/app/admin/products/config/_hooks/useProductConfigBootstrap";
import ProductConfigPageContent from "@/app/admin/products/config/_components/ProductConfigPageContent";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

function ProductConfigPageLoading() {
  return (
    <div className="flex flex-col gap-4">
      <Skeleton className="h-8 w-48" />
      <div className="h-9 w-64 max-w-full border-2 border-black rounded-md p-1 flex gap-1">
        <Skeleton className="h-7 w-20" />
        <Skeleton className="h-7 w-24" />
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-40" />
          </CardHeader>
          <CardContent className="space-y-2">
            <Skeleton className="h-9 w-full" />
            <Skeleton className="h-24 w-full" />
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-40" />
          </CardHeader>
          <CardContent>
            <Skeleton className="h-32 w-full" />
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-32" />
          </CardHeader>
          <CardContent>
            <Skeleton className="h-40 w-full" />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default function ProductConfigPage() {
  const { loading, error, data, refetch } = useProductConfigBootstrap();

  if (loading) {
    return <ProductConfigPageLoading />;
  }

  if (error || !data) {
    return (
      <div className="flex flex-col gap-4 max-w-md">
        <h1 className="text-2xl font-bold">싱글카드 설정</h1>
        <p className="text-sm text-destructive">
          초기 데이터를 불러오지 못했습니다.
        </p>
        <p className="text-xs text-muted-foreground">{error?.message}</p>
        <Button type="button" onClick={() => void refetch()}>
          다시 시도
        </Button>
      </div>
    );
  }

  return <ProductConfigPageContent data={data} />;
}
