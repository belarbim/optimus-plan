export interface PublicHolidayDTO {
  id?: string;
  date: string;
  name: string;
  locale: string;
  recurring: boolean;
}

export interface CreatePublicHolidayRequest {
  date: string;
  name: string;
  locale: string;
  recurring: boolean;
}
