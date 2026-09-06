"use client";
import { useEffect, useState } from "react";
import { ButtonGroup } from "@/components/ui/button-group";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { parseQuantityFromInput, clampQuantity } from "@/utils/productGuards";
import { ShoppingBasket, Plus, Minus } from "lucide-react";

export default function ProductQuantityButton({
  onAddToCart,
  maxStock,
  disabled = false,
  initialQuantity = 1,
  resetKey,
  layout = "row",
}: {
  onAddToCart: (quantity: number) => void;
  maxStock: number;
  disabled?: boolean;
  initialQuantity?: number;
  resetKey?: string | number | null;
  layout?: "row" | "grid" | "list";
}) {
  const [selectedQuantity, setSelectedQuantity] = useState<number>(
    clampQuantity(initialQuantity, maxStock),
  );

  useEffect(() => {
    setSelectedQuantity((q) => clampQuantity(q, maxStock));
  }, [maxStock]);

  useEffect(() => {
    setSelectedQuantity(clampQuantity(initialQuantity, maxStock));
  }, [resetKey, initialQuantity, maxStock]);

  const commitQuantity = () => {
    setSelectedQuantity((q) => clampQuantity(q, maxStock));
  };

  const handleQuantityChange = (mode: "increase" | "decrease") => {
    if (maxStock < 1) return;
    setSelectedQuantity((prev) => {
      if (mode === "increase") {
        return Math.min(prev + 1, maxStock);
      }
      return Math.max(1, prev - 1);
    });
  };

  const addToCartIfValid = (quantity: number) => {
    if (disabled || maxStock < 1) return;
    onAddToCart(clampQuantity(quantity, maxStock));
  };

  const containerClassName =
    layout === "list"
      ? "flex shrink-0 flex-row items-center gap-0.5"
      : layout === "grid"
        ? "mx-auto flex w-fit flex-row items-center justify-between gap-1 lg:w-full"
        : "mx-auto flex w-fit flex-row items-center justify-between gap-1 lg:w-full";

  const iconButtonClassName =
    layout === "list"
      ? "h-8 w-8 shrink-0 p-0 [&_svg]:size-4"
      : "h-8 w-8 shrink-0 p-0 sm:h-9 sm:w-9 md:h-10 md:w-10 [&_svg]:size-4 sm:[&_svg]:size-5 md:[&_svg]:size-6";

  const quantityInputClassName =
    layout === "list"
      ? "h-8 w-9 shrink-0 border-input px-1 text-center text-sm no-spinner focus:outline-none focus:ring-0 focus-visible:ring-0 focus-visible:ring-offset-0"
      : "h-8 w-10 border-input px-1 text-center text-sm no-spinner focus:outline-none focus:ring-0 focus-visible:ring-0 focus-visible:ring-offset-0 sm:h-9 sm:w-11 sm:px-2 md:h-10 md:w-12";

  return (
    <div className={containerClassName}>
      <ButtonGroup>
        <Button
          type="button"
          variant="outline"
          size="icon"
          onClick={() => handleQuantityChange("increase")}
          className={iconButtonClassName}
        >
          <Plus />
        </Button>
        <Input
          type="number"
          value={selectedQuantity}
          onChange={(e) => {
            const next = parseQuantityFromInput(e.target.value, maxStock);
            if (next === null) return;
            setSelectedQuantity(next);
          }}
          onBlur={() => {
            commitQuantity();
          }}
          className={quantityInputClassName}
          min={1}
          max={maxStock}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              const clamped = clampQuantity(selectedQuantity, maxStock);
              setSelectedQuantity(clamped);
              addToCartIfValid(clamped);
            }
          }}
        />
        <Button
          type="button"
          variant="outline"
          size="icon"
          onClick={() => handleQuantityChange("decrease")}
          className={iconButtonClassName}
        >
          <Minus />
        </Button>
      </ButtonGroup>
      <Button
        className={iconButtonClassName}
        size="icon"
        type="button"
        variant="outline"
        onClick={() => addToCartIfValid(selectedQuantity)}
        disabled={disabled}
      >
        <ShoppingBasket />
      </Button>
    </div>
  );
}
