import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzStatisticModule } from 'ng-zorro-antd/statistic';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzInputModule } from 'ng-zorro-antd/input';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { StatCardComponent } from '../../shared/atoms/stat-card/stat-card.component';
import { MonthPickerComponent } from '../../shared/molecules/month-picker/month-picker.component';
import { CapacityService } from '../../core/services/capacity.service';
import { TeamService } from '../../core/services/team.service';
import { TeamDTO } from '../../core/models/team.model';
import {
  CapacityResultDTO,
  RemainingCapacityDTO,
  RollupCapacityDTO,
  SimulationResultDTO,
} from '../../core/models/capacity.model';

@Component({
  selector: 'app-capacity-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NzTableModule,
    NzButtonModule,
    NzSelectModule,
    NzDatePickerModule,
    NzTabsModule,
    NzCardModule,
    NzGridModule,
    NzStatisticModule,
    NzSpinModule,
    NzIconModule,
    NzTagModule,
    NzAlertModule,
    NzDividerModule,
    NzInputModule,
    PageHeaderComponent,
    StatCardComponent,
    MonthPickerComponent,
  ],
  template: `
    <app-page-header title="Capacity" subtitle="Analyze team capacity by month"></app-page-header>

    <!-- Selectors -->
    <nz-card style="margin-bottom: 16px;">
      <div style="display:flex; gap:16px; flex-wrap:wrap; align-items:center;">
        <div>
          <label style="margin-right:8px; font-weight:500;">Team:</label>
          <nz-select
            [(ngModel)]="selectedTeamId"
            nzPlaceHolder="Select team"
            style="width:220px"
            (ngModelChange)="onTeamChange()"
          >
            @for (t of teams; track t.id) {
              <nz-option [nzValue]="t.id" [nzLabel]="t.name"></nz-option>
            }
          </nz-select>
        </div>
        <div>
          <label style="margin-right:8px; font-weight:500;">Month:</label>
          <app-month-picker (monthChange)="onMonthChange($event)"></app-month-picker>
        </div>
        <button nz-button nzType="primary" (click)="loadCapacity()" [disabled]="!selectedTeamId || !selectedMonth">
          <span nz-icon nzType="reload"></span> Load
        </button>
      </div>
    </nz-card>

    @if (selectedTeamId && selectedMonth) {
      <nz-tabs>
        <!-- Capacity Tab -->
        <nz-tab nzTitle="Capacity">
          <nz-spin [nzSpinning]="loading">
            @if (capacityResult) {
              <div nz-row [nzGutter]="[16,16]" style="margin-bottom:24px;">
                <div nz-col [nzXs]="24" [nzSm]="8">
                  <app-stat-card
                    title="Total Capacity (Man Days)"
                    [value]="formatNum(capacityResult.totalCapacity)"
                    icon="bar-chart"
                    color="#1890ff"
                  ></app-stat-card>
                </div>
                <div nz-col [nzXs]="24" [nzSm]="8">
                  <app-stat-card
                    title="Team"
                    [value]="capacityResult.teamName"
                    icon="team"
                    color="#52c41a"
                  ></app-stat-card>
                </div>
                <div nz-col [nzXs]="24" [nzSm]="8">
                  <app-stat-card
                    title="Month"
                    [value]="capacityResult.month"
                    icon="calendar"
                    color="#faad14"
                  ></app-stat-card>
                </div>
              </div>

              <div nz-row [nzGutter]="[16,16]">
                <div nz-col [nzXs]="24" [nzMd]="12">
                  <nz-card nzTitle="Category Breakdown">
                    <nz-table [nzData]="capacityResult.categoryBreakdown" [nzBordered]="true" [nzSize]="'small'" [nzShowPagination]="false">
                      <thead><tr><th>Category</th><th>Man Days</th></tr></thead>
                      <tbody>
                        @for (c of capacityResult.categoryBreakdown; track c.categoryName) {
                          <tr><td>{{ c.categoryName }}</td><td>{{ c.manDays | number:'1.1-2' }}</td></tr>
                        }
                      </tbody>
                    </nz-table>
                  </nz-card>
                </div>
                <div nz-col [nzXs]="24" [nzMd]="12">
                  <nz-card nzTitle="Employee Contributions">
                    <nz-table [nzData]="capacityResult.employeeContributions" [nzBordered]="true" [nzSize]="'small'" [nzPageSize]="10">
                      <thead><tr><th>Employee</th><th>Allocation</th><th>Role</th><th>Man Days</th></tr></thead>
                      <tbody>
                        @for (e of capacityResult.employeeContributions; track e.employeeId) {
                          <tr>
                            <td>{{ e.employeeName }}</td>
                            <td>{{ e.allocationPct }}%</td>
                            <td>{{ e.roleType }}</td>
                            <td>{{ e.totalManDays | number:'1.1-2' }}</td>
                          </tr>
                        }
                      </tbody>
                    </nz-table>
                  </nz-card>
                </div>
              </div>
            } @else if (!loading) {
              <nz-alert nzType="info" nzMessage="Select a team and month, then click Load." nzShowIcon></nz-alert>
            }
          </nz-spin>
        </nz-tab>

        <!-- Rollup Tab -->
        <nz-tab nzTitle="Rollup">
          <nz-spin [nzSpinning]="loadingRollup">
            <div style="margin-bottom:12px;">
              <button nz-button nzType="default" (click)="loadRollup()" [disabled]="!selectedTeamId || !selectedMonth">
                <span nz-icon nzType="reload"></span> Load Rollup
              </button>
            </div>
            @if (rollupResult) {
              <div nz-row [nzGutter]="[16,16]" style="margin-bottom:16px;">
                <div nz-col [nzXs]="24" [nzSm]="8">
                  <app-stat-card title="Own Capacity" [value]="formatNum(rollupResult.ownCapacity.totalCapacity)" icon="bar-chart" color="#1890ff"></app-stat-card>
                </div>
                <div nz-col [nzXs]="24" [nzSm]="8">
                  <app-stat-card title="Sub-Teams" [value]="rollupResult.subTeamCapacities.length" icon="team" color="#52c41a"></app-stat-card>
                </div>
                <div nz-col [nzXs]="24" [nzSm]="8">
                  <app-stat-card title="Consolidated Total" [value]="formatNum(rollupResult.consolidatedTotal)" icon="bar-chart" color="#722ed1"></app-stat-card>
                </div>
              </div>
              <nz-card nzTitle="Sub-Team Capacities">
                <nz-table [nzData]="rollupResult.subTeamCapacities" [nzBordered]="true" [nzSize]="'small'">
                  <thead><tr><th>Team</th><th>Month</th><th>Total Capacity</th></tr></thead>
                  <tbody>
                    @for (sc of rollupResult.subTeamCapacities; track sc.teamId) {
                      <tr><td>{{ sc.teamName }}</td><td>{{ sc.month }}</td><td>{{ sc.totalCapacity | number:'1.1-2' }}</td></tr>
                    }
                  </tbody>
                </nz-table>
              </nz-card>
            }
          </nz-spin>
        </nz-tab>

        <!-- Remaining Tab -->
        <nz-tab nzTitle="Remaining">
          <div style="margin-bottom:16px; display:flex; gap:12px; align-items:center;">
            <nz-date-picker
              [(ngModel)]="remainingDate"
              nzFormat="yyyy-MM-dd"
              nzPlaceHolder="Select date"
            ></nz-date-picker>
            <button nz-button nzType="default" (click)="loadRemaining()" [disabled]="!selectedTeamId || !remainingDate">
              <span nz-icon nzType="reload"></span> Load Remaining
            </button>
          </div>
          <nz-spin [nzSpinning]="loadingRemaining">
            @if (remainingResult) {
              <div nz-row [nzGutter]="[16,16]" style="margin-bottom:16px;">
                <div nz-col [nzXs]="24" [nzSm]="8">
                  <app-stat-card title="Remaining Business Days" [value]="remainingResult.remainingBusinessDays" icon="calendar" color="#1890ff"></app-stat-card>
                </div>
                <div nz-col [nzXs]="24" [nzSm]="8">
                  <app-stat-card title="Total Business Days" [value]="remainingResult.totalBusinessDays" icon="calendar" color="#52c41a"></app-stat-card>
                </div>
                <div nz-col [nzXs]="24" [nzSm]="8">
                  <app-stat-card title="Total Remaining Man Days" [value]="formatNum(remainingResult.totalRemaining)" icon="bar-chart" color="#faad14"></app-stat-card>
                </div>
              </div>
              <nz-card nzTitle="Category Breakdown">
                <nz-table [nzData]="remainingResult.categoryBreakdown" [nzBordered]="true" [nzSize]="'small'" [nzShowPagination]="false">
                  <thead><tr><th>Category</th><th>Man Days</th></tr></thead>
                  <tbody>
                    @for (c of remainingResult.categoryBreakdown; track c.categoryName) {
                      <tr><td>{{ c.categoryName }}</td><td>{{ c.manDays | number:'1.1-2' }}</td></tr>
                    }
                  </tbody>
                </nz-table>
              </nz-card>
            }
          </nz-spin>
        </nz-tab>

        <!-- Simulation Tab -->
        <nz-tab nzTitle="Simulation">
          <nz-card>
            <p style="color:#888;">Enter simulation parameters below (JSON body) and run simulation.</p>
            <textarea
              nz-input
              [(ngModel)]="simulationJson"
              placeholder='{"key": "value"}'
              rows="6"
              style="font-family: monospace; margin-bottom:12px; width:100%;"
            ></textarea>
            <button nz-button nzType="primary" (click)="runSimulation()" [nzLoading]="loadingSimulation">
              Run Simulation
            </button>

            @if (simulationResult) {
              <nz-divider></nz-divider>
              <div nz-row [nzGutter]="[16,16]">
                <div nz-col [nzXs]="24" [nzSm]="12">
                  <h4>Baseline: {{ simulationResult.baseline.totalCapacity | number:'1.1-2' }} man-days</h4>
                </div>
                <div nz-col [nzXs]="24" [nzSm]="12">
                  <h4>Simulated: {{ simulationResult.simulated.totalCapacity | number:'1.1-2' }} man-days</h4>
                </div>
              </div>
              @if (simulationResult.warnings.length > 0) {
                <div style="margin-top:12px;">
                  @for (w of simulationResult.warnings; track w) {
                    <nz-alert nzType="warning" [nzMessage]="w" nzShowIcon style="margin-bottom:8px;"></nz-alert>
                  }
                </div>
              }
              <nz-card nzTitle="Deltas" style="margin-top:12px;">
                <nz-table [nzData]="deltaRows" [nzBordered]="true" [nzSize]="'small'" [nzShowPagination]="false">
                  <thead><tr><th>Category</th><th>Delta</th></tr></thead>
                  <tbody>
                    @for (d of deltaRows; track d.key) {
                      <tr>
                        <td>{{ d.key }}</td>
                        <td>
                          <nz-tag [nzColor]="d.value >= 0 ? 'green' : 'red'">
                            {{ d.value >= 0 ? '+' : '' }}{{ d.value | number:'1.1-2' }}
                          </nz-tag>
                        </td>
                      </tr>
                    }
                  </tbody>
                </nz-table>
              </nz-card>
            }
          </nz-card>
        </nz-tab>
      </nz-tabs>
    } @else {
      <nz-alert nzType="info" nzMessage="Please select a team and month to begin." nzShowIcon></nz-alert>
    }
  `,
})
export class CapacityPageComponent implements OnInit {
  private capacityService = inject(CapacityService);
  private teamService = inject(TeamService);

