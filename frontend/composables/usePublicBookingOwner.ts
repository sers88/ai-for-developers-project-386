export interface PublicBookingOwner {
  name?: string
}

export const usePublicBookingOwner = () =>
  useState<PublicBookingOwner | null>("public-booking-owner", () => null)
