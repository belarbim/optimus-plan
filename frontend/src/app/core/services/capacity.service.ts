import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CapacityResultDTO,
  RemainingCapacityDTO,
  RollupCapacityDTO,
  SimulationRequest,
  SimulationResultDTO,
} from '../models/capacity.model';

@Injectable({ providedIn: 'root' })
export class CapacityService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/capacity`;

  getTeamCapacity(teamId: string, month: string): Observable<CapacityResultDTO> {
    return this.http.get<CapacityResultDTO>(`${this.base}/team/${teamId}`, {
      params: { month },
    });
  }

  getRemainingCapacity(teamId: string, date: string): Observable<RemainingCapacityDTO> {
    return this.http.get<RemainingCapacityDTO>(`${this.base}/team/${teamId}/remaining`, {
      params: { date },
    });
  }

  getRollupCapacity(teamId: string, month: string): Observable<RollupCapacityDTO> {
    return this.http.get<RollupCapacityDTO>(`${this.base}/team/${teamId}/rollup`, {
      params: { month },
    });
  }

  simulate(teamId: string, body: SimulationRequest): Observable<SimulationResultDTO> {
    return this.http.post<SimulationResultDTO>(`${this.base}/team/${teamId}/simulate`, body);
  }

  getCapacityBulk(teamId: string, months: string[]): Observable<CapacityResultDTO[]> {
    return forkJoin(months.map(m => this.getTeamCapacity(teamId, m)));
  }
}
