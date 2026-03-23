import { TeamAssignmentDTO } from './assignment.model';

export interface EmployeeDTO {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  totalAllocation: number;
  assignments: TeamAssignmentDTO[];
  createdAt: string;
}

export interface CreateEmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
}

export interface UpdateEmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
}

export interface ImportRowError {
  row: number;
  email: string;
  reason: string;
}

export interface ImportResult {
  imported: number;
  skipped: number;
  errors: ImportRowError[];
}
