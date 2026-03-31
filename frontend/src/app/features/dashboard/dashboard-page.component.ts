import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { StatCardComponent } from '../../shared/atoms/stat-card/stat-card.component';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { TeamService } from '../../core/services/team.service';
import { EmployeeService } from '../../core/services/employee.service';
import { CapacityService } from '../../core/services/capacity.service';
import { TeamDTO } from '../../core/models/team.model';
import { forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';

interface QuickLink {
  label: string;
  path: string;
  icon: string;
  description: string;
}

interface TeamCapacityRow {
  teamId: string;
  teamName: string;
  totalCapacity: number;
  sharePercent: number;
}

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    NzGridModule,
    NzCardModule,
    NzButtonModule,
    NzIconModule,
    NzSpinModule,
    NzSelectModule,
    NzProgressModule,
    NzTagModule,
    NzEmptyModule,
    StatCardComponent,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header title="Dashboard" subtitle="Capacity Management Overview"></app-page-header>

    <nz-spin [nzSpinning]="loading">
      <div nz-row [nzGutter]="[16, 16]" style="margin-bottom: 24px;">
        <div nz-col [nzXs]="24" [nzSm]="12" [nzMd]="6">
          <app-stat-card
            title="Total Teams"
            [value]="totalTeams"
            icon="team"
            color="#1890ff"
          ></app-stat-card>
        </div>
        <div nz-col [nzXs]="24" [nzSm]="12" [nzMd]="6">
          <app-stat-card
            title="Total Employees"
            [value]="totalEmployees"
            icon="user"
            color="#52c41a"
          ></app-stat-card>
        </div>
        <div nz-col [nzXs]="24" [nzSm]="12" [nzMd]="6">
          <app-stat-card
            title="Active Assignments"
            [value]="activeAssignments"
            icon="schedule"
            color="#faad14"
          ></app-stat-card>
        </div>
        <div nz-col [nzXs]="24" [nzSm]="12" [nzMd]="6">
          <app-stat-card
            title="Avg Allocation %"
            [value]="avgAllocation + '%'"
            icon="bar-chart"
            color="#722ed1"
          ></app-stat-card>
        </div>
      </div>

      <!-- Capacity by Root Team -->
      <div nz-row [nzGutter]="[16, 16]" style="margin-bottom: 24px;">
        <div nz-col [nzXs]="24">
          <nz-card nzTitle="Capacity by Root Team">
            <!-- Filter bar -->
            <div style="display:flex; gap:12px; flex-wrap:wrap; align-items:center; margin-bottom:16px;">
              <label style="font-weight:500;">Year:</label>
              <nz-select [(ngModel)]="capacityYear" style="width:100px;">
                @for (y of availableYears; track y) {
                  <nz-option [nzValue]="y" [nzLabel]="y.toString()"></nz-option>
                }
              </nz-select>

              <label style="font-weight:500;">Root Teams:</label>
              <nz-select
                [(ngModel)]="selectedRootTeamIds"
                nzMode="multiple"
                nzPlaceHolder="All root teams"
                style="min-width:260px; flex:1;"
              >
                @for (t of rootTeams; track t.id) {
                  <nz-option [nzValue]="t.id" [nzLabel]="t.name"></nz-option>
                }
              </nz-select>

              <button nz-button nzType="primary" (click)="loadCapacityPanel()" [nzLoading]="loadingCapacity">
                <span nz-icon nzType="bar-chart"></span> Load
              </button>
            </div>

            <!-- Results -->
            <nz-spin [nzSpinning]="loadingCapacity">
              @if (capacityRows.length > 0) {
                <div style="display:flex; flex-direction:column; gap:12px;">
                  @for (row of capacityRows; track row.teamId) {
                    <div style="display:flex; align-items:center; gap:16px;">
                      <div style="width:180px; font-weight:500; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;"
                           [title]="row.teamName">
                        {{ row.teamName }}
                      </div>
                      <div style="flex:1;">
                        <nz-progress
                          [nzPercent]="row.sharePercent"
                          nzStatus="active"
                          [nzShowInfo]="false"
                          nzStrokeColor="#1890ff"
                        ></nz-progress>
                      </div>
                      <div style="width:160px; text-align:right; white-space:nowrap; color:#555;">
                        <nz-tag nzColor="blue">{{ row.totalCapacity | number:'1.1-1' }} man-days</nz-tag>
                        <span style="color:#aaa; font-size:12px;">{{ row.sharePercent | number:'1.0-0' }}%</span>
                      </div>
                    </div>
                  }
                </div>
                <div style="margin-top:12px; color:#888; font-size:12px; text-align:right;">
                  Total: {{ totalCapacityAcrossTeams | number:'1.1-1' }} man-days — {{ capacityYear }} (rollup incl. sub-teams)
                </div>
              } @else if (!loadingCapacity && capacityLoaded) {
                <nz-empty nzNotFoundContent="No capacity data — configure working days and assignments first"></nz-empty>
              } @else if (!loadingCapacity && !capacityLoaded) {
                <div style="color:#aaa; text-align:center; padding:24px 0;">
                  Select a year and teams, then click Load.
                </div>
              }
            </nz-spin>
          </nz-card>
        </div>
      </div>

      <!-- Quick Access -->
      <div nz-row [nzGutter]="[16, 16]">
        <div nz-col [nzXs]="24">
          <nz-card nzTitle="Quick Access">
            <div nz-row [nzGutter]="[16, 16]">
              @for (link of quickLinks; track link.path) {
                <div nz-col [nzXs]="24" [nzSm]="12" [nzMd]="8" [nzLg]="6">
                  <nz-card
                    class="quick-link-card"
                    [routerLink]="link.path"
                    style="cursor:pointer; transition: box-shadow 0.3s;"
                  >
                    <div class="quick-link-content">
                      <span nz-icon [nzType]="link.icon" style="font-size: 28px; color: #1890ff; margin-bottom: 8px;"></span>
                      <h3 style="margin: 8px 0 4px;">{{ link.label }}</h3>
                      <p style="color: #888; font-size: 13px; margin: 0;">{{ link.description }}</p>
                    </div>
                  </nz-card>
                </div>
              }
            </div>
          </nz-card>
        </div>
      </div>
    </nz-spin>
  `,
  styles: [`
    .quick-link-card:hover {
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    }
    .quick-link-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      padding: 8px 0;
    }
  `],
})
export class DashboardPageComponent implements OnInit {
  private teamService = inject(TeamService);
  private employeeService = inject(EmployeeService);
  private capacityService = inject(CapacityService);

  loading = false;
  totalTeams = 0;
  totalEmployees = 0;
  activeAssignments = 0;
  avgAllocation = 0;

  // Capacity panel
  rootTeams: TeamDTO[] = [];
  selectedRootTeamIds: string[] = [];
  capacityYear = new Date().getFullYear();
  availableYears = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - 2 + i);
  loadingCapacity = false;
  capacityLoaded = false;
  capacityRows: TeamCapacityRow[] = [];
  totalCapacityAcrossTeams = 0;

  quickLinks: QuickLink[] = [
    { label: 'Teams', path: '/teams', icon: 'team', description: 'Manage team hierarchy' },
    { label: 'Employees', path: '/employees', icon: 'user', description: 'View and manage employees' },
    { label: 'Assignments', path: '/assignments', icon: 'schedule', description: 'Track team assignments' },
    { label: 'Capacity', path: '/capacity', icon: 'bar-chart', description: 'Analyze team capacity' },
    { label: 'Snapshots', path: '/snapshots', icon: 'camera', description: 'View capacity snapshots' },
    { label: 'Role Types', path: '/role-types', icon: 'setting', description: 'Configure role types' },
    { label: 'Public Holidays', path: '/public-holidays', icon: 'calendar', description: 'Manage public holidays' },
    { label: 'Working Days', path: '/working-days', icon: 'calendar', description: 'Configure working days' },
    { label: 'Alerts', path: '/alerts', icon: 'bell', description: 'Set capacity alerts' },
    { label: 'Audit Log', path: '/audit', icon: 'audit', description: 'View system audit trail' },
  ];

  ngOnInit(): void {
    this.loading = true;
    forkJoin({
      teams: this.teamService.getTeams(false),
      employees: this.employeeService.getEmployees(),
    }).subscribe({
      next: ({ teams, employees }) => {
        this.totalTeams = teams.length;
        this.totalEmployees = employees.length;
        const allAssignments = employees.flatMap(e => e.assignments ?? []);
        this.activeAssignments = allAssignments.filter(a => !a.endDate).length;
        if (employees.length > 0) {
          const totalAlloc = employees.reduce((sum, e) => sum + (e.totalAllocation ?? 0), 0);
          this.avgAllocation = Math.round(totalAlloc / employees.length);
        }
        this.rootTeams = teams.filter(t => !t.parentId);
        this.selectedRootTeamIds = this.rootTeams.map(t => t.id);
        this.loading = false;
      },
      error: () => { this.loading = false; },
    });
  }

  loadCapacityPanel(): void {
    const teamsToLoad = this.rootTeams.filter(t => this.selectedRootTeamIds.includes(t.id));
    if (teamsToLoad.length === 0) return;

    const months = Array.from({ length: 12 }, (_, i) =>
      `${this.capacityYear}-${String(i + 1).padStart(2, '0')}`
    );

    this.loadingCapacity = true;
    this.capacityLoaded = false;

    const perTeam$ = teamsToLoad.map(team =>
      forkJoin(months.map(m => this.capacityService.getRollupCapacity(team.id, m))).pipe(
        map(results => results.reduce((sum, r) => sum + r.consolidatedTotal, 0))
      )
    );

    forkJoin(perTeam$).subscribe({
      next: capacities => {
        const grand = capacities.reduce((s, v) => s + v, 0);
        this.totalCapacityAcrossTeams = grand;
        this.capacityRows = teamsToLoad
          .map((team, i) => ({
            teamId: team.id,
            teamName: team.name,
            totalCapacity: capacities[i],
            sharePercent: grand > 0 ? Math.round((capacities[i] / grand) * 100) : 0,
          }))
          .sort((a, b) => b.totalCapacity - a.totalCapacity);
        this.loadingCapacity = false;
        this.capacityLoaded = true;
      },
      error: () => { this.loadingCapacity = false; this.capacityLoaded = true; },
    });
  }
}
