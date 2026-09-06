import { useTranslations } from "next-intl";

export default function HowToVisit() {
  const t = useTranslations();
  return (
    <div className="container mx-auto px-4 py-8 min-h-screen">
      <h1 className="text-2xl font-bold">{t("footer.howToVisit")}</h1>
      <span className="text-sm text-gray-500">
        {t("howToVisit.parkingSpace")}
      </span>
      <p className="mt-4 text-sm text-muted-foreground">
        Store map is omitted in this public snapshot.
      </p>
    </div>
  );
}
