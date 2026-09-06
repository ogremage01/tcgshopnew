"use client";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarGroup,
  SidebarMenuSub,
  SidebarMenuSubItem,
  SidebarMenuSubButton,
} from "@/components/ui/sidebar";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuthStore } from "@/stores/auth-store";

function isCurrentPath(pathname: string, url: string) {
  if (url === "#") return false;
  const normalizedPath = pathname.replace(/\/$/, "") || "/";
  const normalizedUrl = url.replace(/\/$/, "") || "/";
  return normalizedPath === normalizedUrl;
}

function handleNavClick(
  event: React.MouseEvent<HTMLAnchorElement>,
  pathname: string,
  url: string,
) {
  if (!isCurrentPath(pathname, url)) return;
  event.preventDefault();
  window.location.assign(url);
}
const data = {
  navMain: [
    {
      title: "주문관리",
      url: "#",
      items: [
        {
          title: "주문 목록",
          url: "/admin/order/list",
        },
        {
          title: "주문 설정",
          url: "/admin/order/config",
        },
      ],
    },
    {
      title: "상품관리",
      url: "#",
      items: [
        {
          title: "싱글카드 관리",
          url: "/admin/products/card",
        },
        {
          title: "적립금 규칙 관리",
          url: "/admin/products/reward-rule",
        },
        {
          title: "세트별 싱글카드 현황",
          url: "/admin/products/card/checkbyset",
        },
        {
          title: "싱글카드 설정",
          url: "/admin/products/config",
        },
        {
          title: "수동 상품 관리",
          url: "/admin/products/manual-product",
        },
        {
          title: "서플라이 관리",
          url: "/admin/products/supplies",
        },
        {
          title: "밀봉 상품 관리",
          url: "/admin/products/sealed",
        },
        {
          title: "제품 IP 관리",
          url: "/admin/products/product-ip",
        },
        {
          title: "가격 오류 카드 관리",
          url: "/admin/products/card/price-error-card",
        },
      ],
    },
    {
      title: "사이트 설정",
      url: "/admin/config",
    },
    {
      title: "고객관리",
      url: "/admin/user",
    },
    {
      title: "입고 관리",
      url: "#",
      items: [
        {
          title: "입고 관리",
          url: "/admin/products/receiving",
        },
        {
          title: "입고 내역",
          url: "/admin/products/receiving-history",
        },
        {
          title: "포장 단위 관리",
          url: "/admin/products/packaging-units",
        },
      ],
    },
    {
      title: "오프라인 상품 및 판매 내역 관리",
      url: "#",
      items: [
        {
          title: "매출 현황",
          url: "/admin/offline/sales",
        },
        {
          title: "개별 주문 목록",
          url: "/admin/offline/sales-list",
        },
        {
          title: "상품 목록 및 온라인 연동",
          url: "/admin/offline/product",
        },
      ],
    },
    {
      title: "온라인 상품 및 판매 내역 관리",
      url: "#",
      items: [
        {
          title: "온라인 매출 현황",
          url: "/admin/analyze/sales",
        },
      ],
    },
    {
      title: "상품 재고 관리",
      url: "#",
      items: [
        {
          title: "누적 판매량 및 매출 현황",
          url: "/admin/analyze/offline-sales-total",
        },
        {
          title: "온라인 재고 현황",
          url: "/admin/analyze/stock",
        },
      ],
    },
    {
      title: "매출 종합",
      url: "#",
      items: [
        {
          title: "매출 종합",
          url: "/admin/analyze/sales-report",
        },
      ],
    },

    {
      title: "시스템 로그",
      url: "/admin/system-log",
    },
  ],
};
export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const router = useRouter();
  const pathname = usePathname() ?? "";
  return (
    <Sidebar collapsible="offcanvas" {...props}>
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton
              asChild
              className="data-[slot=sidebar-menu-button]:p-1.5!"
            >
              <Link
                href="/admin"
                onClick={(e) => handleNavClick(e, pathname, "/admin")}
              >
                <span className="text-base font-semibold">admin</span>
              </Link>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarMenu>
            {data.navMain.map((item) => (
              <SidebarMenuItem key={item.title}>
                <SidebarMenuButton asChild>
                  <Link
                    href={item.url}
                    className="font-medium"
                    onClick={(e) => handleNavClick(e, pathname, item.url)}
                  >
                    {item.title}
                  </Link>
                </SidebarMenuButton>
                {item.items?.length ? (
                  <SidebarMenuSub>
                    {item.items.map((subItem) => (
                      <SidebarMenuSubItem key={subItem.title}>
                        <SidebarMenuSubButton asChild>
                          <Link
                            href={subItem.url}
                            onClick={(e) =>
                              handleNavClick(e, pathname, subItem.url)
                            }
                          >
                            {subItem.title}
                          </Link>
                        </SidebarMenuSubButton>
                      </SidebarMenuSubItem>
                    ))}
                  </SidebarMenuSub>
                ) : null}
              </SidebarMenuItem>
            ))}
          </SidebarMenu>
        </SidebarGroup>
      </SidebarContent>
      <SidebarFooter>
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-gray-500">
            {useAuthStore.getState().user?.name}님
          </span>
          <button
            onClick={() => {
              useAuthStore.getState().logout();
              router.push("/");
            }}
          >
            <span>로그아웃</span>
          </button>
        </div>
      </SidebarFooter>
    </Sidebar>
  );
}
