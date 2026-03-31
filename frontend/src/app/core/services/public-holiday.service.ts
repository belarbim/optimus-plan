import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PublicHolidayDTO, CreatePublicHolidayRequest } from '../models/public-holiday.model';

@Injectable({ providedIn: 'root' })
export class PublicHolidayService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/holidays`;

  getHolidays(year?: number): Observable<PublicHolidayDTO[]> {
    const params: Record<string, string> = {};
    if (year != null) params['year'] = year.toString();
    return this.http.get<PublicHolidayDTO[]>(this.base, { params });
  }

  createHoliday(body: CreatePublicHolidayRequest): Observable<PublicHolidayDTO> {
    return this.http.post<PublicHolidayDTO>(this.base, body);
  }

  updateHoliday(id: string, body: CreatePublicHolidayRequest): Observable<PublicHolidayDTO> {
    return this.http.put<PublicHolidayDTO>(`${this.base}/${id}`, body);
  }

  deleteHoliday(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
