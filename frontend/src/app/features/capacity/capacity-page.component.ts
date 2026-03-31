import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
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
  CategoryBreakdownItem,
  EmployeeContribution,
  RemainingCapacityDTO,
  RollupCapacityDTO,
  SimulationResultDTO,
} from '../../core/models/capacity.model';

type ViewMode = 'month' | 'Q1' | 'Q2' | 'Q3' | 'Q4' | 'year';

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
    <app-page-header title="Capacity" subtitle="Analyze team capacity by period or month"></app-page-header>

    <!-- Team selector -->
    <nz-card style="margin-bottom: 16px;">
      <div style="display:flex; gap:16px; flex-wrap:wrap; align-items:center;">
        <label style="font-weight:500;">Team:</label>
        <nz-select
          [(ngModel)]="selectedTeamId"
          nzPlaceHolder="Select team"
          style="width:240px"
          (ngModelChange)="onTeamChange()"
        >
          @for (t of teams; track t.id) {
            <nz-option [nzValue]="t.id" [nzLabel]="t.name"></nz-option>
          }
        </nz-select>
      </div>
    </nz-card>

    @if (selectedTeamId) {
      <nz-tabs>

        <!-- ── Capacity Tab ── -->
        <nz-tab nzTitle="Capacity">

          <!-- Filter bar -->
          <nz-card style="margin-bottom:16px;">
            <div style="display:flex; gap:16px; flex-wrap:wrap; align-items:center;">
              <div>
                <label style="margin-right:8px; font-weight:500;">View by:</label>
                <nz-select [(ngModel)]="viewMode" style="width:150px" (ngModelChange)="onViewModeChange()">
                  <nz-option nzValue="month" nzLabel="Month"></nz-option>
                  <nz-option nzValue="Q1"    nzLabel="Q1 (Jan – Mar)"></nz-option>
                  <nz-option nzValue="Q2"    nzLabel="Q2 (Apr – Jun)"></nz-option>
                  <nz-option nzValue="Q3"    nzLabel="Q3 (Jul – Sep)"></nz-option>
                  <nz-option nzValue="Q4"    nzLabel="Q4 (Oct – Dec)"></nz-option>
                  <nz-option nzValue="year"  nzLabel="Full Year"></nz-option>
                </nz-select>
              </div>

              @if (viewMode === 'month') {
                <div>
                  <label style="margin-right:8px; font-weight:500;">Month:</label>
                  <app-month-picker (monthChange)="onMonthChange($event)"></app-month-picker>
                </div>
              } @else {
                <div>
                  <label style="margin-right:8px; font-weight:500;">Year:</label>
                  <nz-select [(ngModel)]="selectedYear" style="width:100px">
                    @for (y of availableYears; track y) {
                      <nz-option [nzValue]="y" [nzLabel]="y.toString()"></nz-option>
                    }
                  </nz-select>
                </div>
              }

              <button
                nz-button nzType="primary"
                (click)="loadCapacityForMode()"
                [nzLoading]="loading"
                [disabled]="viewMode === 'month' && !selectedMonth"
              >
                <span nz-icon nzType="reload"></span> Load
              </button>
            </div>
          </nz-card>

          <!-- Unified result view (same layout for month / quarter / year) -->
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
                    [title]="viewMode === 'month' ? 'Month' : viewMode === 'year' ? 'Year' : 'Quarter'"
                    [value]="capacityResult.month"
                    icon="calendar"
                    color="#faad14"
                  ></app-stat-card>
                </div>
              </div>

              <div nz-row [nzGutter]="[16,16]">
                <div nz-col [nzXs]="24" [nzMd]="12">
                  <nz-card nzTitle="Category Breakdown">
                    <nz-table
                      [nzData]="capacityResult.categoryBreakdown"
                      [nzBordered]="true"
                      [nzSize]="'small'"
                      [nzShowPagination]="false"
                    >
                      <thead><tr><th>Category</th><th>Man Days</th></tr></thead>
                      <tbody>
                        @for (c of capacityResult.categoryBreakdown; track c.categoryName) {
                          <tr>
                            <td>{{ c.categoryName }}</td>
                            <td>{{ c.manDays | number:'1.1-2' }}</td>
                          </tr>
                        }
                      </tbody>
                    </nz-table>
                  </nz-card>
                </div>
                <div nz-col [nzXs]="24" [nzMd]="12">
                  <nz-card nzTitle="Employee Contributions">
                    <nz-table
                      [nzData]="capacityResult.employeeContributions"
                      [nzBordered]="true"
                      [nzSize]="'small'"
                      [nzPageSize]="10"
                    >
                      <thead>
                        <tr>
                          <th>Employee</th>
                          <th>Allocation</th>
                          <th>Role</th>
                          <th>Man Days</th>
                        </tr>
                      </thead>
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
              <nz-alert
                nzType="info"
                [nzMessage]="viewMode === 'month' ? 'Select a month then click Load.' : 'Click Load to view capacity for the selected period.'"
                nzShowIcon
              ></nz-alert>
            }
          </nz-spin>
        </nz-tab>

        <!-- ── Rollup Tab ── -->
        <nz-tab nzTitle="Rollup">
          <nz-spin [nzSpinning]="loadingRollup">
            <div style="margin-bottom:12px; display:flex; gap:12px; align-items:center; flex-wrap:wrap;">
              <label style="font-weight:500;">Month:</label>
              <app-month-picker (monthChange)="onRollupMonthChange($event)"></app-month-picker>
              <button nz-button nzType="default" (click)="loadRollup()" [disabled]="!rollupMonth">
                <span nz-icon nzType="reload"></span> Load Rollup
              </button>
            </div>
            @if (rollupResult) {
              <div nz-row [nzGutter]="[16,16]" style="margin-bottom:24px;">
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

              <div nz-row [nzGutter]="[16,16]">
                <div nz-col [nzXs]="24" [nzMd]="12">
                  <nz-card nzTitle="Category Breakdown">
                    <nz-table
                      [nzData]="rollupCategories"
                      [nzBordered]="true"
                      [nzSize]="'small'"
                      [nzShowPagination]="false"
                    >
                      <thead><tr><th>Category</th><th>Man Days</th></tr></thead>
                      <tbody>
                        @for (c of rollupCategories; track c.categoryName) {
                          <tr>
                            <td>{{ c.categoryName }}</td>
                            <td>{{ c.manDays | number:'1.1-2' }}</td>
                          </tr>
                        }
                      </tbody>
                    </nz-table>
                  </nz-card>
                </div>
                <div nz-col [nzXs]="24" [nzMd]="12">
                  <nz-card nzTitle="Employee Contributions">
                    <nz-table
                      [nzData]="rollupEmployees"
                      [nzBordered]="true"
                      [nzSize]="'small'"
                      [nzPageSize]="10"
                    >
                      <thead>
                        <tr>
                          <th>Employee</th>
                          <th>Team</th>
                          <th>Allocation</th>
                          <th>Role</th>
                          <th>Man Days</th>
                        </tr>
                      </thead>
                      <tbody>
                        @for (e of rollupEmployees; track e.employeeId) {
                          <tr>
                            <td>{{ e.employeeName }}</td>
                            <td>{{ e.teamName }}</td>
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
            }
          </nz-spin>
        </nz-tab>

        <!-- ── Remaining Tab ── -->
        <nz-tab nzTitle="Remaining">
          <div style="margin-bottom:16px; display:flex; gap:12px; align-items:center; flex-wrap:wrap;">
            <label style="font-weight:500;">From date:</label>
            <nz-date-picker
              [(ngModel)]="remainingDate"
              nzFormat="yyyy-MM-dd"
              nzPlaceHolder="Select date"
            ></nz-date-picker>
            <span style="color:#888; font-size:13px;">Computes remaining capacity from this date to Dec 31.</span>
            <button nz-button nzType="default" (click)="loadRemaining()" [disabled]="!remainingDate">
              <span nz-icon nzType="reload"></span> Load Remaining
            </button>
          </div>
          <nz-spin [nzSpinning]="loadingRemaining">
            @if (remainingResult) {
              <div nz-row [nzGutter]="[16,16]" style="margin-bottom:16px;">
                <div nz-col [nzXs]="24" [nzSm]="8">
                  <app-stat-card title="Remaining Business Days (to Year-End)" [value]="remainingResult.remainingBusinessDays" icon="calendar" color="#1890ff"></app-stat-card>
                </div>
                <div nz-col [nzXs]="24" [nzSm]="8">
                  <app-stat-card title="Total Business Days (Full Year)" [value]="remainingResult.totalBusinessDays" icon="calendar" color="#52c41a"></app-stat-card>
                </div>
                <div nz-col [nzXs]="24" [nzSm]="8">
                  <app-stat-card title="Remaining Man Days (to Year-End)" [value]="formatNum(remainingResult.totalRemaining)" icon="bar-chart" color="#faad14"></app-stat-card>
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

        <!-- ── Simulation Tab ── -->
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
      <nz-alert nzType="info" nzMessage="Please select a team to begin." nzShowIcon></nz-alert>
    }
  `,
})
export class CapacityPageComponent implements OnInit {
  private capacityService = inject(CapacityService);
  private teamService = inject(TeamService);

  teams: TeamDTO[] = [];
  selectedTeamId: string | null = null;

  // Capacity tab
  viewMode: ViewMode = 'year';
  selectedMonth = '';
  selectedYear = new Date().getFullYear();
  availableYears = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - 2 + i);
  loading = false;
  capacityResult: CapacityResultDTO | null = null;

  // Rollup tab
  rollupMonth = '';
  loadingRollup = false;
  rollupResult: RollupCapacityDTO | null = null;
  rollupCategories: CategoryBreakdownItem[] = [];
  rollupEmployees: (EmployeeContribution & { teamName: string })[] = [];

  // Remaining tab
  remainingDate: Date | null = null;
  loadingRemaining = false;
  remainingResult: RemainingCapacityDTO | null = null;

  // Simulation tab
  simulationJson = '{}';
  loadingSimulation = false;
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
    this.loadCapacityForMode();
  }

  onViewModeChange(): void {
    this.capacityResult = null;
  }

  onMonthChange(month: string): void {
    this.selectedMonth = month;
    this.capacityResult = null;
  }

  onRollupMonthChange(month: string): void {
    this.rollupMonth = month;
    this.rollupResult = null;
  }

  loadCapacityForMode(): void {
    if (this.viewMode === 'month') {
      this.loadCapacity();
    } else {
      this.loadPeriod();
    }
  }

  loadCapacity(): void {
    if (!this.selectedTeamId || !this.selectedMonth) return;
    this.loading = true;
    this.capacityService.getTeamCapacity(this.selectedTeamId, this.selectedMonth).subscribe({
      next: r => { this.capacityResult = r; this.loading = false; },
      error: () => { this.loading = false; },
    });
  }

  loadPeriod(): void {
    if (!this.selectedTeamId) return;
    const quarterMonths: Record<string, number[]> = {
      Q1: [1, 2, 3], Q2: [4, 5, 6], Q3: [7, 8, 9], Q4: [10, 11, 12],
      year: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
    };
    const months = quarterMonths[this.viewMode].map(m =>
      `${this.selectedYear}-${String(m).padStart(2, '0')}`
    );
    this.loading = true;
    this.capacityService.getCapacityBulk(this.selectedTeamId, months).subscribe({
      next: results => {
        this.capacityResult = this.aggregateResults(results);
        this.loading = false;
      },
      error: () => { this.loading = false; },
    });
  }

  private aggregateResults(results: CapacityResultDTO[]): CapacityResultDTO {
    const first = results[0];
    const totalCapacity = results.reduce((s, r) => s + r.totalCapacity, 0);

    const catMap = new Map<string, number>();
    results.forEach(r =>
      r.categoryBreakdown.forEach(c =>
        catMap.set(c.categoryName, (catMap.get(c.categoryName) ?? 0) + c.manDays)
      )
    );

    const empMap = new Map<string, EmployeeContribution>();
    results.forEach(r =>
      r.employeeContributions.forEach(e => {
        const ex = empMap.get(e.employeeId);
        if (ex) {
          ex.totalManDays += e.totalManDays;
        } else {
          empMap.set(e.employeeId, { ...e });
        }
      })
    );

    const periodLabel = this.viewMode === 'year'
      ? `${this.selectedYear} – Full Year`
      : `${this.viewMode} ${this.selectedYear}`;

    return {
      teamId: first.teamId,
      teamName: first.teamName,
      month: periodLabel,
      totalCapacity,
      categoryBreakdown: Array.from(catMap, ([categoryName, manDays]) => ({ categoryName, manDays })),
      employeeContributions: Array.from(empMap.values()),
    };
  }

  loadRollup(): void {
    if (!this.selectedTeamId || !this.rollupMonth) return;
    this.loadingRollup = true;
    this.capacityService.getRollupCapacity(this.selectedTeamId, this.rollupMonth).subscribe({
      next: r => {
        this.rollupResult = r;
        this.rollupCategories = this.aggregateRollupCategories(r);
        this.rollupEmployees = this.aggregateRollupEmployees(r);
        this.loadingRollup = false;
      },
      error: () => { this.loadingRollup = false; },
    });
  }

  private aggregateRollupCategories(r: RollupCapacityDTO): CategoryBreakdownItem[] {
    const all = [r.ownCapacity, ...r.subTeamCapacities];
    const catMap = new Map<string, number>();
    all.forEach(c => c.categoryBreakdown.forEach(b =>
      catMap.set(b.categoryName, (catMap.get(b.categoryName) ?? 0) + b.manDays)
    ));
    return Array.from(catMap, ([categoryName, manDays]) => ({ categoryName, manDays }))
      .sort((a, b) => b.manDays - a.manDays);
  }

  private aggregateRollupEmployees(r: RollupCapacityDTO): (EmployeeContribution & { teamName: string })[] {
    const rows: (EmployeeContribution & { teamName: string })[] = [];
    const addFrom = (cap: CapacityResultDTO) =>
      cap.employeeContributions.forEach(e =>
        rows.push({ ...e, teamName: cap.teamName })
      );
    addFrom(r.ownCapacity);
    r.subTeamCapacities.forEach(sc => addFrom(sc));
    return rows.sort((a, b) => b.totalManDays - a.totalManDays);
  }

  loadRemaining(): void {
    if (!this.selectedTeamId || !this.remainingDate) return;
    const d = this.remainingDate;
    const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    this.loadingRemaining = true;
    this.capacityService.getRemainingCapacity(this.selectedTeamId, dateStr).subscribe({
      next: r => { this.remainingResult = r; this.loadingRemaining = false; },
      error: () => { this.loadingRemaining = false; },
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
      error: () => { this.loadingSimulation = false; },
    });
  }
}
