import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GradeHistoryDTO, CostHistoryDTO } from '../models/grade.model';
import { EmployeeTypeHistoryDTO } from '../models/employee.model';

@Injectable({ providedIn: 'root' })
export class EmployeeCostService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/employees`;

  getGradeHistory(employeeId: string): Observable<GradeHistoryDTO[]> {
    return this.http.get<GradeHistoryDTO[]>(`${this.base}/${employeeId}/grade-history`);
  }
  addGradeHistory(employeeId: string, body: { gradeId: string; effectiveFrom: string }): Observable<GradeHistoryDTO> {
    return this.http.post<GradeHistoryDTO>(`${this.base}/${employeeId}/grade-history`, body);
  }
  getCurrentGrade(employeeId: string): Observable<GradeHistoryDTO | null> {
    return this.http.get<GradeHistoryDTO | null>(`${this.base}/${employeeId}/grade-history/current`);
  }

  getCostHistory(employeeId: string): Observable<CostHistoryDTO[]> {
    return this.http.get<CostHistoryDTO[]>(`${this.base}/${employeeId}/cost-history`);
  }
  addCostHistory(employeeId: string, body: { dailyCost: number; effectiveFrom: string }): Observable<CostHistoryDTO> {
    return this.http.post<CostHistoryDTO>(`${this.base}/${employeeId}/cost-history`, body);
  }
  getCurrentCost(employeeId: string): Observable<CostHistoryDTO | null> {
    return this.http.get<CostHistoryDTO | null>(`${this.base}/${employeeId}/cost-history/current`);
  }

  getTypeHistory(employeeId: string): Observable<EmployeeTypeHistoryDTO[]> {
    return this.http.get<EmployeeTypeHistoryDTO[]>(`${this.base}/${employeeId}/type-history`);
  }
  addTypeHistory(employeeId: string, body: { type: string; effectiveFrom: string }): Observable<EmployeeTypeHistoryDTO> {
    return this.http.post<EmployeeTypeHistoryDTO>(`${this.base}/${employeeId}/type-history`, body);
  }
  updateTypeHistory(employeeId: string, entryId: string, body: { type: string; effectiveFrom: string }): Observable<EmployeeTypeHistoryDTO> {
    return this.http.put<EmployeeTypeHistoryDTO>(`${this.base}/${employeeId}/type-history/${entryId}`, body);
  }
  deleteTypeHistory(employeeId: string, entryId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${employeeId}/type-history/${entryId}`);
  }

  updateGradeHistory(employeeId: string, entryId: string, body: { gradeId: string; effectiveFrom: string }): Observable<GradeHistoryDTO> {
    return this.http.put<GradeHistoryDTO>(`${this.base}/${employeeId}/grade-history/${entryId}`, body);
  }
  deleteGradeHistory(employeeId: string, entryId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${employeeId}/grade-history/${entryId}`);
  }

  updateCostHistory(employeeId: string, entryId: string, body: { dailyCost: number; effectiveFrom: string }): Observable<CostHistoryDTO> {
    return this.http.put<CostHistoryDTO>(`${this.base}/${employeeId}/cost-history/${entryId}`, body);
  }
  deleteCostHistory(employeeId: string, entryId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${employeeId}/cost-history/${entryId}`);
  }
}
