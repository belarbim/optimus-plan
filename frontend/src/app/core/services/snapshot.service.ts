import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CapacitySnapshotDTO } from '../models/snapshot.model';

@Injectable({ providedIn: 'root' })
export class SnapshotService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/snapshots`;

  generateSnapshot(teamId: string, month: string): Observable<CapacitySnapshotDTO[]> {
    return this.http.post<CapacitySnapshotDTO[]>(`${this.base}/generate`, null, {
      params: { teamId, month },
    });
  }

  generateAll(month: string): Observable<void> {
    return this.http.post<void>(`${this.base}/generate-all`, null, {
      params: { month },
    });
  }

  getByTeam(teamId: string, from: string, to: string): Observable<CapacitySnapshotDTO[]> {
    return this.http.get<CapacitySnapshotDTO[]>(`${this.base}/team/${teamId}`, {
      params: { from, to },
    });
  }
}
