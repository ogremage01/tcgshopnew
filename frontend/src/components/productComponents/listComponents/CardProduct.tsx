import type { ProductItemDto } from "@/types/product";
import CardProductRow from "./CardProductRow";
import ProductImage from "@/components/product/ProductImage";
import { capitalizeFirstLetter } from "@/lib/utils";
import PrintTypeLabel from "@/components/product/PrintTypeLabel";
import { isGameSetBrowsePath } from "@/lib/product-browse";
import { usePathname } from "@/i18n/navigation";
import { useLocale } from "next-intl";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import Link from "next/link";
import {
  languageDisplayName,
  languageImageCandidates,
  sortSaleMapEntries,
} from "@/lib/card-product-language";
import { useCardProductLanguages } from "@/hooks/use-card-product-languages";

export default function CardProduct({
  productItemDto,
  handleAddToCart,
}: {
  productItemDto: ProductItemDto;
  handleAddToCart: (searchMapId: number, quantity: number) => void;
}) {
  const pathname = usePathname();
  const locale = useLocale();
  const { languages } = useCardProductLanguages();
  const hideGameSetMeta = isGameSetBrowsePath(pathname);
  const languageGroups = sortSaleMapEntries(
    productItemDto.card?.cardProductSaleDtoMap,
  );
  const firstLanguage = languageGroups[0]?.code ?? "en";
  const imageCandidates = languageImageCandidates(
    productItemDto.languageImageUrlMap,
    firstLanguage,
    productItemDto.imageUrlEn,
  );

  const isFoil = (productItemDto.card?.printType ?? "")
    .toLowerCase()
    .includes("foil");

  return (
    <div className="flex flex-col">
      <div className="flex flex-col">
        <div className="flex flex-row">
          <ul className="flex flex-row flex-wrap gap-2">
            {!hideGameSetMeta && (
              <>
                <li className="text-gray-500 text-sm">
                  {productItemDto?.card?.game}
                </li>
                <li className="text-gray-500 text-sm">
                  / {productItemDto?.setName}
                </li>
              </>
            )}
            <li className="text-gray-500 text-sm">
              {!hideGameSetMeta && "/ "}
              {capitalizeFirstLetter(productItemDto?.card?.rarity)}
            </li>
            <li
              className={
                isFoil ? "text-black font-bold text-sm" : "text-black text-sm"
              }
            >
              / {capitalizeFirstLetter(productItemDto?.card?.printType)}
            </li>
          </ul>
        </div>
      </div>
      <div className="w-full justify-between gap-2 rounded-md bg-gray-100 flex flex-col sm:flex-row">
        <div className="image-container relative w-full h-fit sm:w-[180px] max-w-[260px] sm:max-w-none mx-auto sm:mx-0 shrink-0 overflow-hidden rounded-lg bg-gray-100 aspect-[63/88]">
          <Link href={`/products/${productItemDto.card?.publicId}`}>
            <ProductImage
              src={imageCandidates[0]}
              fallbackSrcs={imageCandidates.slice(1)}
              alt={productItemDto.productNameEn}
              fill
              sizes="(max-width: 640px) 70vw, 180px"
              className="object-contain object-top"
            />
          </Link>
          <div className="absolute bottom-2 right-2">
            <PrintTypeLabel
              printing={
                productItemDto.card?.printing ?? productItemDto.card?.printType
              }
              foilHighlight="badge"
              className="text-sm"
            />
          </div>
        </div>
        <div className="flex min-w-0 flex-1 flex-row gap-2 overflow-hidden">
          <div className="flex w-full min-w-0 flex-col gap-2">
            {languageGroups.map(({ code, sales }) => (
              <div key={code} className="flex min-w-0 flex-row items-center">
                <div className="flex w-full min-w-0 flex-col text-left">
                  <Label className="text-gray-500 my-2 font-bold">
                    {languageDisplayName(code, locale, languages)}
                  </Label>
                  {sales.map((sale, idx) => (
                    <div
                      key={sale.id ?? `unavailable-${idx}`}
                      className="w-full min-w-0 transition-colors duration-200 hover:bg-gray-200"
                    >
                      <CardProductRow
                        sale={sale}
                        handleAddToCart={handleAddToCart}
                        rewardPercentage={productItemDto.rewardPercentage}
                      />
                    </div>
                  ))}
                  <Separator className="my-1" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
