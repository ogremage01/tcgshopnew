"use client"

import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import { api } from "@/lib/api"
import { TcgPSetInfoDto } from "@/types/tcgMetadata"
import GameSetListItem from "./GameSetListItem"
import { Separator } from "@/components/ui/separator"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Button } from "@/components/ui/button"
import { useTranslations } from "next-intl"
import { usePathname } from "@/i18n/navigation"
import { BFCACHE_RESTORE_EVENT } from "@/app/_components/DisableBFCache"

type TcgpSetListProps = {
    productLineId: number
    game: string
    productLineName: string
    /** SSR로 내려준 초기 목록 — 뒤로가기 시 빈 클라이언트 상태 복원 방지 */
    initialSets?: TcgPSetInfoDto[]
}

export default function TcgpSetList({
    productLineId,
    game,
    productLineName,
    initialSets = [],
}: TcgpSetListProps) {
    const t = useTranslations()
    const pathname = usePathname()
    const groupSectionRefs = useRef<Record<string, HTMLDivElement | null>>({})

    const [tcgpSetInfoList, setTcgpSetInfoList] = useState<TcgPSetInfoDto[]>(initialSets)
    const [sort, setSort] = useState<"alpha" | "releaseDate">("releaseDate")

    const fetchSets = useCallback(async () => {
        try {
            const response = await api.get<TcgPSetInfoDto[]>(`/api/game/sets/${productLineId}`)
            setTcgpSetInfoList(Array.isArray(response) ? response : [])
        } catch {
            // 네트워크 오류 시 기존 목록 유지
        }
    }, [productLineId])

    useEffect(() => {
        if (initialSets.length > 0) {
            setTcgpSetInfoList(initialSets)
        }
    }, [initialSets])

    useEffect(() => {
        void fetchSets()
    }, [pathname, fetchSets])

    useEffect(() => {
        const onPopState = () => {
            void fetchSets()
        }
        const onBfcacheRestore = () => {
            void fetchSets()
        }
        window.addEventListener("popstate", onPopState)
        window.addEventListener(BFCACHE_RESTORE_EVENT, onBfcacheRestore)
        return () => {
            window.removeEventListener("popstate", onPopState)
            window.removeEventListener(BFCACHE_RESTORE_EVENT, onBfcacheRestore)
        }
    }, [fetchSets])

    useEffect(() => {
        if (typeof window === "undefined") return
        if (!window.location.hash) return
        const cleanUrl = `${window.location.pathname}${window.location.search}`
        window.history.replaceState(window.history.state, "", cleanUrl)
    }, [])

    const groupedList = useMemo(() => {
        if (!tcgpSetInfoList.length) return []

        const sorted = [...tcgpSetInfoList].sort((a, b) => {
            if (sort === "alpha") {
                return a.setName.localeCompare(b.setName)
            }
            return new Date(b.releaseDate ?? "").getTime() - new Date(a.releaseDate ?? "").getTime()
        })

        const map = new Map<string, TcgPSetInfoDto[]>()

        sorted.forEach((set: TcgPSetInfoDto) => {
            const key =
                sort === "alpha"
                    ? getAlphaKey(set.setName)
                    : new Date(set.releaseDate ?? "").getFullYear().toString()

            if (!map.has(key)) map.set(key, [])
            map.get(key)!.push(set)
        })

        return Array.from(map, ([key, items]) => ({ key, items }))
    }, [tcgpSetInfoList, sort])

    function getAlphaKey(name: string): string {
        const first = name.trim()[0]?.toUpperCase()

        if (!first) return "#"

        if (/[0-9]/.test(first)) {
            return "0-9"
        }

        if (/[A-Z]/.test(first)) {
            return first
        }

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
                <h2 className="text-2xl font-bold">{productLineName} {t("game.setList.sets")}</h2>
                <div className="flex flex-row gap-2 items-center">
                    <Label>{t("game.setList.sort.latest")}</Label>
                    <Switch checked={sort === "alpha"} onCheckedChange={() => setSort(sort === "alpha" ? "releaseDate" : "alpha")} />
                    <Label>{t("game.setList.sort.alpha")}</Label>
                </div>
            </div>
            <Separator className="my-4" />

            <div className="mt-4 flex flex-col gap-2">
                <div className="flex flex-row flex-wrap gap-2 items-center justify-start">
                    {groupedList.map(group => (
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
            </div>
            <div className="columns-2 md:columns-3 lg:columns-4 gap-8">
                {groupedList.map(group => (
                    <div id={getGroupAnchorId(group.key)} key={group.key} ref={(element) => {
                        groupSectionRefs.current[group.key] = element
                    }} className="break-inside-avoid mb-6 scroll-mt-24">
                        <div className="font-bold mb-2">
                            {group.key}
                        </div>
                        <div className="space-y-1">
                            {group.items.map((set: TcgPSetInfoDto) => (
                                <GameSetListItem key={set.setName} productLineId={productLineId} set={set} game={game} />
                            ))}
                        </div>
                        <Separator className="my-4" />
                    </div>
                ))}
            </div>
        </div>
    )
}
