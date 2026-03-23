import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { AuditService } from '../../core/services/audit.service';
import { AuditLogDTO, PageResult } from '../../core/models/audit.model';

@Component({
  selector: 'app-audit-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NzTableModule,
    NzButtonModule,
    NzSelectModule,
    NzDatePickerModule,
    NzInputModule,
    NzTagModule,
    NzSpinModule,
    NzIconModule,
    NzCardModule,
    NzGridModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header title="Audit Log" subtitle="Track all system changes"></app-page-header>

    <!-- Filters -->
    <nz-card style="margin-bottom:16px;">
      <div nz-row [nzGutter]="[12, 12]" style="align-items:flex-end;">
        <div nz-col [nzXs]="24" [nzSm]="12" [nzMd]="6">
          <label style="display:block; margin-bottom:4px; font-weight:500;">Entity Type</label>
          <nz-select [(ngModel)]="filterEntityType" nzAllowClear nzPlaceHolder="All types" style="width:100%">
            @for (t of entityTypes; track t) {
              <nz-option [nzValue]="t" [nzLabel]="t"></nz-option>
            }
          </nz-select>
        </div>
        <div nz-col [nzXs]="24" [nzSm]="12" [nzMd]="6">
          <label style="display:block; margin-bottom:4px; font-weight:500;">Action</label>
          <nz-select [(ngModel)]="filterAction" nzAllowClear nzPlaceHolder="All actions" style="width:100%">
            @for (a of actions; track a) {
              <nz-option [nzValue]="a" [nzLabel]="a"></nz-option>
            }
          </nz-select>
        </div>
        <div nz-col [nzXs]="24" [nzSm]="12" [nzMd]="8">
          <label style="display:block; margin-bottom:4px; font-weight:500;">Date Range</label>
          <nz-range-picker
            [(ngModel)]="dateRange"
            nzFormat="yyyy-MM-dd"
            style="width:100%"
          ></nz-range-picker>
        </div>
        <div nz-col [nzXs]="24" [nzSm]="12" [nzMd]="4">
          <button nz-button nzType="primary" style="width:100%" (click)="applyFilters()">
            <span nz-icon nzType="search"></span> Search
          </button>
        </div>
      </div>
    </nz-card>

    <!-- Table -->
    <nz-spin [nzSpinning]="loading">
      <nz-table
        [nzData]="logs"
        [nzBordered]="true"
        [nzSize]="'middle'"
        [nzTotal]="total"
        [nzPageIndex]="pageIndex"
        [nzPageSize]="pageSize"
        [nzFrontPagination]="false"
        (nzPageIndexChange)="onPageChange($event)"
        (nzPageSizeChange)="onPageSizeChange($event)"
        [nzShowSizeChanger]="true"
        [nzPageSizeOptions]="[10, 20, 50]"
      >
        <thead>
          <tr>
            <th>Timestamp</th>
            <th>Entity Type</th>
            <th>Entity ID</th>
            <th>Action</th>
            <th>Actor</th>
            <th>Changes</th>
          </tr>
        </thead>
        <tbody>
          @for (log of logs; track log.id) {
            <tr>
              <td style="white-space:nowrap;">{{ log.timestamp | date:'short' }}</td>
              <td><nz-tag nzColor="blue">{{ log.entityType }}</nz-tag></td>
              <td style="font-family:monospace; font-size:12px;">{{ log.entityId }}</td>
              <td>
                <nz-tag [nzColor]="getActionColor(log.action)">{{ log.action }}</nz-tag>
              </td>
              <td>{{ log.actor }}</td>
              <td style="max-width:300px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
                {{ log.changes }}
              </td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-spin>
  `,
})
export class AuditPageComponent implements OnInit {
  private auditService = inject(AuditService);

  logs: AuditLogDTO[] = [];
  total = 0;
  pageIndex = 1;
  pageSize = 20;
  loading = false;

  filterEntityType: string | null = null;
  filterAction: string | null = null;
  dateRange: [Date, Date] | null = null;

  entityTypes = ['TEAM', 'EMPLOYEE', 'ASSIGNMENT', 'CAPACITY', 'ALERT', 'ROLE_TYPE', 'HOLIDAY'];
  actions = ['CREATE', 'UPDATE', 'DELETE', 'READ'];

  ngOnInit(): void {
    this.loadLogs();
  }

  applyFilters(): void {
    this.pageIndex = 1;
    this.loadLogs();
  }

  loadLogs(): void {
    this.loading = true;
    const params: Record<string, unknown> = {
      page: this.pageIndex - 1,
      size: this.pageSize,
    };
    if (this.filterEntityType) params['entityType'] = this.filterEntityType;
    if (this.filterAction) params['action'] = this.filterAction;
    if (this.dateRange?.[0]) params['dateFrom'] = this.dateRange[0].toISOString().split('T')[0];
    if (this.dateRange?.[1]) params['dateTo'] = this.dateRange[1].toISOString().split('T')[0];

    this.auditService.getLogs(params as Parameters<typeof this.auditService.getLogs>[0]).subscribe({
      next: (page: PageResult<AuditLogDTO>) => {
        this.logs = page.content;
        this.total = page.totalElements;
        this.loading = false;
      },
      error: () => this.loading = false,
    });
  }

  onPageChange(page: number): void {
    this.pageIndex = page;
    this.loadLogs();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.pageIndex = 1;
    this.loadLogs();
  }

  getActionColor(action: string): string {
    switch (action) {
      case 'CREATE': return 'success';
      case 'UPDATE': return 'warning';
      case 'DELETE': return 'error';
      default: return 'default';
    }
  }
}
