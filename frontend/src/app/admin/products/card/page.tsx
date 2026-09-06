"use client";

import { useState } from "react";
import AdminCardCatalogSearchSection from "./_components/AdminCardCatalogSearchSection";
import AdminCardSearchResultSections from "./_components/AdminCardSearchResultSections";

export default function AdminProductsCardPage() {
  const [keyword, setKeyword] = useState("");
  const [searchKey, setSearchKey] = useState(0);

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">싱글카드 관리</h2>

      <AdminCardCatalogSearchSection
        onSearchRequest={(kw) => {
          setKeyword(kw);
          setSearchKey((k) => k + 1);
        }}
      />
      <AdminCardSearchResultSections key={searchKey} keyword={keyword} />
    </div>
  );
}