  teams: TeamDTO[] = [];
  selectedTeamId: string | null = null;
  selectedMonth = '';
  remainingDate: Date | null = null;
  simulationJson = '{}';

  loading = false;
  loadingRollup = false;
  loadingRemaining = false;
  loadingSimulation = false;

  capacityResult: CapacityResultDTO | null = null;
  rollupResult: RollupCapacityDTO | null = null;
  remainingResult: RemainingCapacityDTO | null = null;
  simulationResult: SimulationResultDTO | null = null;
  deltaRows: { key: string; value: number }[] = [];

  ngOnInit(): void {
    this.teamService.getTeams().subscribe({ next: t => (this.teams = t) });
  }

  formatNum(val: number): string {
    return val?.toFixed(2) ?? '0.00';
  }

  onTeamChange(): void {
    this.capacityResult = null;
    this.rollupResult = null;
    this.remainingResult = null;
    this.simulationResult = null;
  }

  onMonthChange(month: string): void {
    this.selectedMonth = month;
    this.capacityResult = null;
  }

  loadCapacity(): void {
    if (!this.selectedTeamId || !this.selectedMonth) return;
    this.loading = true;
    this.capacityService.getTeamCapacity(this.selectedTeamId, this.selectedMonth).subscribe({
      next: r => { this.capacityResult = r; this.loading = false; },
      error: () => this.loading = false,
    });
  }

