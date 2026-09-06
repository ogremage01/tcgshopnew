"use client";
import { Input } from "@/components/ui/input";
import { useState } from "react";
import { apiClient } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Search } from "lucide-react";
import {
  Field,
  FieldLabel,
  FieldDescription,
  FieldContent,
} from "@/components/ui/field";
export type AdminSearchBarSearchRequest = {
  keyword: string;
  page: number;
  size: number;
};

export default function AdminSearchBar({
  url,
  queryParam,
  pageParam,
  sizeParam,
  placeholder,
  onSearch,
  onSearchRequest,
  description,
}: {
  url: string;
  queryParam: string;
  pageParam: string;
  sizeParam: string;
  placeholder: string;
  onSearch?: (value: unknown) => void;
  /** 설정 시 검색 API는 호출하지 않고 부모가 페이지네이션·요청을 처리합니다. */
  onSearchRequest?: (ctx: AdminSearchBarSearchRequest) => void;
  description: string;
}) {
  const [page, setPage] = useState(0);
  const [size] = useState(12);
  const [value, setValue] = useState("");
  const handleSearch = () => {
    if (onSearchRequest) {
      onSearchRequest({ keyword: value, page: 0, size });
      setValue("");
      return;
    }
    if (!onSearch) return;
    apiClient
      .get(`${url}`, {
        params: {
          [pageParam]: page,
          [sizeParam]: size,
          [queryParam]: value,
        },
      })
      .then((response) => {
        onSearch(response.data);
      });
  };
  return (
    <Field>
      <FieldLabel>{description}</FieldLabel>
      <FieldContent className="flex flex-row gap-2">
        <Input
          className="w-full"
          type="text"
          placeholder={placeholder}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              handleSearch();
            }
          }}
        />
        <Button
          className="w-fit"
          onClick={handleSearch}
          disabled={value.length < 2}
        >
          <Search />
        </Button>
      </FieldContent>
      <FieldDescription></FieldDescription>
    </Field>
  );
}
