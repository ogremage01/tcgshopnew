"use client";

import { useTranslations } from "next-intl";
import { ScrollArea } from "@/components/ui/scroll-area";

export function PrivacyContent() {
  const t = useTranslations();

  return (
    <ScrollArea className="text-sm h-[500px]">
      <div dangerouslySetInnerHTML={{ __html: t("privacy.content") }} />
    </ScrollArea>
  );
}
