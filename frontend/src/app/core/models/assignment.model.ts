export interface TeamAssignmentDTO {
  id: string;
  teamId: string;
  teamName: string;
  employeeId: string;
  employeeName: string;
  allocationPct: number;
  roleType: string;
  roleWeight: number;
  startDate: string;
  endDate?: string;
}

export interface RoleHistoryDTO {
  id: string;
  assignmentId: string;
  roleType: string;
  roleWeight: number;
  effectiveFrom: string;
  effectiveTo?: string;
}

export interface CreateAssignmentRequest {
  teamId: string;
  employeeId: string;
  allocationPct: number;
  roleType: string;
  roleWeight: number;
  startDate: string;
}

export interface UpdateAllocationRequest {
  allocationPct: number;
}

export interface UpdateRoleRequest {
  roleType: string;
  roleWeight: number;
  effectiveFrom: string;
}
