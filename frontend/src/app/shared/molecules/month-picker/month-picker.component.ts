import { Component, Output, EventEmitter, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';

@Component({
  selector: 'app-month-picker',
  standalone: true,
  imports: [CommonModule, FormsModule, NzDatePickerModule],
  template: `
    <nz-date-picker
      nzMode="month"
      nzFormat="yyyy-MM"
      [ngModel]="dateValue"
      (ngModelChange)="onMonthChange($event)"
      [nzPlaceHolder]="placeholder"
      [nzAllowClear]="allowClear"
      style="width: 180px;"
    ></nz-date-picker>
  `,
})
export class MonthPickerComponent {
  @Input() placeholder = 'Select month';
  @Input() allowClear = true;
  @Output() monthChange = new EventEmitter<string>();

  dateValue: Date | null = null;

  onMonthChange(date: Date | null): void {
    this.dateValue = date;
    if (date) {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      this.monthChange.emit(`${year}-${month}`);
    } else {
      this.monthChange.emit('');
    }
  }
}
