"use client";

import { useEffect } from "react";
import { usePathname, useSearchParams } from "next/navigation";

/**
 * 뒤로가기 등 클라이언트 사이드 네비게이션 후 Radix UI가
 * body 및 DOM에 남겨놓은 잔여 상태를 초기화한다.
 *
 * Radix UI는 Dropdown/Dialog/Sheet 등이 열린 채로 페이지 이동이 발생하면
 * - body 의 pointer-events / overflow / data-scroll-locked
 * - DOM 의 [data-radix-popper-content-wrapper], [data-radix-focus-guard]
 * 등을 정리하지 못하고 남겨두어 이후 인터랙션을 방해한다.
 * (헤더·검색 전체 remount 는 SiteHeaderRegion 참고.)
 */
export default function BodyPointerEventsReset() {
    const pathname = usePathname();
    const searchParams = useSearchParams();

    const resetBodyInteractionState = () => {
        // body 스타일 초기화
        document.body.style.pointerEvents = "";
        document.body.style.overflow = "";
        document.body.removeAttribute("data-scroll-locked");
    };

    useEffect(() => {
        resetBodyInteractionState();
    }, [pathname, searchParams]);

    useEffect(() => {
        // 브라우저 히스토리 복원/뒤로가기 시점에도 body 상태를 복구한다.
        window.addEventListener("pageshow", resetBodyInteractionState);
        window.addEventListener("popstate", resetBodyInteractionState);

        return () => {
            window.removeEventListener("pageshow", resetBodyInteractionState);
            window.removeEventListener("popstate", resetBodyInteractionState);
        };
    }, []);

    return null;
}
