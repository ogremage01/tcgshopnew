"use client"

import { useState } from "react"
import type { UnionPriceSlimDto } from "@/types/product"

export function useSelectedSearchCard() {
    const [selectedSearchCard, setSelectedSearchCard] = useState<UnionPriceSlimDto | null>(null)
    return { selectedSearchCard, setSelectedSearchCard }
}
