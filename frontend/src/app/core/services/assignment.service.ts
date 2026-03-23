import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  TeamAssignmentDTO,
  RoleHistoryDTO,
  CreateAssignmentRequest,
  UpdateAllocationRequest,
  UpdateRoleRequest,
} from '../models/assignment.model';

@Injectable({ providedIn: 'root' })
export class AssignmentService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/assignments`;

  createAssignment(body: CreateAssignmentRequest): Observable<TeamAssignmentDTO> {
    return this.http.post<TeamAssignmentDTO>(this.base, body);
  }

  endAssignment(id: string, endDate: string): Observable<TeamAssignmentDTO> {
    return this.http.put<TeamAssignmentDTO>(`${this.base}/${id}/end`, null, {
      params: { endDate },
    });
  }

  updateAllocation(id: string, body: UpdateAllocationRequest): Observable<TeamAssignmentDTO> {
    return this.http.put<TeamAssignmentDTO>(`${this.base}/${id}/allocation`, body);
  }

  updateRole(id: string, body: UpdateRoleRequest): Observable<RoleHistoryDTO> {
    return this.http.put<RoleHistoryDTO>(`${this.base}/${id}/role`, body);
  }

  getByTeam(teamId: string): Observable<TeamAssignmentDTO[]> {
    return this.http.get<TeamAssignmentDTO[]>(`${this.base}/team/${teamId}`);
  }

  getByEmployee(employeeId: string): Observable<TeamAssignmentDTO[]> {
    return this.http.get<TeamAssignmentDTO[]>(`${this.base}/employee/${employeeId}`);
  }

  getRoleHistory(id: string): Observable<RoleHistoryDTO[]> {
    return this.http.get<RoleHistoryDTO[]>(`${this.base}/${id}/role-history`);
  }
}
