import { Routes } from '@angular/router';
import { ShellComponent } from './layout/shell/shell.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard-page.component').then(m => m.DashboardPageComponent),
      },
      {
        path: 'teams',
        loadComponent: () =>
          import('./features/teams/teams-page.component').then(m => m.TeamsPageComponent),
      },
      {
        path: 'applications',
        loadComponent: () =>
          import('./features/applications/applications-page.component').then(m => m.ApplicationsPageComponent),
      },
      {
        path: 'employees',
        loadComponent: () =>
          import('./features/employees/employees-page.component').then(m => m.EmployeesPageComponent),
      },
      {
        path: 'assignments',
        loadComponent: () =>
          import('./features/assignments/assignments-page.component').then(m => m.AssignmentsPageComponent),
      },
      {
        path: 'capacity',
        loadComponent: () =>
          import('./features/capacity/capacity-page.component').then(m => m.CapacityPageComponent),
      },
      {
        path: 'snapshots',
        loadComponent: () =>
          import('./features/snapshots/snapshots-page.component').then(m => m.SnapshotsPageComponent),
      },
      {
        path: 'grades',
        loadComponent: () =>
          import('./features/grades/grades-page.component').then(m => m.GradesPageComponent),
      },
      {
        path: 'role-types',
        loadComponent: () =>
          import('./features/role-types/role-types-page.component').then(m => m.RoleTypesPageComponent),
      },
      {
        path: 'public-holidays',
        loadComponent: () =>
          import('./features/public-holidays/public-holidays-page.component').then(m => m.PublicHolidaysPageComponent),
      },
      {
        path: 'working-days',
        loadComponent: () =>
          import('./features/working-days/working-days-page.component').then(m => m.WorkingDaysPageComponent),
      },
      {
        path: 'alerts',
        loadComponent: () =>
          import('./features/alerts/alerts-page.component').then(m => m.AlertsPageComponent),
      },
      {
        path: 'audit',
        loadComponent: () =>
          import('./features/audit/audit-page.component').then(m => m.AuditPageComponent),
      },
    ],
  },
];
