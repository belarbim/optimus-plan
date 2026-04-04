import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApplicationDTO } from '../models/application.model';

@Injectable({ providedIn: 'root' })
export class ApplicationService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/applications`;

  getAll(): Observable<ApplicationDTO[]> {
    return this.http.get<ApplicationDTO[]>(this.base);
  }

  search(query: string): Observable<ApplicationDTO[]> {
    const params = new HttpParams().set('search', query);
    return this.http.get<ApplicationDTO[]>(this.base, { params });
  }

  getById(id: string): Observable<ApplicationDTO> {
    return this.http.get<ApplicationDTO>(`${this.base}/${id}`);
  }

  create(body: { name: string; description: string | null; teamId: string | null }): Observable<ApplicationDTO> {
    return this.http.post<ApplicationDTO>(this.base, body);
  }

  update(id: string, body: { name: string; description: string | null; teamId: string | null }): Observable<ApplicationDTO> {
    return this.http.put<ApplicationDTO>(`${this.base}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  importCsv(file: File): Observable<{ successCount: number; errorCount: number; errors: string[] }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<{ successCount: number; errorCount: number; errors: string[] }>(
      `${this.base}/import`, form
    );
  }
}
