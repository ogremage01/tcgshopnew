import Image from "next/image"
import { Link } from "@/i18n/navigation"
import { resolveAssetUrl } from "@/lib/public-asset-url"
import type { SetBannerDto } from "@/types/banner"

type SetBannerSectionProps = {
    banner: SetBannerDto | null
}

export function SetBannerSection({ banner }: SetBannerSectionProps) {
    if (!banner?.imageUrl) {
        return null
    }

    const image = (
        <div className="relative w-full min-h-40">
            <Image
                src={resolveAssetUrl(banner.imageUrl)}
                alt={banner.title ?? "set banner"}
                fill
                className="object-contain"
                sizes="(max-width: 768px) 100vw, 1200px"
                priority
            />
        </div>
    )

    if (banner.link) {
        return (
            <Link href={banner.link} className="block w-full">
                {image}
            </Link>
        )
    }

    return image
}
