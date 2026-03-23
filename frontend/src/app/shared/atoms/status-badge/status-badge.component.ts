import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzBadgeModule } from 'ng-zorro-antd/badge';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule, NzTagModule, NzBadgeModule],
  template: `
    <nz-tag [nzColor]="color">{{ label }}</nz-tag>
  `,
})
export class StatusBadgeComponent {
  @Input() set status(value: string) {
    switch (value?.toLowerCase()) {
      case 'active':
        this.color = 'success';
        this.label = 'Active';
        break;
      case 'inactive':
        this.color = 'default';
        this.label = 'Inactive';
        break;
      case 'pending':
        this.color = 'warning';
        this.label = 'Pending';
        break;
      case 'ended':
        this.color = 'error';
        this.label = 'Ended';
        break;
      default:
        this.color = 'default';
        this.label = value ?? 'Unknown';
    }
  }

  color = 'default';
  label = 'Unknown';
}
