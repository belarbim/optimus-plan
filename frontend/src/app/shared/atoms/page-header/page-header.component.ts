import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NzPageHeaderModule } from 'ng-zorro-antd/page-header';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [CommonModule, NzPageHeaderModule, NzButtonModule, NzIconModule],
  template: `
    <nz-page-header
      [nzTitle]="title"
      [nzSubtitle]="subtitle"
      [nzBackIcon]="showBack ? backIconTpl : null"
      (nzBack)="back.emit()"
    >
    </nz-page-header>
    <ng-template #backIconTpl>
      <span nz-icon nzType="arrow-left"></span>
    </ng-template>
  `,
})
export class PageHeaderComponent {
  @Input() title = '';
  @Input() subtitle = '';
  @Input() showBack = false;
  @Output() back = new EventEmitter<void>();
}
