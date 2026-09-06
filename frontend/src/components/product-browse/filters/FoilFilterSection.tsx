"use client";

import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { useTranslations } from "next-intl";

type Props = {
  isFoil: boolean | null;
  setNullableBooleanParam: (key: string, value: boolean | null) => void;
};

export default function FoilFilterSection({
  isFoil,
  setNullableBooleanParam,
}: Props) {
  const t = useTranslations("game.searchFilterSidebar");

  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-medium">{t("foilFilter")}</span>
      <RadioGroup
        value={isFoil === true ? "foil" : isFoil === false ? "normal" : "all"}
        onValueChange={(value) =>
          setNullableBooleanParam(
            "isFoil",
            value === "all" ? null : value === "foil",
          )
        }
        className="gap-2"
      >
        <div className="flex flex-row gap-2 items-center">
          <RadioGroupItem value="all" id="isFoil-all" />
          <Label htmlFor="isFoil-all">{t("foilFilterAll")}</Label>
        </div>
        <div className="flex flex-row gap-2 items-center">
          <RadioGroupItem value="foil" id="isFoil-foil" />
          <Label htmlFor="isFoil-foil">{t("foilOnly")}</Label>
        </div>
        <div className="flex flex-row gap-2 items-center">
          <RadioGroupItem value="normal" id="isFoil-normal" />
          <Label htmlFor="isFoil-normal">{t("normalOnly")}</Label>
        </div>
      </RadioGroup>
    </div>
  );
}
