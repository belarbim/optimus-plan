import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TeamDTO, CreateTeamRequest, UpdateTeamRequest } from '../models/team.model';

@Injectable({ providedIn: 'root' })
export class TeamService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/teams`;

  getTeams(tree = false): Observable<TeamDTO[]> {
    return this.http.get<TeamDTO[]>(this.base, { params: { tree: String(tree) } });
  }

  getTeam(id: string): Observable<TeamDTO> {
    return this.http.get<TeamDTO>(`${this.base}/${id}`);
  }

  createTeam(body: CreateTeamRequest): Observable<TeamDTO> {
    return this.http.post<TeamDTO>(this.base, body);
  }

  updateTeam(id: string, body: UpdateTeamRequest): Observable<TeamDTO> {
    return this.http.put<TeamDTO>(`${this.base}/${id}`, body);
  }

  deleteTeam(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  getCategories(teamId: string): Observable<import('../models/snapshot.model').CategoryAllocationDTO[]> {
    return this.http.get<import('../models/snapshot.model').CategoryAllocationDTO[]>(`${this.base}/${teamId}/categories`);
  }

  updateCategories(teamId: string, body: import('../models/snapshot.model').UpdateCategoriesRequest): Observable<import('../models/snapshot.model').CategoryAllocationDTO[]> {
    return this.http.put<import('../models/snapshot.model').CategoryAllocationDTO[]>(`${this.base}/${teamId}/categories`, body);
  }

  importCsv(file: File): Observable<{ successCount: number; errorCount: number; errors: string[] }> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<{ successCount: number; errorCount: number; errors: string[] }>(`${this.base}/import`, fd);
  }
}
