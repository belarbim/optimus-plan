import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { WorkingDaysConfigDTO } from '../models/working-days.model';

@Injectable({ providedIn: 'root' })
export class WorkingDaysService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/working-days`;

  getWorkingDays(): Observable<WorkingDaysConfigDTO[]> {
    return this.http.get<WorkingDaysConfigDTO[]>(this.base);
  }

  importCsv(file: File): Observable<WorkingDaysConfigDTO[]> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<WorkingDaysConfigDTO[]>(`${this.base}/import`, formData);
  }
}