  loadRollup(): void {
    if (!this.selectedTeamId || !this.selectedMonth) return;
    this.loadingRollup = true;
    this.capacityService.getRollupCapacity(this.selectedTeamId, this.selectedMonth).subscribe({
      next: r => { this.rollupResult = r; this.loadingRollup = false; },
      error: () => this.loadingRollup = false,
    });
  }

  loadRemaining(): void {
    if (!this.selectedTeamId || !this.remainingDate) return;
    const dateStr = this.remainingDate.toISOString().split('T')[0];
    this.loadingRemaining = true;
    this.capacityService.getRemainingCapacity(this.selectedTeamId, dateStr).subscribe({
      next: r => { this.remainingResult = r; this.loadingRemaining = false; },
      error: () => this.loadingRemaining = false,
    });
  }

  runSimulation(): void {
    if (!this.selectedTeamId) return;
    let body: Record<string, unknown> = {};
    try { body = JSON.parse(this.simulationJson); } catch { return; }
    this.loadingSimulation = true;
    this.capacityService.simulate(this.selectedTeamId, body).subscribe({
      next: r => {
        this.simulationResult = r;
        this.deltaRows = Object.entries(r.deltas).map(([key, value]) => ({ key, value }));
        this.loadingSimulation = false;
      },
      error: () => this.loadingSimulation = false,
    });
  }
}
