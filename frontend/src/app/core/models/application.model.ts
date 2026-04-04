export interface ApplicationDTO {
  id: string;
  name: string;
  description: string | null;
  teamId: string | null;
  teamName: string | null;
  createdAt: string;
}
