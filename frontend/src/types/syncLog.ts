export type SyncLogDto = {
    id: number
    syncSource: string
    syncTarget: string
    startTime: Date
    endTime: Date
    proceedingTime: number
    result: string
    message: string
}
