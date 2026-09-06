import Image from "next/image"
import { resolveAssetUrl } from "@/lib/public-asset-url"
import type { MainPageContentDto } from "@/types/mainPageContent"
import Link from "next/link"
function extractTextPreview(html: string): string {
  const stripped = html.replace(/<[^>]*>/g, " ").replace(/\s+/g, " ").trim()
  return stripped.slice(0, 120)
}

type ContentBlockProps = {
  item: MainPageContentDto
}

export function ContentBlock({ item }: ContentBlockProps) {
  if (!item.content) return null

  const preview = extractTextPreview(item.content)
  const hasLink = !!item.link

  const inner = (
    <article className="group flex flex-col overflow-hidden rounded-xl border bg-card shadow-sm transition-shadow hover:shadow-md h-full">
      {item.imageUrl && (
        <div className="relative aspect-video w-full overflow-hidden bg-muted">
          <Image
            src={resolveAssetUrl(item.imageUrl)}
            alt={item.name}
            fill
            className="object-cover transition-transform duration-300 group-hover:scale-105"
          />
        </div>
      )}
      <div className="flex flex-col gap-2 p-4">
        <h2 className="text-base font-semibold leading-snug line-clamp-2">{item.name}</h2>
        {preview && (
          <p className="text-sm text-muted-foreground leading-relaxed line-clamp-3">{preview}</p>
        )}
      </div>
    </article>
  )

  if (hasLink) {
    return (
      <Link href={item.link!} target="_blank" rel="noopener noreferrer" className="block">
        {inner}
      </Link>
    )
  }

  return inner
}
