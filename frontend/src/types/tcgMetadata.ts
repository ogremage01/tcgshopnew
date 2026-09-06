export interface TcgPSetInfoDto {
    setCode: string
    setName: string
    urlName: string
    releaseDate: string
}

export interface TcgPProductLineDto {
    productLineId: number
    productLineName: string
}

export interface MtgSetInfoDto {
    setCode: string
    name: string
    nameK: string
    type: string
    releaseDate: string
}

export interface FabSetInfoDto {
    setCode: string
    name: string
    porder: number
}
