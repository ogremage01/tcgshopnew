"use client";

import ProductImage from "@/components/product/ProductImage";
import { PRODUCT_GRID_IMAGE_SIZES } from "@/constants/productImage";
import { cn } from "@/lib/utils";

type GridProductImageProps = {
  src?: string | null;
  fallbackSrcs?: Array<string | null | undefined>;
  alt: string;
  className?: string;
};

export default function GridProductImage({
  src,
  fallbackSrcs,
  alt,
  className,
}: GridProductImageProps) {
  return (
    <ProductImage
      src={src ?? undefined}
      fallbackSrcs={fallbackSrcs}
      alt={alt}
      fill
      sizes={PRODUCT_GRID_IMAGE_SIZES}
      className={cn("object-contain sm:object-top", className)}
    />
  );
}
