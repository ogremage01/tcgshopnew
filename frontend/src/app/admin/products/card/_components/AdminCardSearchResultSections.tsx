"use client";

import AdminCardProductTable from "./AdminCardProductTable";
import AdminCardSearchResults from "./AdminCardSearchResults";
import { useCardProductMetadata } from "../_hooks/useCardProductMetadata";
import { useProductManagementSearch } from "../_hooks/useProductManagementSearch";
import { useUnionPriceSearch } from "../_hooks/useUnionPriceSearch";
import type { UnionPriceSearchFilters } from "../_hooks/useUnionPriceSearch";
import { useState } from "react";

type AdminCardSearchResultSectionsProps = {
  keyword: string;
};

export default function AdminCardSearchResultSections({
  keyword,
}: AdminCardSearchResultSectionsProps) {
  const [productMgmtFilters, setProductMgmtFilters] =
    useState<UnionPriceSearchFilters>({});
  const [unionPriceFilters, setUnionPriceFilters] =
    useState<UnionPriceSearchFilters>({});

  const unionPrice = useUnionPriceSearch(keyword, unionPriceFilters);
  const productMgmt = useProductManagementSearch(keyword, productMgmtFilters);
  const { storageList, gradePolicyList } = useCardProductMetadata();

  return (
    <>
      <AdminCardProductTable
        cardList={productMgmt.data}
        facets={productMgmt.facets}
        filters={productMgmtFilters}
        currentPage={productMgmt.currentPage}
        totalPages={productMgmt.totalPages}
        onPageChange={productMgmt.handlePageChange}
        onApplyFilters={(filters) => {
          setProductMgmtFilters(filters);
          productMgmt.handlePageChange(1);
        }}
        onProductDeleted={productMgmt.removeFromList}
      />
      <AdminCardSearchResults
        currentPage={unionPrice.currentPage}
        totalPages={unionPrice.totalPages}
        onPageChange={unionPrice.handlePageChange}
        searchedCardList={unionPrice.data}
        totalElements={unionPrice.totalElements}
        facets={unionPrice.facets}
        storageList={storageList}
        gradePolicyList={gradePolicyList}
        filters={unionPriceFilters}
        onApplyFilters={(filters) => {
          setUnionPriceFilters(filters);
          unionPrice.handlePageChange(1);
        }}
      />
    </>
  );
}
