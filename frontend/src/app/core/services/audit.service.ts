import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuditLogDTO, PageResult } from '../models/audit.model';

export interface AuditQueryParams {
  entityType?: string;
  action?: string;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class AuditService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/audit`;

  getLogs(params: AuditQueryParams = {}): Observable<PageResult<AuditLogDTO>> {
    const queryParams: Record<string, string> = {};
    if (params.entityType) queryParams['entityType'] = params.entityType;
    if (params.action) queryParams['action'] = params.action;
    if (params.dateFrom) queryParams['dateFrom'] = params.dateFrom;
    if (params.dateTo) queryParams['dateTo'] = params.dateTo;
    queryParams['page'] = String(params.page ?? 0);
    queryParams['size'] = String(params.size ?? 20);
    return this.http.get<PageResult<AuditLogDTO>>(this.base, { params: queryParams });
  }
}
