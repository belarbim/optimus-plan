import { Component, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { CommonModule } from '@angular/common';

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
        [nzWidth]="200"
        [nzCollapsedWidth]="52"
      >
        <div class="logo">
          @if (!collapsed()) {
            <span class="logo-text">Optimus Plan</span>
          } @else {
            <span class="logo-icon">OP</span>
          }
        </div>

        <ul nz-menu nzTheme="dark" nzMode="inline" [nzInlineIndent]="16">

          <li nz-menu-item [routerLink]="'/dashboard'" routerLinkActive="ant-menu-item-selected">
            <span nz-icon nzType="dashboard"></span>
            <span>Dashboard</span>
          </li>

          <li nz-submenu nzTitle="Organization" nzIcon="team" [nzOpen]="!collapsed()">
            <ul>
              <li nz-menu-item [routerLink]="'/teams'" routerLinkActive="ant-menu-item-selected">
                <span nz-icon nzType="apartment"></span><span>Teams</span>
              </li>
              <li nz-menu-item [routerLink]="'/employees'" routerLinkActive="ant-menu-item-selected">
                <span nz-icon nzType="user"></span><span>Employees</span>
              </li>
              <li nz-menu-item [routerLink]="'/assignments'" routerLinkActive="ant-menu-item-selected">
                <span nz-icon nzType="schedule"></span><span>Assignments</span>
              </li>
              <li nz-menu-item [routerLink]="'/applications'" routerLinkActive="ant-menu-item-selected">
                <span nz-icon nzType="appstore"></span><span>Applications</span>
              </li>
            </ul>
          </li>

          <li nz-submenu nzTitle="Capacity" nzIcon="bar-chart" [nzOpen]="!collapsed()">
            <ul>
              <li nz-menu-item [routerLink]="'/capacity'" routerLinkActive="ant-menu-item-selected">
                <span nz-icon nzType="bar-chart"></span><span>Capacity</span>
              </li>
              <li nz-menu-item [routerLink]="'/working-days'" routerLinkActive="ant-menu-item-selected">
                <span nz-icon nzType="calendar"></span><span>Working Days</span>
              </li>
              <li nz-menu-item [routerLink]="'/public-holidays'" routerLinkActive="ant-menu-item-selected">
                <span nz-icon nzType="calendar"></span><span>Public Holidays</span>
              </li>
              <li nz-menu-item [routerLink]="'/snapshots'" routerLinkActive="ant-menu-item-selected">
                <span nz-icon nzType="camera"></span><span>Snapshots</span>
              </li>
            </ul>
          </li>

          <li nz-submenu nzTitle="Configuration" nzIcon="setting" [nzOpen]="!collapsed()">
            <ul>
              <li nz-menu-item [routerLink]="'/grades'" routerLinkActive="ant-menu-item-selected">
                <span nz-icon nzType="dollar"></span><span>Grades</span>
              </li>
              <li nz-menu-item [routerLink]="'/role-types'" routerLinkActive="ant-menu-item-selected">
                <span nz-icon nzType="tool"></span><span>Role Types</span>
              </li>
            </ul>
          </li>

          <li nz-submenu nzTitle="System" nzIcon="cluster" [nzOpen]="!collapsed()">
            <ul>
              <li nz-menu-item [routerLink]="'/alerts'" routerLinkActive="ant-menu-item-selected">
                <span nz-icon nzType="bell"></span><span>Alerts</span>
              </li>
              <li nz-menu-item [routerLink]="'/audit'" routerLinkActive="ant-menu-item-selected">
                <span nz-icon nzType="audit"></span><span>Audit Log</span>
              </li>
            </ul>
          </li>

        </ul>
      </nz-sider>

      <nz-layout>
        <nz-header class="app-header">
          <button nz-button nzType="text" (click)="collapsed.set(!collapsed())" class="trigger-btn">
            <span nz-icon [nzType]="collapsed() ? 'menu-unfold' : 'menu-fold'" style="font-size:16px; color:#fff"></span>
          </button>
          <span class="header-title">Capacity Management System</span>
        </nz-header>
        <nz-content class="content-area">
          <div class="inner-content">
            <router-outlet />
          </div>
        </nz-content>
        <nz-footer style="text-align:center; background:#f0f2f5; padding: 12px;">
          Optimus Plan &copy; {{ currentYear }}
        </nz-footer>
      </nz-layout>
    </nz-layout>
  `,
  styles: [`
    .logo {
      height: 52px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 14px;
      font-weight: bold;
      border-bottom: 1px solid rgba(255,255,255,0.1);
      overflow: hidden;
      white-space: nowrap;
    }
    .logo-text { padding: 0 12px; }
    .logo-icon { font-size: 16px; }

    .app-header {
      background: #001529;
      display: flex;
      align-items: center;
      padding: 0 16px;
      gap: 12px;
      height: 52px;
      line-height: 52px;
    }
    .trigger-btn { color: #fff; }
    .header-title {
      color: #fff;
      font-size: 14px;
      font-weight: 500;
    }
    .content-area {
      margin: 16px;
    }
    .inner-content {
      padding: 20px;
      background: #fff;
      min-height: 360px;
      border-radius: 4px;
    }

    /* Compact menu items */
    :host ::ng-deep .ant-menu-dark .ant-menu-item {
      height: 34px;
      line-height: 34px;
      font-size: 13px;
      margin: 0;
    }
    :host ::ng-deep .ant-menu-dark .ant-menu-submenu-title {
      height: 36px;
      line-height: 36px;
      font-size: 13px;
      margin: 0;
    }
    :host ::ng-deep .ant-menu-dark.ant-menu-inline .ant-menu-item {
      margin: 0;
    }
  `],
})
export class ShellComponent {
  collapsed = signal(false);
  currentYear = new Date().getFullYear();
}
