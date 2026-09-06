"use client"

import { useRef } from "react"
import { useVirtualizer } from "@tanstack/react-virtual"
import { type MtgSetInfoDto, type TcgPSetInfoDto } from "@/types/tcgMetadata"
import { SelectItem } from "@/components/ui/select"

const ITEM_HEIGHT = 36

type SetDto = TcgPSetInfoDto | MtgSetInfoDto

export const VitualizedList = ({ setList }: { setList: SetDto[] }) => {
    const parentRef = useRef<HTMLDivElement>(null)

    const rowVirtualizer = useVirtualizer({
        count: setList.length,
        getScrollElement: () => parentRef.current,
        estimateSize: () => ITEM_HEIGHT,
        overscan: 5,
    })

    return (
        <div ref={parentRef} className="max-h-80 overflow-auto">
            <div
                style={{
                    height: `${rowVirtualizer.getTotalSize()}px`,
                    position: "relative",
                }}
            >
                {rowVirtualizer.getVirtualItems().map((virtualRow) => {
                    const item = setList[virtualRow.index]

                    const label = "name" in item ? item.name : item.setName
                    const code = item.setCode

                    /** SelectItem 바깥에 래퍼 div를 두면 Radix Collect가 동작이 어긋날 수 있어, 각 행은 Item 하나만 둔다. */
                    return (
                        <SelectItem
                            key={code}
                            value={code}
                            textValue={`${code} ${label ?? ""}`}
                            className="absolute left-0 right-0 w-full"
                            style={{
                                top: virtualRow.start,
                                height: virtualRow.size,
                            }}
                        >
                            {code} - {label}
                        </SelectItem>
                    )
                })}
            </div>
        </div>
    )
}
