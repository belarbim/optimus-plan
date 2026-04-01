export interface TeamDTO {
  id: string;
  name: string;
  parentId?: string;
  teamTypeId?: string;
  children?: TeamDTO[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeamRequest {
  name: string;
  parentId?: string;
  teamTypeId?: string;
}

export interface UpdateTeamRequest {
  name: string;
  parentId?: string;
}
