import Link from "next/link"
import { TcgPSetInfoDto } from "@/types/tcgMetadata"

export default function GameSetListItem({ productLineId, set, game }: { productLineId: number, set: TcgPSetInfoDto, game: string }) {
    return (
        <Link key={set.setName} href={`/game/${game}/${set.urlName}`} className="flex flex-row gap-1 items-center hover:bg-gray-100 rounded-md p-2">
            <span className="text-sm font-medium">{set.setName}</span>
        </Link>
    )
}