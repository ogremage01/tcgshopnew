import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import CartClient from "./_components/CartClient";
import { Separator } from "@/components/ui/separator";

type Props = { params: Promise<{ locale: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "cart" });
  return {
    title: t("title"),
    description: t("title"),
  };
}

export default function CartPage() {
  return (


    <CartClient />

  );
}
