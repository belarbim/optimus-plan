export interface AuditLogDTO {
  id: string;
  entityType: string;
  entityId: string;
  action: string;
  changes: string;
  actor: string;
  timestamp: string;
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
