import { http } from './http'
import type { AddressRequest, AddressResponse } from './types/addresses'

const ADDRESSES_PATH = '/api/addresses'
const withCredentials = { withCredentials: true }

export const getMyAddresses = async (): Promise<AddressResponse[]> => {
  const response = await http.get<AddressResponse[]>(ADDRESSES_PATH, withCredentials)
  return response.data
}

export const createAddress = async (payload: AddressRequest): Promise<AddressResponse> => {
  const response = await http.post<AddressResponse>(ADDRESSES_PATH, payload, withCredentials)
  return response.data
}

export const updateAddress = async (
  addressId: number,
  payload: AddressRequest,
): Promise<AddressResponse> => {
  const response = await http.put<AddressResponse>(
    `${ADDRESSES_PATH}/${addressId}`,
    payload,
    withCredentials,
  )
  return response.data
}

export const deleteAddress = async (addressId: number): Promise<void> => {
  await http.delete(`${ADDRESSES_PATH}/${addressId}`, withCredentials)
}
