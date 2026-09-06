"use client"

import Image, { type ImageProps } from "next/image"
import { useMemo, useState } from "react"
import {
  defaultCardImageUrl,
  resolveProductImageSrc,
} from "@/lib/public-asset-url"

type ProductImageProps = Omit<ImageProps, "src" | "onError"> & {
  src?: string | null
  fallbackSrcs?: Array<string | null | undefined>
  onError?: ImageProps["onError"]
}

/** 상품 이미지 — URL 없음·로드 실패 시 card-images/default-card.png */
export default function ProductImage({
  src,
  fallbackSrcs = [],
  alt,
  onError,
  unoptimized = true,
  className,
  ...props
}: ProductImageProps) {
  const candidates = useMemo(
    () => [src, ...fallbackSrcs, defaultCardImageUrl()]
      .map((value) => resolveProductImageSrc(value))
      .filter((value, index, all) => value && all.indexOf(value) === index),
    [src, fallbackSrcs],
  )
  const candidateKey = candidates.join("\u0000")
  const [fallbackState, setFallbackState] = useState({ key: candidateKey, index: 0 })
  const candidateIndex = fallbackState.key === candidateKey ? fallbackState.index : 0

  return (
    <Image
      {...props}
      className={className}
      alt={alt}
      src={candidates[candidateIndex] ?? defaultCardImageUrl()}
      unoptimized={unoptimized}
      onError={(e) => {
        setFallbackState((current) => ({
          key: candidateKey,
          index: Math.min((current.key === candidateKey ? current.index : 0) + 1, candidates.length - 1),
        }))
        onError?.(e)
      }}
    />
  )
}
