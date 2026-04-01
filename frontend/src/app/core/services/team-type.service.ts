import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TeamTypeDTO, TeamTypeRequest } from '../models/team-type.model';

@Injectable({ providedIn: 'root' })
export class TeamTypeService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/team-types`;

  getAll(): Observable<TeamTypeDTO[]> {
    return this.http.get<TeamTypeDTO[]>(this.base);
  }

  create(body: TeamTypeRequest): Observable<TeamTypeDTO> {
    return this.http.post<TeamTypeDTO>(this.base, body);
  }

  update(id: string, body: TeamTypeRequest): Observable<TeamTypeDTO> {
    return this.http.put<TeamTypeDTO>(`${this.base}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
