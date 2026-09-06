"use client";
import { useTranslations } from "next-intl";

export default function NoProductsFoundComponent() {
    const t = useTranslations("game");
    return (
        <div className="flex h-full flex-col items-center justify-center mx-auto my-4">
            <p className="text-2xl font-bold">{t("setList.noProductsFound")}</p>
        </div>
    );
}
