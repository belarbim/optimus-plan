export interface RoleTypeConfigDTO {
  id?: string;
  roleType: string;
  defaultWeight: number;
  description: string;
}

export interface CreateRoleTypeRequest {
  roleType: string;
  defaultWeight: number;
  description: string;
}
