import { capitalizeFirstLetter, cn } from "@/lib/utils";

type PrintTypeLabelProps = {
  printing?: string | null;
  printType?: string | null;
  foilHighlight?: "badge" | "bold" | "none";
  as?: "span" | "li";
  prefix?: string;
  className?: string;
};

function resolvePrinting(
  printing?: string | null,
  printType?: string | null,
): string {
  return printing ?? printType ?? "";
}

function isFoilPrinting(printing: string): boolean {
  return printing.toLowerCase().includes("foil");
}

function getFoilBadgeClass(printing: string): string {
  const base = "p-0.5 rounded-md text-white";

  switch (printing.toLowerCase()) {
    case "cold foil":
      return `${base} bg-cyan-600`;
    case "holofoil":
      return `${base} bg-purple-600`;
    case "foil":
      return `${base} bg-green-600`;
    case "foil etched":
      return `${base} bg-amber-700`;
    case "rainbow foil":
      return `${base} bg-gradient-to-r from-rose-500 via-amber-500 to-violet-500`;
    default:
      return "";
  }
}

export default function PrintTypeLabel({
  printing,
  printType,
  foilHighlight = "bold",
  as: Component = "span",
  prefix,
  className,
}: PrintTypeLabelProps) {
  const resolvedPrinting = resolvePrinting(printing, printType);
  const isFoil = isFoilPrinting(resolvedPrinting);

  const highlightClass =
    foilHighlight === "badge"
      ? getFoilBadgeClass(resolvedPrinting)
      : foilHighlight === "bold" && isFoil
        ? "font-bold"
        : "";

  return (
    <>
      {isFoil ? (
        <Component className={cn("foil-text-box", highlightClass, className)}>
          {prefix}
          {capitalizeFirstLetter(resolvedPrinting)}
        </Component>
      ) : null}
    </>
  );
}
