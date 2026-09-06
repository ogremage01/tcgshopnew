"use client"

import { useMemo, useRef, useState } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import { Switch } from "@/components/ui/switch"
import { FabSetInfoDto } from "@/types/tcgMetadata"

type FabSetListProps = {
    initialSets?: FabSetInfoDto[]
}

export default function FabSetList({ initialSets = [] }: FabSetListProps) {
    const t = useTranslations();
    const tNav = useTranslations("headerNav");
    const groupSectionRefs = useRef<Record<string, HTMLDivElement | null>>({})
    const [sort, setSort] = useState<"alpha" | "porder">("porder")

    const sortedSets = useMemo(() => {
        return [...initialSets].sort((a, b) => {
            if (sort === "alpha") {
                return a.name.localeCompare(b.name)
            }

            const orderDiff = (b.porder ?? 0) - (a.porder ?? 0)
            return orderDiff || a.name.localeCompare(b.name)
        })
    }, [initialSets, sort])

    const groupedList = useMemo(() => {
        if (sort !== "alpha") return []

        const map = new Map<string, FabSetInfoDto[]>()

        sortedSets.forEach((set) => {
            const key = getAlphaKey(set.name)
            if (!map.has(key)) map.set(key, [])
            map.get(key)!.push(set)
        })

        return Array.from(map, ([key, items]) => ({ key, items }))
    }, [sortedSets, sort])

    function getAlphaKey(name: string): string {
        const first = name.trim()[0]?.toUpperCase()

        if (!first) return "#"
        if (/[0-9]/.test(first)) return "0-9"
        if (/[A-Z]/.test(first)) return first
        return "#"
    }

    const getGroupAnchorId = (key: string): string => {
        if (key === "#") return "group-special"
        if (key === "0-9") return "group-0-9"
        return `group-${key.toLowerCase()}`
    }

    const scrollToGroup = (key: string) => {
        const target = groupSectionRefs.current[key]
        if (!target) return
        target.scrollIntoView({ behavior: "smooth", block: "start" })
    }

    return (
        <div>
            <Separator className="my-4" />
            <div className="flex flex-row gap-2 items-center justify-between">
                <h2 className="text-2xl font-bold">
                    {tNav("games.fab")} {t("game.setList.sets")}
                </h2>
                <div className="flex flex-row gap-2 items-center">
                    <Label>{t("game.setList.sort.latest")}</Label>
                    <Switch
                        checked={sort === "alpha"}
                        onCheckedChange={() => setSort(sort === "alpha" ? "porder" : "alpha")}
                    />
                    <Label>{t("game.setList.sort.alpha")}</Label>
                </div>
            </div>
            <Separator className="my-4" />

            <div className="mt-4 flex flex-col gap-2">
                {sort === "alpha" && (
                    <div className="flex flex-row flex-wrap gap-2 items-center justify-start">
                        {groupedList.map((group) => (
                            <Button
                                key={group.key}
                                variant="outline"
                                className="text-sm font-medium hover:bg-gray-400"
                                onClick={() => scrollToGroup(group.key)}
                            >
                                {group.key}
                            </Button>
                        ))}
                        <Separator className="w-full" />
                    </div>
                )}
                <div className="columns-2 md:columns-3 lg:columns-4 gap-8">
                    {sort === "alpha"
                        ? groupedList.map((group) => (
                            <div
                                id={getGroupAnchorId(group.key)}
                                key={group.key}
                                ref={(element) => {
                                    groupSectionRefs.current[group.key] = element
                                }}
                                className="break-inside-avoid mb-6 scroll-mt-24"
                            >
                                <div className="font-bold mb-2">
                                    {group.key}
                                </div>
                                <div className="space-y-1">
                                    {group.items.map((set) => (
                                        <Link
                                            key={set.setCode}
                                            href={`/game/fab/${encodeURIComponent(set.setCode)}`}
                                            className="flex flex-row gap-1 items-center rounded-md p-2 hover:bg-gray-100"
                                        >
                                            <span className="text-sm font-medium">{set.name}</span>
                                        </Link>
                                    ))}
                                </div>
                                <Separator className="my-4" />
                            </div>
                        ))
                        : sortedSets.map((set) => (
                            <Link
                                key={set.setCode}
                                href={`/game/fab/${encodeURIComponent(set.setCode)}`}
                                className="mb-2 flex break-inside-avoid flex-row gap-1 items-center rounded-md p-2 hover:bg-gray-100"
                            >
                                <span className="text-sm font-medium">{set.name}</span>
                            </Link>
                        ))}
                </div>
            </div>
        </div>
    )
}
