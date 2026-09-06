export type MainPageContentDto = {
  id: number
  name: string
  content: string
  imageUrl: string | null
  link: string | null
  displayOrder: number
}

export type SaveMainPageContentDto = {
  name: string
  content: string
  link: string
}

export type ChangeMainPageContentOrderDto = {
  orderedIds: number[]
}
