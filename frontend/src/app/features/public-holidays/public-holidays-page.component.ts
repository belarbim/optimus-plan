import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { MonthPickerComponent } from '../../shared/molecules/month-picker/month-picker.component';
import { PublicHolidayService } from '../../core/services/public-holiday.service';
import { PublicHolidayDTO } from '../../core/models/public-holiday.model';

@Component({
  selector: 'app-public-holidays-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    NzTableModule,
    NzButtonModule,
    NzModalModule,
    NzFormModule,
    NzInputModule,
    NzSelectModule,
    NzDatePickerModule,
    NzSwitchModule,
    NzPopconfirmModule,
    NzIconModule,
    NzSpinModule,
    NzTagModule,
    NzCheckboxModule,
    PageHeaderComponent,
    MonthPickerComponent,
  ],
  template: `
    <app-page-header title="Public Holidays" subtitle="Manage public holidays by locale"></app-page-header>

    <div style="margin-bottom:16px; display:flex; gap:12px; flex-wrap:wrap; align-items:center; justify-content:space-between;">
      <div style="display:flex; gap:12px; flex-wrap:wrap; align-items:center;">
        <app-month-picker placeholder="Filter by month" (monthChange)="filterMonth = $event; loadHolidays()"></app-month-picker>
        <nz-select [(ngModel)]="filterLocale" nzAllowClear nzPlaceHolder="Filter by locale" style="width:160px" (ngModelChange)="loadHolidays()">
          @for (l of locales; track l) {
            <nz-option [nzValue]="l" [nzLabel]="l"></nz-option>
          }
        </nz-select>
        <button nz-button nzType="default" (click)="filterMonth=''; filterLocale=null; loadHolidays()">
          <span nz-icon nzType="reload"></span> Reset
        </button>
      </div>
      <button nz-button nzType="primary" (click)="openModal()">
        <span nz-icon nzType="plus"></span> Add Holiday
      </button>
    </div>

    <nz-spin [nzSpinning]="loading">
      <nz-table
        #table
        [nzData]="holidays"
        [nzBordered]="true"
        [nzSize]="'middle'"
        [nzPageSize]="15"
      >
        <thead>
          <tr>
            <th>Date</th>
            <th>Name</th>
            <th>Locale</th>
            <th>Recurring</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (h of table.data; track h.id) {
            <tr>
              <td>{{ h.date }}</td>
              <td>{{ h.name }}</td>
              <td><nz-tag>{{ h.locale }}</nz-tag></td>
              <td>
                <nz-tag [nzColor]="h.recurring ? 'green' : 'default'">
                  {{ h.recurring ? 'Recurring' : 'One-time' }}
                </nz-tag>
              </td>
              <td>
                <button nz-button nzType="link" (click)="openModal(h)">
                  <span nz-icon nzType="edit"></span>
                </button>
                <button
                  nz-button nzType="link" nzDanger
                  nz-popconfirm
                  nzPopconfirmTitle="Delete this holiday?"
                  (nzOnConfirm)="deleteHoliday(h.id!)"
                >
                  <span nz-icon nzType="delete"></span>
                </button>
              </td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-spin>

    <nz-modal
      [(nzVisible)]="modalVisible"
      [nzTitle]="editingHoliday ? 'Edit Holiday' : 'Add Holiday'"
      (nzOnCancel)="closeModal()"
      (nzOnOk)="submitForm()"
      [nzOkLoading]="saving"
    >
      <ng-container *nzModalContent>
        <form nz-form [formGroup]="form" nzLayout="vertical">
          <nz-form-item>
            <nz-form-label nzRequired>Date</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-date-picker formControlName="date" nzFormat="yyyy-MM-dd" style="width:100%"></nz-date-picker>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Name</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <input nz-input formControlName="name" placeholder="Holiday name" />
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Locale</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-select formControlName="locale" nzPlaceHolder="Select locale" style="width:100%">
                @for (l of locales; track l) {
                  <nz-option [nzValue]="l" [nzLabel]="l"></nz-option>
                }
              </nz-select>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label>Recurring</nz-form-label>
            <nz-form-control>
              <label nz-checkbox formControlName="recurring">Recurring annually</label>
            </nz-form-control>
          </nz-form-item>
        </form>
      </ng-container>
    </nz-modal>
  `,
})
export class PublicHolidaysPageComponent implements OnInit {
  private holidayService = inject(PublicHolidayService);
  private message = inject(NzMessageService);
  private fb = inject(FormBuilder);

  loading = false;
  saving = false;
  modalVisible = false;
  holidays: PublicHolidayDTO[] = [];
  editingHoliday: PublicHolidayDTO | null = null;
  filterMonth = '';
  filterLocale: string | null = null;
  locales = ['FR', 'US', 'GB', 'DE', 'ES', 'IT', 'PT', 'BE', 'NL', 'CH'];
  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      date: [null, Validators.required],
      name: ['', Validators.required],
      locale: ['FR', Validators.required],
      recurring: [false],
    });
    this.loadHolidays();
  }

  loadHolidays(): void {
    this.loading = true;
    this.holidayService.getHolidays(this.filterMonth || undefined, this.filterLocale || undefined).subscribe({
      next: h => { this.holidays = h; this.loading = false; },
      error: () => { this.message.error('Failed to load'); this.loading = false; },
    });
  }

  openModal(h?: PublicHolidayDTO): void {
    this.editingHoliday = h ?? null;
    if (h) {
      this.form.patchValue({
        date: h.date ? new Date(h.date) : null,
        name: h.name,
        locale: h.locale,
        recurring: h.recurring,
      });
    } else {
      this.form.reset({ locale: 'FR', recurring: false });
    }
    this.modalVisible = true;
  }

  closeModal(): void {
    this.modalVisible = false;
    this.editingHoliday = null;
  }

  submitForm(): void {
    if (this.form.invalid) { Object.values(this.form.controls).forEach(c => { c.markAsDirty(); c.updateValueAndValidity(); }); return; }
    const v = this.form.value;
    const dateStr = v.date instanceof Date ? v.date.toISOString().split('T')[0] : v.date;
    const body = { ...v, date: dateStr };
    this.saving = true;
    const req = this.editingHoliday
      ? this.holidayService.updateHoliday(this.editingHoliday.id!, body)
      : this.holidayService.createHoliday(body);

    req.subscribe({
      next: () => { this.message.success(this.editingHoliday ? 'Updated' : 'Created'); this.saving = false; this.closeModal(); this.loadHolidays(); },
      error: () => { this.message.error('Failed'); this.saving = false; },
    });
  }

  deleteHoliday(id: string): void {
    this.holidayService.deleteHoliday(id).subscribe({
      next: () => { this.message.success('Deleted'); this.loadHolidays(); },
      error: () => this.message.error('Failed to delete'),
    });
  }
}
