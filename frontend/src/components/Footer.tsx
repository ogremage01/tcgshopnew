"use client";

import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Separator } from "./ui/separator";

export default function Footer() {
  const t = useTranslations("footer");
  return (
    <footer className="border-t py-4 px-4 sm:px-6 lg:px-8 text-start text-xs sm:text-sm text-muted-foreground bg-gray-100">
      <div className="flex flex-col gap-4">
        <div className="flex flex-row gap-2 justify-center">
          <span>{t("copyright")}</span>
        </div>
        <div className="grid grid-cols-2 justify-between gap-2 mx-auto w-fit">
          <div className="flex flex-col gap-2">
            <span>
              {t("companyName")}: {t("companyNameValue")}
            </span>
            <span>
              {t("representativeName")}: {t("representativeNameValue")}
            </span>
            <span>
              {t("businessRegistrationNumber")}:{" "}
              {t("businessRegistrationNumberValue")}
            </span>
            <span>
              {t("businessRegistrationNumberfortelecom")}:{" "}
              {t("businessRegistrationfortelecomNumberValue")}
            </span>
            <span>
              {t("address")}: {t("addressValue")}
            </span>
            <span>{t("phone")}: 000-0000-0000</span>
            <span>{t("email")}: support@example.com</span>
            <span>
              {t("wireTransferAccount")}: {t("transferBankName")}{" "}
              000-000000-00000 Demo TCG Shop
            </span>
          </div>

          <div className="flex flex-col gap-2">
            <Link href="/policy/privacy">{t("privacyPolicy")}</Link>
            <Link href="/policy/use">{t("termsOfService")}</Link>
            <Link href="/policy/cardcondition">{t("cardConditionGuide")}</Link>
            <Link href="/support/how-to-visit">{t("howToVisit")}</Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
