import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GradeDTO, GradeCostHistoryDTO } from '../models/grade.model';

@Injectable({ providedIn: 'root' })
export class GradeService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/grades`;

  getAll(): Observable<GradeDTO[]> { return this.http.get<GradeDTO[]>(this.base); }
  create(body: { name: string; dailyCost: number; effectiveFrom?: string | null }): Observable<GradeDTO> { return this.http.post<GradeDTO>(this.base, body); }
  update(id: string, body: { name: string }): Observable<GradeDTO> { return this.http.put<GradeDTO>(`${this.base}/${id}`, body); }
  delete(id: string): Observable<void> { return this.http.delete<void>(`${this.base}/${id}`); }
  getCostHistory(id: string): Observable<GradeCostHistoryDTO[]> { return this.http.get<GradeCostHistoryDTO[]>(`${this.base}/${id}/cost-history`); }
  addCostHistory(id: string, body: { dailyCost: number; effectiveFrom: string }): Observable<GradeCostHistoryDTO> { return this.http.post<GradeCostHistoryDTO>(`${this.base}/${id}/cost-history`, body); }
  updateCostHistory(gradeId: string, entryId: string, body: { dailyCost: number; effectiveFrom: string }): Observable<GradeCostHistoryDTO> { return this.http.put<GradeCostHistoryDTO>(`${this.base}/${gradeId}/cost-history/${entryId}`, body); }
  deleteCostHistory(gradeId: string, entryId: string): Observable<void> { return this.http.delete<void>(`${this.base}/${gradeId}/cost-history/${entryId}`); }
}
