"use client";

import { ProductIpsContent } from "./_components/ProductIpsContent";

export default function ProductIpPage() {
  return (
    <div>
      <h1 className="text-2xl font-bold">제품 IP 관리</h1>
      <p className="text-muted-foreground text-sm">
        제품 IP를 관리합니다. 수동 상품 등록 시 이 목록에서 선택할 수 있습니다.
      </p>
      <div className="mt-4">
        <ProductIpsContent />
      </div>
    </div>
  );
}
