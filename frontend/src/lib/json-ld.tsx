import type { Product } from "@/types/product";
import { resolveAssetUrl } from "@/lib/public-asset-url";

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "";

export function JsonLdProduct({ product }: { product: Product }) {
  const schema = {
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.name,
    description: product.description ?? product.name,
    image: resolveAssetUrl(product.imageUrl),
    offers: {
      "@type": "Offer",
      price: product.price,
      priceCurrency: "KRW",
    },
  };
  return (
    <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(schema) }} />
  );
}

export function JsonLdItemList(products: Product[], listName: string) {
  const schema = {
    "@context": "https://schema.org",
    "@type": "ItemList",
    name: listName,
    numberOfItems: products.length,
    itemListElement: products.map((p, i) => ({
      "@type": "ListItem",
      position: i + 1,
      item: {
        "@type": "Product",
        name: p.name,
        url: `${siteUrl}/products/${p.id}`,
      },
    })),
  };
  return (
    <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(schema) }} />
  );
}
