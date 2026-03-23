import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzUploadModule, NzUploadFile } from 'ng-zorro-antd/upload';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { WorkingDaysService } from '../../core/services/working-days.service';
import { WorkingDaysConfigDTO } from '../../core/models/working-days.model';

@Component({
  selector: 'app-working-days-page',
  standalone: true,
  imports: [
    CommonModule,
    NzTableModule,
    NzButtonModule,
    NzUploadModule,
    NzIconModule,
    NzSpinModule,
    NzCardModule,
    NzAlertModule,
    NzTagModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header title="Working Days" subtitle="Configure average working days per month"></app-page-header>

    <nz-card nzTitle="Import Working Days" style="margin-bottom:16px;">
      <div style="display:flex; gap:12px; align-items:center; flex-wrap:wrap;">
        <nz-upload
          [nzBeforeUpload]="beforeUpload"
          [nzShowUploadList]="false"
          nzAccept=".csv"
        >
          <button nz-button>
            <span nz-icon nzType="upload"></span> Select CSV File
          </button>
        </nz-upload>
        @if (selectedFile) {
          <span>{{ selectedFile.name }}</span>
          <button nz-button nzType="primary" (click)="importCsv()" [nzLoading]="importing">
            Import
          </button>
        }
      </div>
      <div style="margin-top:12px;">
        <nz-alert
          nzType="info"
          nzMessage="CSV format: month (YYYY-MM), avgDaysWorked (number)"
          nzShowIcon
        ></nz-alert>
      </div>
    </nz-card>

    <nz-spin [nzSpinning]="loading">
      <nz-table
        #table
        [nzData]="workingDays"
        [nzBordered]="true"
        [nzSize]="'middle'"
        [nzPageSize]="20"
      >
        <thead>
          <tr>
            <th>Month</th>
            <th>Avg Days Worked</th>
          </tr>
        </thead>
        <tbody>
          @for (w of table.data; track w.id) {
            <tr>
              <td><nz-tag nzColor="blue">{{ w.month }}</nz-tag></td>
              <td>{{ w.avgDaysWorked }}</td>
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
  workingDays: WorkingDaysConfigDTO[] = [];
  selectedFile: File | null = null;

  ngOnInit(): void {
    this.loadWorkingDays();
  }

  loadWorkingDays(): void {
    this.loading = true;
    this.workingDaysService.getWorkingDays().subscribe({
      next: d => { this.workingDays = d; this.loading = false; },
      error: () => { this.message.error('Failed to load working days'); this.loading = false; },
    });
  }

  beforeUpload = (file: NzUploadFile): boolean => {
    this.selectedFile = file as unknown as File;
    return false; // prevent auto upload
  };

  importCsv(): void {
    if (!this.selectedFile) return;
    this.importing = true;
    this.workingDaysService.importCsv(this.selectedFile).subscribe({
      next: d => {
        this.message.success(`Imported ${d.length} records`);
        this.importing = false;
        this.selectedFile = null;
        this.loadWorkingDays();
      },
      error: () => { this.message.error('Import failed'); this.importing = false; },
    });
  }
}
