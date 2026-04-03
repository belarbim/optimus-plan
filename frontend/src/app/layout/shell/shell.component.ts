import { Component, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { CommonModule } from '@angular/common';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    NzLayoutModule,
    NzMenuModule,
    NzIconModule,
    NzButtonModule,
  ],
  template: `
    <nz-layout style="min-height: 100vh;">
      <nz-sider
        nzCollapsible
        [nzCollapsed]="collapsed()"
        (nzCollapsedChange)="collapsed.set($event)"
        nzTheme="dark"
        [nzWidth]="220"
        [nzCollapsedWidth]="64"
      >
        <div class="logo">
          @if (!collapsed()) {
            <span class="logo-text">Optimus Plan</span>
          } @else {
            <span class="logo-icon">OP</span>
          }
        </div>
        <ul nz-menu nzTheme="dark" nzMode="inline">
          @for (item of navItems; track item.path) {
            <li nz-menu-item [routerLink]="item.path" routerLinkActive="ant-menu-item-selected">
              <span nz-icon [nzType]="item.icon"></span>
              <span>{{ item.label }}</span>
            </li>
          }
        </ul>
      </nz-sider>
      <nz-layout>
        <nz-header class="app-header">
          <button
            nz-button
            nzType="text"
            (click)="collapsed.set(!collapsed())"
            class="trigger-btn"
          >
            <span nz-icon [nzType]="collapsed() ? 'menu-unfold' : 'menu-fold'" style="font-size:18px; color:#fff"></span>
          </button>
          <span class="header-title">Capacity Management System</span>
        </nz-header>
        <nz-content class="content-area">
          <div class="inner-content">
            <router-outlet />
          </div>
        </nz-content>
        <nz-footer style="text-align:center; background:#f0f2f5;">
          Optimus Plan &copy; {{ currentYear }}
        </nz-footer>
      </nz-layout>
    </nz-layout>
  `,
  styles: [`
    .logo {
      height: 64px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 16px;
      font-weight: bold;
      border-bottom: 1px solid rgba(255,255,255,0.1);
      overflow: hidden;
      white-space: nowrap;
    }
    .logo-text { padding: 0 16px; }
    .logo-icon { font-size: 18px; }
    .app-header {
      background: #001529;
      display: flex;
      align-items: center;
      padding: 0 16px;
      gap: 16px;
    }
    .trigger-btn { color: #fff; }
    .header-title {
      color: #fff;
      font-size: 16px;
      font-weight: 500;
    }
    .content-area {
      margin: 24px 16px;
    }
    .inner-content {
      padding: 24px;
      background: #fff;
      min-height: 360px;
      border-radius: 4px;
    }
  `],
})
export class ShellComponent {
  collapsed = signal(false);
  currentYear = new Date().getFullYear();

  navItems: NavItem[] = [
    { path: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
    { path: '/teams', label: 'Teams', icon: 'team' },
    { path: '/employees', label: 'Employees', icon: 'user' },
    { path: '/assignments', label: 'Assignments', icon: 'schedule' },
    { path: '/capacity', label: 'Capacity', icon: 'bar-chart' },
    { path: '/snapshots', label: 'Snapshots', icon: 'camera' },
    { path: '/grades', label: 'Grades', icon: 'dollar' },
    { path: '/role-types', label: 'Role Types', icon: 'setting' },
    { path: '/public-holidays', label: 'Public Holidays', icon: 'calendar' },
    { path: '/working-days', label: 'Working Days', icon: 'calendar' },
    { path: '/alerts', label: 'Alerts', icon: 'bell' },
    { path: '/audit', label: 'Audit Log', icon: 'audit' },
  ];
}
