export interface BannerDto {
    id: number
    /** 배너 타겟: 어떤 페이지에 걸릴지 */
    target: string
    imageUrl: string
    link: string
    title?: string
    displayOrder?: number
}

export interface AddBannerDto {
    imageFile: File
    link?: string
    title?: string
}

export interface ChangeBannerOrderDto {
    orderedIds: number[]
}

export interface SetBannerDto {
    game: string
    bannerId: string
    imageUrl: string
    link: string
    title?: string
    active?: boolean
}
