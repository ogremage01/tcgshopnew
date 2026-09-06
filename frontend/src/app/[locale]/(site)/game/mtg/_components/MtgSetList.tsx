"use client"
import { useEffect, useMemo, useRef, useState } from "react"
import { api } from "@/lib/api"
import { MtgSetInfoDto } from "@/types/tcgMetadata"
import { Separator } from "@/components/ui/separator"
import { Switch } from "@/components/ui/switch"
import { Label } from "@/components/ui/label"
import { formatDateTime } from "@/utils/date"
import { Button } from "@/components/ui/button"
import Link from "next/link"
import { useTranslations } from "next-intl"
export default function MtgSetList() {
    const groupSectionRefs = useRef<Record<string, HTMLDivElement | null>>({})
    const t = useTranslations("game")
    const tNav = useTranslations("headerNav")
    // 서버에서 전동되는 최초 세트 정보 리스트
    const [mtgSetInfoList, setMtgSetInfoList] = useState<MtgSetInfoDto[]>([])
    // 정렬 기준: 알파벳순, 최신순
    const [sort, setSort] = useState<"alpha" | "releaseDate">("releaseDate")

    // 그룹화된 세트 정보는 useMemo를 사용하여 계산한다.

    useEffect(() => {
        void api.get<MtgSetInfoDto[]>("/api/mtg/sets").then((response) => {
            setMtgSetInfoList(response)

        })
    }, [])

    useEffect(() => {
        if (typeof window === "undefined") return
        if (!window.location.hash) return
        const cleanUrl = `${window.location.pathname}${window.location.search}`
        window.history.replaceState(window.history.state, "", cleanUrl)
    }, [])

    const groupedList = useMemo(() => {
        if (!mtgSetInfoList.length) return []

        // 1. 정렬
        const sorted = [...mtgSetInfoList].sort((a, b) => {
            if (sort === "alpha") {
                return a.name.localeCompare(b.name)
            } else {
                return new Date(b.releaseDate).getTime() - new Date(a.releaseDate).getTime()
            }
        })

        // 2. 그룹핑
        const map = new Map<string, MtgSetInfoDto[]>()

        sorted.forEach(set => {
            const key =
                sort === "alpha"
                    ? getAlphaKey(set.name)
                    : new Date(set.releaseDate).getFullYear().toString()

            if (!map.has(key)) map.set(key, [])
            map.get(key)!.push(set)
        })

        // 3. 렌더용 배열로 변환
        return Array.from(map, ([key, items]) => ({ key, items }))
    }, [mtgSetInfoList, sort])

    // 알파벳 키 계산(대문자 변환 및 숫자 통합)
    function getAlphaKey(name: string): string {
        const first = name.trim()[0]?.toUpperCase()

        if (!first) return "#"

        // 숫자면 통합
        if (/[0-9]/.test(first)) {
            return "0-9"
        }

        // 알파벳
        if (/[A-Z]/.test(first)) {
            return first
        }

        // 기타 문자
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
                    {tNav("games.mtg")} {t("setList.sets")}
                </h2>
                <div className="flex flex-row gap-2 items-center">
                    <Label>{t("setList.sort.latest")}</Label>
                    <Switch checked={sort === "alpha"} onCheckedChange={() => setSort(sort === "alpha" ? "releaseDate" : "alpha")} />
                    <Label>{t("setList.sort.alpha")}</Label>
                </div>
            </div>
            <Separator className="my-4" />

            <div className="mt-4 flex flex-col gap-2">
                {/* 목적 세트로 가는 버튼 묶음 */}
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
                {/* 세트 리스트 */}
                <div className="columns-2 md:columns-3 lg:columns-4 gap-8">
                    {groupedList.map(group => (
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
                                {group.items.map(item => (
                                    <Link key={item.setCode} href={`/game/mtg/${item.setCode}`} className="flex flex-row gap-1 items-center hover:bg-gray-100 rounded-md p-2">
                                        <span className="text-sm font-medium">{item.name}</span>
                                    </Link>
                                ))}
                            </div>
                            <Separator className="my-4" />
                        </div>
                    ))}
                </div>
            </div>
        </div>
    )
}