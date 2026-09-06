import * as React from "react"
import {
  Pagination,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
  PaginationContent,
} from "@/components/ui/pagination"

type PaginationRangeProps = {
  // 현재 페이지
  currentPage: number
  // 총 페이지 수
  totalPages: number
  // 페이지 범위 설정 (기본값: 2)
  siblingCount?: number
  // 페이지 이동 함수
  hrefForPage: (page: number) => string
}

export default function PaginationRange({
  // 현재 페이지
  currentPage,
  // 총 페이지 수
  totalPages,
  // 페이지 범위 설정 (기본값: 2)
  siblingCount = 2,
  // 페이지 이동 함수
  hrefForPage,
}: PaginationRangeProps) {
  if (totalPages <= 1) return null

  const startPage = Math.max(1, currentPage - siblingCount)
  const endPage = Math.min(totalPages, currentPage + siblingCount)
  const pages = Array.from({ length: endPage - startPage + 1 }, (_, idx) => startPage + idx)

  const showFirst = startPage > 1
  const showFirstEllipsis = startPage > 3
  const showLast = endPage < totalPages
  const showLastEllipsis = endPage < totalPages - 2

  return (
    <Pagination>
      {currentPage > 1 && <PaginationPrevious href={hrefForPage(currentPage - 1)} />}

      <PaginationContent>
        {showFirst && (
          <>
            <PaginationItem>
              <PaginationLink href={hrefForPage(1)} isActive={currentPage === 1}>
                1
              </PaginationLink>
            </PaginationItem>
            {showFirstEllipsis && <PaginationEllipsis />}
          </>
        )}

        {pages.map(page => (
          <PaginationItem key={page}>
            <PaginationLink href={hrefForPage(page)} isActive={page === currentPage}>
              {page}
            </PaginationLink>
          </PaginationItem>
        ))}

        {showLast && (
          <>
            {showLastEllipsis && <PaginationEllipsis />}
            <PaginationItem>
              <PaginationLink
                href={hrefForPage(totalPages)}
                isActive={currentPage === totalPages}
              >
                {totalPages}
              </PaginationLink>
            </PaginationItem>
          </>
        )}
      </PaginationContent>

      {currentPage < totalPages && <PaginationNext href={hrefForPage(currentPage + 1)} />}
    </Pagination>
  )
}
