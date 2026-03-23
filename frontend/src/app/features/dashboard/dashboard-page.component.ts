import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { StatCardComponent } from '../../shared/atoms/stat-card/stat-card.component';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { TeamService } from '../../core/services/team.service';
import { EmployeeService } from '../../core/services/employee.service';
import { forkJoin } from 'rxjs';

interface QuickLink {
  label: string;
  path: string;
  icon: string;
  description: string;
}

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    NzGridModule,
    NzCardModule,
    NzButtonModule,
    NzIconModule,
    NzSpinModule,
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

  loading = false;
  totalTeams = 0;
  totalEmployees = 0;
  activeAssignments = 0;
  avgAllocation = 0;

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
        this.loading = false;
      },
      error: () => { this.loading = false; },
    });
  }
}
