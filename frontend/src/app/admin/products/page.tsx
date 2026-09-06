import Link from "next/link";

export const metadata = {
  title: "상품 관리",
  description: "관리자 상품 등록·수정·삭제",
};

export default function AdminProductsPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link
          href="/admin"
          className="text-sm text-muted-foreground hover:text-foreground"
        >
          ← 대시보드
        </Link>
      </div>
      <h2 className="text-2xl font-bold">상품 관리</h2>
      <p className="text-muted-foreground text-sm">상품 목록·등록·수정 기능은 추후 구현 예정입니다.</p>
    </div>
  );
}
