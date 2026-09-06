
import {
  Pagination,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
  PaginationContent,
} from "@/components/ui/pagination-async"
import { cn } from "@/lib/utils"

type PaginationRangeAsyncProps = {
  // 현재 페이지
  currentPage: number
  // 총 페이지 수
  totalPages: number
  // 페이지 범위 설정 (기본값: 2)
  siblingCount?: number
  // 페이지 이동 함수
  onPageChange: (page: number) => void
}

export default function PaginationRangeAsync({
  // 현재 페이지
  currentPage,
  // 총 페이지 수
  totalPages,
  // 페이지 범위 설정 (기본값: 2)
  siblingCount = 2,
  // 페이지 이동 함수
  onPageChange,
}: PaginationRangeAsyncProps) {
  if (totalPages <= 1) return null

  const startPage = Math.max(1, currentPage - siblingCount)
  const endPage = Math.min(totalPages, currentPage + siblingCount)
  const pages = Array.from({ length: endPage - startPage + 1 }, (_, idx) => startPage + idx)

  const showFirst = startPage > 1
  const showFirstEllipsis = startPage > 3
  const showLast = endPage < totalPages
  const showLastEllipsis = endPage < totalPages - 2

  return (
    <div className="flex justify-center my-2">
      <Pagination>
        {currentPage > 1 && <PaginationPrevious onClick={() => onPageChange(currentPage - 1)} isActive={currentPage === 1} />}

        <PaginationContent>
          {showFirst && (
            <>
              <PaginationItem>
                <PaginationLink onClick={() => onPageChange(1)}>
                  1
                </PaginationLink>
              </PaginationItem>
              {showFirstEllipsis && <PaginationEllipsis />}
            </>
          )}

          {pages.map(page => (
            <PaginationItem className={cn("", currentPage === page ? "bg-primary text-primary-foreground rounded-md" : "")} key={page}>
              <PaginationLink onClick={() => onPageChange(page)}>
                {page}
              </PaginationLink>
            </PaginationItem>
          ))}

          {showLast && (
            <>
              {showLastEllipsis && <PaginationEllipsis />}
              <PaginationItem>
                <PaginationLink
                  onClick={() => onPageChange(totalPages)}
                  isActive={currentPage === totalPages}
                >
                  {totalPages}
                </PaginationLink>
              </PaginationItem>
            </>
          )}
        </PaginationContent>

        {currentPage < totalPages && <PaginationNext onClick={() => onPageChange(currentPage + 1)} />}
      </Pagination>
    </div>
  )
}

