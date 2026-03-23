import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzStatisticModule } from 'ng-zorro-antd/statistic';
import { NzIconModule } from 'ng-zorro-antd/icon';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule, NzCardModule, NzStatisticModule, NzIconModule],
  template: `
    <nz-card [nzBodyStyle]="{'padding': '20px'}">
      <div class="stat-wrapper">
        <div class="stat-icon" [style.background]="color + '1a'" [style.color]="color">
          <span nz-icon [nzType]="icon" nzTheme="outline" style="font-size:24px"></span>
        </div>
        <div class="stat-content">
          <nz-statistic [nzTitle]="title" [nzValue]="value"></nz-statistic>
        </div>
      </div>
    </nz-card>
  `,
  styles: [`
    .stat-wrapper {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .stat-content { flex: 1; }
  `],
})
export class StatCardComponent {
  @Input() title = '';
  @Input() value: string | number = 0;
  @Input() icon = 'dashboard';
  @Input() color = '#1890ff';
}
