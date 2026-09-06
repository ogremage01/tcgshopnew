export interface Page<T> {
    content: T[]

    pageNumber: number   // 0-based
    pageSize: number
    offset: number

    paged: boolean
    unpaged: boolean

    totalElements: number
    totalPages: number

    last: boolean
    first: boolean

    size: number
    number: number // 현재 페이지 (0-based)

    sort: Sort

    numberOfElements: number
    empty: boolean
}

export interface Sort {
    empty: boolean
    sorted: boolean
    unsorted: boolean
}

/** 요청용 페이지 파라미터. sort는 Spring 요청 sort 와 동일 (예: "paymentDate,desc", "id,asc"). */
export interface PageParam {
    page: number;
    size: number;
    sort: string[];
}
