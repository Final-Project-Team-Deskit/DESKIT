export type AddressResponse = {
  address_id: number
  receiver: string
  postcode: string
  addr_detail: string
  is_default: boolean
}

export type AddressRequest = {
  receiver: string
  postcode: string
  addr_detail: string
  is_default: boolean
}
