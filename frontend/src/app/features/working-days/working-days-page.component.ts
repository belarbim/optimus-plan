import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
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
    NzModalModule,
    NzIconModule,
    NzSpinModule,
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

    <!-- Year selector + actions -->
    <div style="margin-bottom:16px; display:flex; gap:12px; align-items:center; justify-content:space-between; flex-wrap:wrap;">
      <div style="display:flex; gap:12px; align-items:center;">
        <label style="font-weight:500;">Year:</label>
        <nz-select [(ngModel)]="selectedYear" style="width:100px" (ngModelChange)="loadYear()">
          @for (y of availableYears; track y) {
            <nz-option [nzValue]="y" [nzLabel]="y.toString()"></nz-option>
          }
        </nz-select>
      </div>
      <button nz-button (click)="openImportModal()">
        <span nz-icon nzType="upload"></span> Import CSV
      </button>
    </div>

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

    <!-- Import CSV Modal -->
    <nz-modal
      [(nzVisible)]="importModalVisible"
      nzTitle="Import Working Days from CSV"
      (nzOnCancel)="importModalVisible = false"
      [nzFooter]="null"
    >
      <ng-container *nzModalContent>
        <p style="margin-bottom:8px;">
          Upload a CSV file with the following columns (header row required):
        </p>
        <code style="display:block; background:#f5f5f5; padding:8px; border-radius:4px; font-size:12px; margin-bottom:16px;">
          month, avgDaysWorked
        </code>
        <p style="font-size:12px; color:#888; margin-bottom:16px;">
          month format: YYYY-MM (e.g. 2025-01). avgDaysWorked is a number between 0 and 31.
        </p>

        <button nz-button style="margin-bottom:16px;" (click)="downloadTemplate()">
          <span nz-icon nzType="download"></span> Download Template
        </button>

        <div
          style="border:2px dashed #d9d9d9; border-radius:4px; padding:24px; text-align:center; cursor:pointer;"
          (click)="fileInput.click()"
          (dragover)="$event.preventDefault()"
          (drop)="onFileDrop($event)"
        >
          <span nz-icon nzType="inbox" style="font-size:32px; color:#40a9ff;"></span>
          <p style="margin:8px 0 4px;">Click or drag CSV file here</p>
          <p style="font-size:12px; color:#888;">{{ importFile ? importFile.name : 'No file selected' }}</p>
        </div>
        <input #fileInput type="file" accept=".csv" style="display:none" (change)="onFileSelect($event)">

        @if (importResult !== null) {
          <div style="margin-top:16px;">
            <p>
              <span nz-icon nzType="check-circle" style="color:#52c41a;"></span>
              {{ importResult }} record(s) imported successfully
            </p>
          </div>
        }

        <div style="margin-top:16px; display:flex; justify-content:flex-end; gap:8px;">
          <button nz-button (click)="importModalVisible = false">Cancel</button>
          <button nz-button nzType="primary" [disabled]="!importFile" [nzLoading]="importing" (click)="submitImport()">
            Import
          </button>
        </div>
      </ng-container>
    </nz-modal>
  `,
})
export class WorkingDaysPageComponent implements OnInit {
  private workingDaysService = inject(WorkingDaysService);
  private message = inject(NzMessageService);

  loading = false;
  importing = false;
  importModalVisible = false;
  importFile: File | null = null;
  importResult: number | null = null;
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

  openImportModal(): void {
    this.importFile = null;
    this.importResult = null;
    this.importModalVisible = true;
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      this.importFile = input.files[0];
      this.importResult = null;
    }
  }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file) { this.importFile = file; this.importResult = null; }
  }

  submitImport(): void {
    if (!this.importFile) return;
    this.importing = true;
    this.workingDaysService.importCsv(this.importFile).subscribe({
      next: d => {
        this.importResult = d.length;
        this.importing = false;
        if (d.length > 0) this.loadYear();
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
