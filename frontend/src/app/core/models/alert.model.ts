export interface CapacityAlertDTO {
  id?: string;
  teamId: string;
  thresholdManDays: number;
  enabled: boolean;
}

export interface CreateAlertRequest {
  teamId: string;
  thresholdManDays: number;
  enabled: boolean;
}
