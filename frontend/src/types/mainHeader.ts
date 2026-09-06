export type MainHeaderDto = {
  id: number
  title: string
  urlString: string
  isActive: boolean
  displayOrder: number
}

export type SaveMainHeaderDto = {
  title: string
  urlString: string
  isActive?: boolean
}

export type ChangeMainHeaderOrderDto = {
  orderedIds: number[]
}
