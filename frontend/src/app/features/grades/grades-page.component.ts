import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';
import { NzTimelineModule } from 'ng-zorro-antd/timeline';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { GradeService } from '../../core/services/grade.service';
import { GradeDTO, GradeCostHistoryDTO } from '../../core/models/grade.model';

@Component({
  selector: 'app-grades-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    NzTableModule,
    NzButtonModule,
    NzModalModule,
    NzFormModule,
    NzInputModule,
    NzInputNumberModule,
    NzPopconfirmModule,
    NzIconModule,
    NzSpinModule,
    NzDrawerModule,
    NzTimelineModule,
    NzDividerModule,
    NzDatePickerModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header title="Grades" subtitle="Manage employee grades and their daily rates"></app-page-header>

    <div style="margin-bottom: 16px; display: flex; justify-content: flex-end;">
      <button nz-button nzType="primary" (click)="openModal()">
        <span nz-icon nzType="plus"></span> Add Grade
      </button>
    </div>

    <nz-spin [nzSpinning]="loading">
      <nz-table
        #table
        [nzData]="grades"
        [nzBordered]="true"
        [nzSize]="'middle'"
        [nzPageSize]="15"
      >
        <thead>
          <tr>
            <th [nzSortFn]="sortByName">Name</th>
            <th [nzSortFn]="sortByCost">Daily Rate (€)</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (grade of table.data; track grade.id) {
            <tr style="cursor: pointer;" (click)="openDrawer(grade)">
              <td>{{ grade.name }}</td>
              <td>{{ grade.dailyCost | number:'1.2-2' }} €/day</td>
              <td (click)="$event.stopPropagation()">
                <button nz-button nzType="link" (click)="openModal(grade)">
                  <span nz-icon nzType="edit"></span>
                </button>
                <button
                  nz-button nzType="link" nzDanger
                  nz-popconfirm
                  nzPopconfirmTitle="Delete this grade? This will affect employees using it."
                  (nzOnConfirm)="deleteGrade(grade.id)"
                >
                  <span nz-icon nzType="delete"></span>
                </button>
              </td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-spin>

    <!-- Add Modal (name + initial dailyCost) -->
    <nz-modal
      [(nzVisible)]="modalVisible"
      [nzTitle]="editingGrade ? 'Edit Grade Name' : 'Add Grade'"
      (nzOnCancel)="closeModal()"
      (nzOnOk)="submitForm()"
      [nzOkLoading]="saving"
    >
      <ng-container *nzModalContent>
        <form nz-form [formGroup]="form" nzLayout="vertical">
          <nz-form-item>
            <nz-form-label nzRequired>Name</nz-form-label>
            <nz-form-control nzErrorTip="Name is required">
              <input nz-input formControlName="name" placeholder="e.g. Junior, Senior, Lead" />
            </nz-form-control>
          </nz-form-item>
          @if (!editingGrade) {
            <nz-form-item>
              <nz-form-label nzRequired>Initial Daily Rate (€)</nz-form-label>
              <nz-form-control nzErrorTip="Daily rate is required">
                <nz-input-number
                  formControlName="dailyCost"
                  [nzMin]="0"
                  [nzStep]="50"
                  [nzPrecision]="2"
                  style="width: 100%"
                  nzPlaceHolder="e.g. 450.00"
                ></nz-input-number>
              </nz-form-control>
            </nz-form-item>
            <nz-form-item>
              <nz-form-label nzRequired>Effective From</nz-form-label>
              <nz-form-control nzErrorTip="Date is required">
                <nz-date-picker formControlName="effectiveFrom" nzFormat="yyyy-MM-dd" style="width:100%"></nz-date-picker>
              </nz-form-control>
            </nz-form-item>
          }
        </form>
      </ng-container>
    </nz-modal>

    <!-- Cost History Drawer -->
    <nz-drawer
      [nzVisible]="drawerVisible"
      [nzTitle]="selectedGrade ? selectedGrade.name + ' — Cost History' : 'Cost History'"
      nzWidth="520"
      (nzOnClose)="closeDrawer()"
    >
      <ng-container *nzDrawerContent>
        @if (selectedGrade) {
          <p style="color:#666; margin-bottom:16px;">
            Current rate: <strong>{{ selectedGrade.dailyCost | number:'1.2-2' }} €/day</strong>
          </p>

          <nz-divider nzText="History" nzOrientation="left"></nz-divider>
          <nz-spin [nzSpinning]="loadingHistory">
            @if (costHistory.length > 0) {
              <nz-timeline>
                @for (entry of costHistory; track entry.id) {
                  <nz-timeline-item>
                    @if (editingCostEntryId === entry.id) {
                      <form nz-form [formGroup]="costEntryEditForm" nzLayout="inline" style="display:inline-flex;gap:4px;align-items:center">
                        <nz-input-number formControlName="dailyCost" [nzMin]="0" [nzStep]="10" [nzPrecision]="2" style="width:120px"></nz-input-number>
                        <nz-date-picker formControlName="effectiveFrom" nzFormat="yyyy-MM-dd" style="width:140px"></nz-date-picker>
                        <button nz-button nzType="primary" nzSize="small" [disabled]="costEntryEditForm.invalid || savingHistory" (click)="saveCostEntry(entry.id)">Save</button>
                        <button nz-button nzSize="small" (click)="cancelCostEntry()">Cancel</button>
                      </form>
                    } @else {
                      <strong>{{ entry.dailyCost | number:'1.2-2' }} €/day</strong>
                      <small style="color:#888"> Effective from: {{ entry.effectiveFrom }}</small>
                      <button nz-button nzType="link" nzSize="small" style="padding:0 4px" (click)="startEditCostEntry(entry)"><span nz-icon nzType="edit"></span></button>
                      <button nz-button nzType="link" nzDanger nzSize="small" style="padding:0 4px"
                        nz-popconfirm nzPopconfirmTitle="Delete this cost entry?"
                        (nzOnConfirm)="deleteCostEntry(entry.id)"><span nz-icon nzType="delete"></span></button>
                    }
                  </nz-timeline-item>
                }
              </nz-timeline>
            } @else {
              <p style="color:#aaa">No cost history yet.</p>
            }
          </nz-spin>

          <nz-divider nzText="Add New Rate" nzOrientation="left"></nz-divider>
          <div style="padding:12px; border:1px dashed #d9d9d9; border-radius:4px;">
            <form nz-form [formGroup]="costHistoryForm" nzLayout="vertical">
              <nz-form-item>
                <nz-form-label nzRequired>Daily Rate (€)</nz-form-label>
                <nz-form-control nzErrorTip="Required">
                  <nz-input-number
                    formControlName="dailyCost"
                    [nzMin]="0"
                    [nzStep]="10"
                    [nzPrecision]="2"
                    style="width: 100%"
                  ></nz-input-number>
                </nz-form-control>
              </nz-form-item>
              <nz-form-item>
                <nz-form-label nzRequired>Effective From</nz-form-label>
                <nz-form-control nzErrorTip="Required">
                  <nz-date-picker formControlName="effectiveFrom" nzFormat="yyyy-MM-dd" style="width:100%"></nz-date-picker>
                </nz-form-control>
              </nz-form-item>
              <button nz-button nzType="primary" [disabled]="costHistoryForm.invalid || savingHistory" (click)="addCostHistory()">
                Add Rate Entry
              </button>
            </form>
          </div>
        }
      </ng-container>
    </nz-drawer>
  `,
})
export class GradesPageComponent implements OnInit {
  private gradeService = inject(GradeService);
  private message = inject(NzMessageService);
  private fb = inject(FormBuilder);

  loading = false;
  saving = false;
  savingHistory = false;
  loadingHistory = false;
  modalVisible = false;
  drawerVisible = false;
  grades: GradeDTO[] = [];
  editingGrade: GradeDTO | null = null;
  selectedGrade: GradeDTO | null = null;
  costHistory: GradeCostHistoryDTO[] = [];

  form!: FormGroup;
  costHistoryForm!: FormGroup;
  costEntryEditForm!: FormGroup;
  editingCostEntryId: string | null = null;

  sortByName = (a: GradeDTO, b: GradeDTO) => a.name.localeCompare(b.name);
  sortByCost = (a: GradeDTO, b: GradeDTO) => a.dailyCost - b.dailyCost;

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', Validators.required],
      dailyCost: [null],
      effectiveFrom: [null],
    });
    this.costHistoryForm = this.fb.group({
      dailyCost: [null, [Validators.required, Validators.min(0)]],
      effectiveFrom: [null, Validators.required],
    });
    this.costEntryEditForm = this.fb.group({
      dailyCost: [null, [Validators.required, Validators.min(0)]],
      effectiveFrom: [null, Validators.required],
    });
    this.loadGrades();
  }

  loadGrades(): void {
    this.loading = true;
    this.gradeService.getAll().subscribe({
      next: grades => { this.grades = grades; this.loading = false; },
      error: () => { this.message.error('Failed to load grades'); this.loading = false; },
    });
  }

  openModal(grade?: GradeDTO): void {
    this.editingGrade = grade ?? null;
    if (grade) {
      this.form.patchValue({ name: grade.name, dailyCost: null, effectiveFrom: null });
      this.form.get('dailyCost')?.clearValidators();
      this.form.get('dailyCost')?.updateValueAndValidity();
      this.form.get('effectiveFrom')?.clearValidators();
      this.form.get('effectiveFrom')?.updateValueAndValidity();
    } else {
      this.form.reset();
      this.form.get('dailyCost')?.setValidators([Validators.required, Validators.min(0)]);
      this.form.get('dailyCost')?.updateValueAndValidity();
      this.form.get('effectiveFrom')?.setValidators([Validators.required]);
      this.form.get('effectiveFrom')?.updateValueAndValidity();
    }
    this.modalVisible = true;
  }

  closeModal(): void {
    this.modalVisible = false;
    this.editingGrade = null;
    this.form.reset();
  }

  submitForm(): void {
    if (this.form.invalid) {
      Object.values(this.form.controls).forEach(c => { c.markAsDirty(); c.updateValueAndValidity(); });
      return;
    }
    this.saving = true;
    const val = this.form.value;
    const effectiveFrom = val.effectiveFrom instanceof Date
      ? val.effectiveFrom.toISOString().split('T')[0]
      : val.effectiveFrom;
    const req = this.editingGrade
      ? this.gradeService.update(this.editingGrade.id, { name: val.name })
      : this.gradeService.create({ name: val.name, dailyCost: val.dailyCost, effectiveFrom });

    req.subscribe({
      next: () => {
        this.message.success(this.editingGrade ? 'Grade updated' : 'Grade created');
        this.saving = false;
        this.closeModal();
        this.loadGrades();
      },
      error: () => {
        this.message.error('Operation failed');
        this.saving = false;
      },
    });
  }

  deleteGrade(id: string): void {
    this.gradeService.delete(id).subscribe({
      next: () => { this.message.success('Grade deleted'); this.loadGrades(); },
      error: () => this.message.error('Failed to delete grade'),
    });
  }

  openDrawer(grade: GradeDTO): void {
    this.selectedGrade = grade;
    this.drawerVisible = true;
    this.costHistory = [];
    this.costHistoryForm.reset();
    this.loadCostHistory(grade.id);
  }

  closeDrawer(): void {
    this.drawerVisible = false;
    this.selectedGrade = null;
    this.costHistory = [];
    this.costHistoryForm.reset();
    this.editingCostEntryId = null;
    this.costEntryEditForm.reset();
  }

  loadCostHistory(gradeId: string): void {
    this.loadingHistory = true;
    this.gradeService.getCostHistory(gradeId).subscribe({
      next: h => { this.costHistory = h; this.loadingHistory = false; },
      error: () => { this.message.error('Failed to load cost history'); this.loadingHistory = false; },
    });
  }

  startEditCostEntry(entry: GradeCostHistoryDTO): void {
    this.editingCostEntryId = entry.id;
    const date = entry.effectiveFrom ? new Date(entry.effectiveFrom) : null;
    this.costEntryEditForm.setValue({ dailyCost: entry.dailyCost, effectiveFrom: date });
  }

  cancelCostEntry(): void {
    this.editingCostEntryId = null;
    this.costEntryEditForm.reset();
  }

  saveCostEntry(entryId: string): void {
    if (this.costEntryEditForm.invalid || !this.selectedGrade) return;
    this.savingHistory = true;
    const val = this.costEntryEditForm.value;
    const effectiveFrom = val.effectiveFrom instanceof Date
      ? val.effectiveFrom.toISOString().split('T')[0]
      : val.effectiveFrom;
    this.gradeService.updateCostHistory(this.selectedGrade.id, entryId, { dailyCost: val.dailyCost, effectiveFrom }).subscribe({
      next: () => {
        this.message.success('Cost entry updated');
        this.savingHistory = false;
        this.editingCostEntryId = null;
        this.costEntryEditForm.reset();
        if (this.selectedGrade) {
          this.loadCostHistory(this.selectedGrade.id);
          this.loadGrades();
        }
      },
      error: () => { this.message.error('Failed to update cost entry'); this.savingHistory = false; },
    });
  }

  deleteCostEntry(entryId: string): void {
    if (!this.selectedGrade) return;
    this.gradeService.deleteCostHistory(this.selectedGrade.id, entryId).subscribe({
      next: () => {
        this.message.success('Cost entry deleted');
        if (this.selectedGrade) {
          this.loadCostHistory(this.selectedGrade.id);
          this.loadGrades();
        }
      },
      error: () => this.message.error('Failed to delete cost entry'),
    });
  }

  addCostHistory(): void {
    if (this.costHistoryForm.invalid || !this.selectedGrade) return;
    this.savingHistory = true;
    const val = this.costHistoryForm.value;
    const effectiveFrom = val.effectiveFrom instanceof Date
      ? val.effectiveFrom.toISOString().split('T')[0]
      : val.effectiveFrom;
    this.gradeService.addCostHistory(this.selectedGrade.id, { dailyCost: val.dailyCost, effectiveFrom }).subscribe({
      next: () => {
        this.message.success('Cost entry added');
        this.savingHistory = false;
        this.costHistoryForm.reset();
        if (this.selectedGrade) {
          this.loadCostHistory(this.selectedGrade.id);
          this.loadGrades();
        }
      },
      error: () => {
        this.message.error('Failed to add cost entry');
        this.savingHistory = false;
      },
    });
  }
}
