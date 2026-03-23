import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EmployeeDTO, CreateEmployeeRequest, UpdateEmployeeRequest, ImportResult } from '../models/employee.model';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/employees`;

  getEmployees(): Observable<EmployeeDTO[]> {
    return this.http.get<EmployeeDTO[]>(this.base);
  }

  getEmployee(id: string): Observable<EmployeeDTO> {
    return this.http.get<EmployeeDTO>(`${this.base}/${id}`);
  }

  createEmployee(body: CreateEmployeeRequest): Observable<EmployeeDTO> {
    return this.http.post<EmployeeDTO>(this.base, body);
  }

  updateEmployee(id: string, body: UpdateEmployeeRequest): Observable<EmployeeDTO> {
    return this.http.put<EmployeeDTO>(`${this.base}/${id}`, body);
  }

  deleteEmployee(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  importEmployees(file: File): Observable<ImportResult> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ImportResult>(`${this.base}/import`, form);
  }
}
