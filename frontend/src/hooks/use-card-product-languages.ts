"use client"

import { useEffect, useState } from "react"
import { api } from "@/lib/api"
import type { CardProductLanguageDto } from "@/types/product"
import { compareLanguageCode, FALLBACK_CARD_PRODUCT_LANGUAGES } from "@/lib/card-product-language"

export function useCardProductLanguages() {
  const [languages, setLanguages] = useState<CardProductLanguageDto[]>(FALLBACK_CARD_PRODUCT_LANGUAGES)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    let mounted = true
    api.get<CardProductLanguageDto[]>("/api/admin/product/card-product-languages")
      .then((response) => {
        if (!mounted) return
        setLanguages([...response].sort((a, b) => compareLanguageCode(a.code, b.code)))
      })
      .catch(() => {
        if (!mounted) return
        setLanguages(FALLBACK_CARD_PRODUCT_LANGUAGES)
      })
      .finally(() => {
        if (mounted) setLoading(false)
      })
    return () => {
      mounted = false
    }
  }, [])

  return { languages, loading }
}
