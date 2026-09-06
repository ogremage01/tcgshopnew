"use client";

//로딩 스피너
import { Spinner } from "@/components/ui/spinner";
import { useTranslations } from "next-intl";

export default function ProductBrowseLoading() {
    const t = useTranslations("productBrowse");

    return (
        <div
            className="flex min-h-[40vh] flex-col items-center justify-center gap-3"
            role="status"
            aria-live="polite"
        >
            <Spinner className="size-8" />
            <p className="text-sm text-muted-foreground">{t("loading")}</p>
        </div>
    );
}

/** 결과 영역(3/4 너비) 전용 로딩 — 사이드바는 유지 */
export function ProductResultsLoading() {
    const t = useTranslations("productBrowse");

    return (
        <div
            className="flex min-h-[320px] w-full flex-col items-center justify-center gap-3 lg:w-3/4"
            role="status"
            aria-live="polite"
        >
            <Spinner className="size-8" />
            <p className="text-sm text-muted-foreground">{t("loading")}</p>
        </div>
    );
}
