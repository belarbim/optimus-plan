export interface PublicHolidayDTO {
  id?: string;
  date: string;
  name: string;
  recurring: boolean;
}

export interface CreatePublicHolidayRequest {
  date: string;
  name: string;
  recurring: boolean;
}
