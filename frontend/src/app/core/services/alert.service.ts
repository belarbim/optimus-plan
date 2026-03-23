import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CapacityAlertDTO, CreateAlertRequest } from '../models/alert.model';

@Injectable({ providedIn: 'root' })
export class AlertService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/alerts`;

  createAlert(body: CreateAlertRequest): Observable<CapacityAlertDTO> {
    return this.http.post<CapacityAlertDTO>(this.base, body);
  }

  getByTeam(teamId: string): Observable<CapacityAlertDTO> {
    return this.http.get<CapacityAlertDTO>(`${this.base}/team/${teamId}`);
  }

  deleteAlert(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
