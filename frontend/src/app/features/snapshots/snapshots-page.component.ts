import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { MonthPickerComponent } from '../../shared/molecules/month-picker/month-picker.component';
import { SnapshotService } from '../../core/services/snapshot.service';
import { TeamService } from '../../core/services/team.service';
import { CapacitySnapshotDTO } from '../../core/models/snapshot.model';
import { TeamDTO } from '../../core/models/team.model';

@Component({
  selector: 'app-snapshots-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NzTableModule,
    NzButtonModule,
    NzSelectModule,
    NzDatePickerModule,
    NzCardModule,
    NzSpinModule,
    NzIconModule,
    NzDividerModule,
    NzTagModule,
    NzAlertModule,
    PageHeaderComponent,
    MonthPickerComponent,
  ],
  template: `
    <app-page-header title="Snapshots" subtitle="Generate and view capacity snapshots"></app-page-header>

    <!-- Generate Panel -->
    <nz-card nzTitle="Generate Snapshot" style="margin-bottom: 16px;">
      <div style="display:flex; gap:12px; flex-wrap:wrap; align-items:flex-end;">
        <div>
          <label style="display:block; margin-bottom:4px; font-weight:500;">Team</label>
          <nz-select [(ngModel)]="generateTeamId" nzPlaceHolder="Select team" style="width:200px">
            @for (t of teams; track t.id) {
              <nz-option [nzValue]="t.id" [nzLabel]="t.name"></nz-option>
            }
          </nz-select>
        </div>
        <div>
          <label style="display:block; margin-bottom:4px; font-weight:500;">Month</label>
          <app-month-picker (monthChange)="generateMonth = $event"></app-month-picker>
        </div>
        <button nz-button nzType="primary" (click)="generate()" [nzLoading]="generating" [disabled]="!generateTeamId || !generateMonth">
          <span nz-icon nzType="camera"></span> Generate
        </button>
        <nz-divider nzType="vertical"></nz-divider>
        <div>
          <label style="display:block; margin-bottom:4px; font-weight:500;">All Teams - Month</label>
          <app-month-picker (monthChange)="generateAllMonth = $event"></app-month-picker>
        </div>
        <button nz-button nzType="default" (click)="generateAll()" [nzLoading]="generatingAll" [disabled]="!generateAllMonth">
          <span nz-icon nzType="camera"></span> Generate All
        </button>
      </div>
    </nz-card>

    <!-- View Snapshots Panel -->
    <nz-card nzTitle="View Snapshots">
      <div style="display:flex; gap:12px; flex-wrap:wrap; align-items:flex-end; margin-bottom:16px;">
        <div>
          <label style="display:block; margin-bottom:4px; font-weight:500;">Team</label>
          <nz-select [(ngModel)]="viewTeamId" nzPlaceHolder="Select team" style="width:200px">
            @for (t of teams; track t.id) {
              <nz-option [nzValue]="t.id" [nzLabel]="t.name"></nz-option>
            }
          </nz-select>
        </div>
        <div>
          <label style="display:block; margin-bottom:4px; font-weight:500;">From</label>
          <app-month-picker (monthChange)="fromMonth = $event"></app-month-picker>
        </div>
        <div>
          <label style="display:block; margin-bottom:4px; font-weight:500;">To</label>
          <app-month-picker (monthChange)="toMonth = $event"></app-month-picker>
        </div>
        <button nz-button nzType="default" (click)="loadSnapshots()" [nzLoading]="loading" [disabled]="!viewTeamId || !fromMonth || !toMonth">
          <span nz-icon nzType="reload"></span> Load
        </button>
      </div>

      <nz-spin [nzSpinning]="loading">
        @if (snapshots.length > 0) {
          <nz-table [nzData]="snapshots" [nzBordered]="true" [nzSize]="'middle'" [nzPageSize]="20">
            <thead>
              <tr>
                <th>Month</th>
                <th>Category</th>
                <th>Capacity (Man Days)</th>
                <th>Created At</th>
              </tr>
            </thead>
            <tbody>
              @for (s of snapshots; track s.id) {
                <tr>
                  <td><nz-tag nzColor="blue">{{ s.snapshotMonth }}</nz-tag></td>
                  <td>{{ s.categoryName }}</td>
                  <td>{{ s.capacityManDays | number:'1.1-2' }}</td>
                  <td>{{ s.createdAt | date:'medium' }}</td>
                </tr>
              }
            </tbody>
          </nz-table>
        } @else if (!loading) {
          <nz-alert nzType="info" nzMessage="No snapshots found. Select a team and date range, then click Load." nzShowIcon></nz-alert>
        }
      </nz-spin>
    </nz-card>
  `,
})
export class SnapshotsPageComponent implements OnInit {
  private snapshotService = inject(SnapshotService);
  private teamService = inject(TeamService);
  private message = inject(NzMessageService);

  teams: TeamDTO[] = [];
  snapshots: CapacitySnapshotDTO[] = [];

  generateTeamId: string | null = null;
  generateMonth = '';
  generateAllMonth = '';
  viewTeamId: string | null = null;
  fromMonth = '';
  toMonth = '';

  loading = false;
  generating = false;
  generatingAll = false;

  ngOnInit(): void {
    this.teamService.getTeams().subscribe({ next: t => (this.teams = t) });
  }

  generate(): void {
    if (!this.generateTeamId || !this.generateMonth) return;
    this.generating = true;
    this.snapshotService.generateSnapshot(this.generateTeamId, this.generateMonth).subscribe({
      next: () => { this.message.success('Snapshot generated'); this.generating = false; },
      error: () => { this.message.error('Failed to generate'); this.generating = false; },
    });
  }

  generateAll(): void {
    if (!this.generateAllMonth) return;
    this.generatingAll = true;
    this.snapshotService.generateAll(this.generateAllMonth).subscribe({
      next: () => { this.message.success('All snapshots generated'); this.generatingAll = false; },
      error: () => { this.message.error('Failed'); this.generatingAll = false; },
    });
  }

  loadSnapshots(): void {
    if (!this.viewTeamId || !this.fromMonth || !this.toMonth) return;
    this.loading = true;
    this.snapshotService.getByTeam(this.viewTeamId, this.fromMonth, this.toMonth).subscribe({
      next: s => { this.snapshots = s; this.loading = false; },
      error: () => { this.message.error('Failed to load'); this.loading = false; },
    });
  }
}
