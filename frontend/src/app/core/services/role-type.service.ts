import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RoleTypeConfigDTO, CreateRoleTypeRequest } from '../models/role-type.model';

@Injectable({ providedIn: 'root' })
export class RoleTypeService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/role-types`;

  getRoleTypes(): Observable<RoleTypeConfigDTO[]> {
    return this.http.get<RoleTypeConfigDTO[]>(this.base);
  }

  getRoleType(id: string): Observable<RoleTypeConfigDTO> {
    return this.http.get<RoleTypeConfigDTO>(`${this.base}/${id}`);
  }

  createRoleType(body: CreateRoleTypeRequest): Observable<RoleTypeConfigDTO> {
    return this.http.post<RoleTypeConfigDTO>(this.base, body);
  }

  updateRoleType(id: string, body: CreateRoleTypeRequest): Observable<RoleTypeConfigDTO> {
    return this.http.put<RoleTypeConfigDTO>(`${this.base}/${id}`, body);
  }

  deleteRoleType(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
