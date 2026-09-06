"use client"

import { useEffect, useState } from "react";
import { ProductItemDto } from "@/types/product";
import { apiClient } from "@/lib/api.client";
import { useParams } from "next/navigation";
import { Separator } from "@/components/ui/separator";
import ProductImage from "@/components/product/ProductImage";
import CardProductInfo from "./_components/cardgameproduct/CardProductInfo";
import GameSalesInfo from "./_components/cardgameproduct/GameSalesInfo";
import SealedProductInfo from "./_components/cardgameproduct/SealedProductInfo";
import ManualProductInfo from "./_components/manualproduct/ManualProductInfo";
import SupplyProductInfo from "./_components/supply/SupplyProductInfo";
import { Button } from "@/components/ui/button";
import { RefreshCw } from "lucide-react";
import { useTranslations } from "next-intl";
import { languageImageCandidates } from "@/lib/card-product-language";
import PrintTypeLabel from "@/components/product/PrintTypeLabel";

export default function ProductDetailPage() {
    const params = useParams<{ id: string }>();
    const id = params?.id;
    const [productData, setProductData] = useState<ProductItemDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [selectedLanguage, setSelectedLanguage] = useState<string>("en");
    const [side, setSide] = useState<"front" | "back">("front");
    const t = useTranslations("productSalesInfo");
    const tCommon = useTranslations("common");
    useEffect(() => {
        if (!id) return;

        const fetchProductDetail = async () => {
            setIsLoading(true);
            setErrorMessage(null);
            try {
                //console.log(`/api/products/${id}/detail`);
                const data = await apiClient
                    .get<ProductItemDto>(`/api/products/${id}/detail`)
                    .then((res) => res.data);
                setProductData(data);
                //console.log(data);
            } catch (error) {
                //console.error(error);
                setErrorMessage(t("failedToLoadProductData"));
            } finally {
                setIsLoading(false);
            }
        };

        fetchProductDetail();
    }, [id]);

    if (isLoading) {
        return <div className="container mx-auto mt-4">{t("loading")}</div>;
    }

    if (errorMessage) {
        return <div className="container mx-auto mt-4">{errorMessage}</div>;
    }

    if (!productData) {
        return <div className="container mx-auto mt-4">{t("productNotFound")}</div>;
    }

    const handleSelectLanguage = (language: string) => {
        if (!language) return;
        setSelectedLanguage(language);
    }

    const handleFlipCardImage = () => {
        if (!productData?.isDoubleSided) return;
        const nextSide = side === "front" ? "back" : "front";
        setSide(nextSide);
    };

    const imageCandidates = side === "back"
        ? languageImageCandidates(productData.languageBackImageUrlMap, selectedLanguage, productData.backImageUrlEn)
        : languageImageCandidates(productData.languageImageUrlMap, selectedLanguage, productData.imageUrlEn);

    const renderProductDetailRight = () => {
        switch (productData.productType) {
            case "Cards":
                return (
                    <>
                        <CardProductInfo productItemDto={productData} handleSelectLanguage={handleSelectLanguage} />
                        <GameSalesInfo gameSalesInfo={productData.card?.gameSalesInfo} />
                    </>
                );
            case "SealedProducts":
                return (
                    <>
                        <SealedProductInfo productItemDto={productData} handleSelectLanguage={handleSelectLanguage} />
                        <GameSalesInfo gameSalesInfo={productData.sealedProductInfoDto?.gameSalesInfo} />
                    </>
                );
            case "ManualProducts":
                return <ManualProductInfo productItemDto={productData} />;
            case "Supplies":
                return <SupplyProductInfo productItemDto={productData} />;
            default:
                return null;
        }
    };

    return (
        <div className="container mx-auto flex flex-row gap-4 mt-4">
            <div className="flex flex-col mx-auto gap-2">
                <div className="flex flex-col gap-2">
                    <h1 className="text-2xl font-bold">{t("productInfo")}</h1>
                    <span>{productData.card?.game}</span>
                    <Separator />
                </div>
                <div className="flex flex-row flex-wrap gap-4 mx-auto items-start justify-center">
                    <div id="product-detail-left" className="flex flex-col gap-2 relative">
                        <ProductImage
                            src={imageCandidates[0]}
                            fallbackSrcs={imageCandidates.slice(1)}
                            alt={productData.productNameEn ?? tCommon("productImageAlt")}
                            width={300}
                            height={300}
                        />
                        <PrintTypeLabel
                            printing={productData.card?.printing ?? productData.card?.printType}
                            foilHighlight="badge"
                            className="text-sm absolute bottom-2 right-2"
                        />
                        {productData.isDoubleSided ? (
                            <Button variant="outline" onClick={handleFlipCardImage}>
                                <RefreshCw /> {t("flipCardImage")}
                            </Button>
                        ) : null}

                    </div>
                    <div id="product-detail-right">
                        {renderProductDetailRight()}
                    </div>
                </div>
            </div>
        </div>
    );
}
