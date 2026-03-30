import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzUploadModule, NzUploadFile } from 'ng-zorro-antd/upload';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { WorkingDaysService } from '../../core/services/working-days.service';
import { WorkingDaysConfigDTO } from '../../core/models/working-days.model';

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

interface MonthRow {
  month: string;        // yyyy-MM
  label: string;        // "January"
  avgDaysWorked: number | null;
  editing: boolean;
  editValue: number | null;
  saving: boolean;
}

@Component({
  selector: 'app-working-days-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NzTableModule,
    NzButtonModule,
    NzUploadModule,
    NzIconModule,
    NzSpinModule,
    NzCardModule,
    NzAlertModule,
    NzTagModule,
    NzSelectModule,
    NzInputNumberModule,
    NzTooltipModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header
      title="Working Days"
      subtitle="Configure average working days per month — used in capacity computation"
    ></app-page-header>

    <!-- Year selector + CSV import -->
    <nz-card style="margin-bottom:16px;">
      <div style="display:flex; gap:16px; flex-wrap:wrap; align-items:center; justify-content:space-between;">
        <div style="display:flex; gap:12px; align-items:center;">
          <label style="font-weight:500;">Year:</label>
          <nz-select [(ngModel)]="selectedYear" style="width:100px" (ngModelChange)="loadYear()">
            @for (y of availableYears; track y) {
              <nz-option [nzValue]="y" [nzLabel]="y.toString()"></nz-option>
            }
          </nz-select>
        </div>

        <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
          <nz-upload [nzBeforeUpload]="beforeUpload" [nzShowUploadList]="false" nzAccept=".csv">
            <button nz-button>
              <span nz-icon nzType="upload"></span> Import CSV
            </button>
          </nz-upload>
          @if (selectedFile) {
            <span style="color:#666;">{{ selectedFile.name }}</span>
            <button nz-button nzType="primary" (click)="importCsv()" [nzLoading]="importing">
              Upload
            </button>
          }
          <button nz-button nzType="default" (click)="downloadTemplate()">
            <span nz-icon nzType="download"></span> CSV Template
          </button>
        </div>
      </div>
      <div style="margin-top:10px;">
        <nz-alert
          nzType="info"
          nzMessage="CSV format: month (YYYY-MM), avgDaysWorked — used to override calendar business days in capacity computation."
          nzShowIcon
        ></nz-alert>
      </div>
    </nz-card>

    <!-- 12-month table -->
    <nz-spin [nzSpinning]="loading">
      <nz-table
        [nzData]="rows"
        [nzBordered]="true"
        [nzSize]="'middle'"
        [nzShowPagination]="false"
      >
        <thead>
          <tr>
            <th style="width:160px">Month</th>
            <th>Working Days (avg)</th>
            <th style="width:140px">Used in Capacity</th>
            <th style="width:120px">Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (row of rows; track row.month) {
            <tr>
              <td><strong>{{ row.label }}</strong> <span style="color:#999; font-size:12px;">{{ row.month }}</span></td>
              <td>
                @if (row.editing) {
                  <nz-input-number
                    [(ngModel)]="row.editValue"
                    [nzMin]="0"
                    [nzMax]="31"
                    [nzStep]="0.5"
                    style="width:120px"
                    (keydown.enter)="saveRow(row)"
                    (keydown.escape)="cancelEdit(row)"
                  ></nz-input-number>
                } @else if (row.avgDaysWorked !== null) {
                  <nz-tag nzColor="blue">{{ row.avgDaysWorked }}</nz-tag>
                } @else {
                  <span style="color:#bbb;">— not configured</span>
                }
              </td>
              <td>
                @if (row.avgDaysWorked !== null) {
                  <nz-tag nzColor="green">
                    <span nz-icon nzType="check"></span> Configured
                  </nz-tag>
                } @else {
                  <nz-tag nzColor="warning"
                    nz-tooltip nzTooltipTitle="Capacity will use calendar business days (Mon–Fri) as fallback">
                    Calendar fallback
                  </nz-tag>
                }
              </td>
              <td>
                @if (row.editing) {
                  <button nz-button nzType="link" (click)="saveRow(row)" [nzLoading]="row.saving">
                    <span nz-icon nzType="check"></span>
                  </button>
                  <button nz-button nzType="link" (click)="cancelEdit(row)">
                    <span nz-icon nzType="close"></span>
                  </button>
                } @else {
                  <button nz-button nzType="link" (click)="startEdit(row)" title="Edit">
                    <span nz-icon nzType="edit"></span>
                  </button>
                }
              </td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-spin>
  `,
})
export class WorkingDaysPageComponent implements OnInit {
  private workingDaysService = inject(WorkingDaysService);
  private message = inject(NzMessageService);

  loading = false;
  importing = false;
  selectedFile: File | null = null;
  selectedYear = new Date().getFullYear();
  availableYears = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - 2 + i);
  rows: MonthRow[] = [];

  ngOnInit(): void {
    this.loadYear();
  }

  loadYear(): void {
    this.rows = this.buildRows([]);
    this.loading = true;
    this.workingDaysService.getByYear(this.selectedYear).subscribe({
      next: data => { this.rows = this.buildRows(data); this.loading = false; },
      error: () => { this.message.error('Failed to load working days'); this.loading = false; },
    });
  }

  private buildRows(data: WorkingDaysConfigDTO[]): MonthRow[] {
    const map = new Map(data.map(d => [d.month, d.avgDaysWorked]));
    return MONTH_NAMES.map((label, i) => {
      const month = `${this.selectedYear}-${String(i + 1).padStart(2, '0')}`;
      const configured = map.get(month) ?? null;
      return { month, label, avgDaysWorked: configured, editing: false, editValue: null, saving: false };
    });
  }

  startEdit(row: MonthRow): void {
    row.editValue = row.avgDaysWorked ?? 20;
    row.editing = true;
  }

  cancelEdit(row: MonthRow): void {
    row.editing = false;
    row.editValue = null;
  }

  saveRow(row: MonthRow): void {
    if (row.editValue === null) return;
    row.saving = true;
    this.workingDaysService.upsertMonth(row.month, row.editValue).subscribe({
      next: saved => {
        row.avgDaysWorked = saved.avgDaysWorked;
        row.editing = false;
        row.editValue = null;
        row.saving = false;
        this.message.success(`${row.label} updated to ${saved.avgDaysWorked} days`);
      },
      error: () => { this.message.error('Failed to save'); row.saving = false; },
    });
  }

  beforeUpload = (file: NzUploadFile): boolean => {
    this.selectedFile = file as unknown as File;
    return false;
  };

  importCsv(): void {
    if (!this.selectedFile) return;
    this.importing = true;
    this.workingDaysService.importCsv(this.selectedFile).subscribe({
      next: d => {
        this.message.success(`Imported ${d.length} records`);
        this.importing = false;
        this.selectedFile = null;
        this.loadYear();
      },
      error: () => { this.message.error('Import failed'); this.importing = false; },
    });
  }

  downloadTemplate(): void {
    const lines = ['month,avgDaysWorked'];
    for (let m = 1; m <= 12; m++) {
      lines.push(`${this.selectedYear}-${String(m).padStart(2, '0')},20`);
    }
    const blob = new Blob([lines.join('\n')], { type: 'text/csv' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `working-days-${this.selectedYear}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
  }
}
