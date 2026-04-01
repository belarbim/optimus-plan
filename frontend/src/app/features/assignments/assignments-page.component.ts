import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { AssignmentService } from '../../core/services/assignment.service';
import { TeamService } from '../../core/services/team.service';
import { EmployeeService } from '../../core/services/employee.service';
import { RoleTypeService } from '../../core/services/role-type.service';
import { TeamAssignmentDTO } from '../../core/models/assignment.model';
import { TeamDTO } from '../../core/models/team.model';
import { EmployeeDTO } from '../../core/models/employee.model';
import { RoleTypeConfigDTO } from '../../core/models/role-type.model';
import { forkJoin } from 'rxjs';


@Component({
  selector: 'app-assignments-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    NzTableModule,
    NzButtonModule,
    NzModalModule,
    NzFormModule,
    NzInputModule,
    NzSelectModule,
    NzDatePickerModule,
    NzInputNumberModule,
    NzPopconfirmModule,
    NzIconModule,
    NzSpinModule,
    NzTagModule,
    NzDividerModule,
    PageHeaderComponent,
    FormsModule,
  ],
  template: `
    <app-page-header title="Assignments" subtitle="Manage team assignments"></app-page-header>

    <div style="margin-bottom:16px; display:flex; gap:12px; flex-wrap:wrap; align-items:center; justify-content:space-between;">
      <div style="display:flex; gap:12px; flex-wrap:wrap;">
        <nz-select
          nzAllowClear
          nzPlaceHolder="Filter by Team"
          style="width:200px"
          [(ngModel)]="filterTeamId"
          (ngModelChange)="onFilterChange()"
        >
          @for (t of teams; track t.id) {
            <nz-option [nzValue]="t.id" [nzLabel]="t.name"></nz-option>
          }
        </nz-select>
        <nz-select
          nzAllowClear
          nzPlaceHolder="Filter by Employee"
          style="width:220px"
          [(ngModel)]="filterEmployeeId"
          (ngModelChange)="onFilterChange()"
        >
          @for (e of employees; track e.id) {
            <nz-option [nzValue]="e.id" [nzLabel]="e.firstName + ' ' + e.lastName"></nz-option>
          }
        </nz-select>
      </div>
      <div style="display:flex; gap:8px;">
        <button nz-button (click)="openImportModal()">
          <span nz-icon nzType="upload"></span> Import CSV
        </button>
        <button nz-button nzType="primary" (click)="openCreateModal()">
          <span nz-icon nzType="plus"></span> New Assignment
        </button>
      </div>
    </div>

    <nz-spin [nzSpinning]="loading">
      <nz-table
        #table
        [nzData]="filteredAssignments"
        [nzBordered]="true"
        [nzSize]="'middle'"
        [nzPageSize]="15"
      >
        <thead>
          <tr>
            <th>Employee</th>
            <th>Team</th>
            <th>Allocation %</th>
            <th>Role</th>
            <th>Weight</th>
            <th>Start Date</th>
            <th>End Date</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (a of table.data; track a.id) {
            <tr>
              <td>{{ a.employeeName }}</td>
              <td>{{ a.teamName }}</td>
              <td>
                <nz-tag [nzColor]="a.allocationPct >= 100 ? 'green' : 'blue'">{{ a.allocationPct }}%</nz-tag>
              </td>
              <td>{{ a.roleType }}</td>
              <td>{{ a.roleWeight }}</td>
              <td>{{ a.startDate }}</td>
              <td>
                @if (a.endDate) {
                  <nz-tag nzColor="error">{{ a.endDate }}</nz-tag>
                } @else {
                  <nz-tag nzColor="success">Active</nz-tag>
                }
              </td>
              <td>
                <button nz-button nzType="link" nzSize="small" (click)="openEditModal(a)" title="Edit Assignment">
                  <span nz-icon nzType="edit"></span>
                </button>
                @if (!a.endDate) {
                  <nz-divider nzType="vertical"></nz-divider>
                  <button nz-button nzType="link" nzSize="small" (click)="openRoleModal(a)" title="Change Role">
                    <span nz-icon nzType="setting"></span>
                  </button>
                }
                <nz-divider nzType="vertical"></nz-divider>
                <button nz-button nzType="link" nzSize="small" nzDanger
                  nz-popconfirm nzPopconfirmTitle="Delete this assignment?"
                  (nzOnConfirm)="deleteAssignment(a)" title="Delete">
                  <span nz-icon nzType="delete"></span>
                </button>
              </td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-spin>

    <!-- Create Assignment Modal -->
    <nz-modal
      [(nzVisible)]="createModalVisible"
      nzTitle="New Assignment"
      (nzOnCancel)="createModalVisible = false"
      (nzOnOk)="submitCreate()"
      [nzOkLoading]="saving"
    >
      <ng-container *nzModalContent>
        <form nz-form [formGroup]="createForm" nzLayout="vertical">
          <nz-form-item>
            <nz-form-label nzRequired>Employee</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-select formControlName="employeeId" nzPlaceHolder="Select employee" style="width:100%">
                @for (e of employees; track e.id) {
                  <nz-option [nzValue]="e.id" [nzLabel]="e.firstName + ' ' + e.lastName"></nz-option>
                }
              </nz-select>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Team</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-select formControlName="teamId" nzPlaceHolder="Select team" style="width:100%">
                @for (t of teams; track t.id) {
                  <nz-option [nzValue]="t.id" [nzLabel]="t.name"></nz-option>
                }
              </nz-select>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Allocation %</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-input-number formControlName="allocationPct" [nzMin]="1" [nzMax]="100" style="width:100%"></nz-input-number>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Role Type</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-select formControlName="roleType" nzPlaceHolder="Select role" style="width:100%">
                @for (r of roleTypes; track r.id) {
                  <nz-option [nzValue]="r.roleType" [nzLabel]="r.roleType"></nz-option>
                }
              </nz-select>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Role Weight</nz-form-label>
            <nz-form-control nzErrorTip="Required" nzExtra="Auto-filled from role type — you can override it">
              <nz-input-number formControlName="roleWeight" [nzMin]="0" [nzMax]="1" [nzStep]="0.1" style="width:100%"></nz-input-number>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Start Date</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-date-picker formControlName="startDate" nzFormat="yyyy-MM-dd" style="width:100%"></nz-date-picker>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label>End Date</nz-form-label>
            <nz-form-control nzExtra="Optional — leave blank for open-ended assignment">
              <nz-date-picker formControlName="endDate" nzFormat="yyyy-MM-dd" style="width:100%" nzAllowClear></nz-date-picker>
            </nz-form-control>
          </nz-form-item>
        </form>
      </ng-container>
    </nz-modal>

    <!-- Edit Assignment Modal -->
    <nz-modal
      [(nzVisible)]="editModalVisible"
      nzTitle="Edit Assignment"
      (nzOnCancel)="editModalVisible = false"
      (nzOnOk)="submitEdit()"
      [nzOkLoading]="saving"
    >
      <ng-container *nzModalContent>
        <form nz-form [formGroup]="editForm" nzLayout="vertical">
          <nz-form-item>
            <nz-form-label nzRequired>Team</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-select formControlName="teamId" nzPlaceHolder="Select team" style="width:100%">
                @for (t of teams; track t.id) {
                  <nz-option [nzValue]="t.id" [nzLabel]="t.name"></nz-option>
                }
              </nz-select>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Allocation %</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-input-number formControlName="allocationPct" [nzMin]="1" [nzMax]="100" style="width:100%"></nz-input-number>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Role Type</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-select formControlName="roleType" nzPlaceHolder="Select role" style="width:100%">
                @for (r of roleTypes; track r.id) {
                  <nz-option [nzValue]="r.roleType" [nzLabel]="r.roleType"></nz-option>
                }
              </nz-select>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Role Weight</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-input-number formControlName="roleWeight" [nzMin]="0" [nzMax]="1" [nzStep]="0.1" style="width:100%"></nz-input-number>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Start Date</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-date-picker formControlName="startDate" nzFormat="yyyy-MM-dd" style="width:100%"></nz-date-picker>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label>End Date</nz-form-label>
            <nz-form-control nzExtra="Leave blank to keep the assignment open-ended">
              <nz-date-picker formControlName="endDate" nzFormat="yyyy-MM-dd" style="width:100%" nzAllowClear></nz-date-picker>
            </nz-form-control>
          </nz-form-item>
        </form>
      </ng-container>
    </nz-modal>

    <!-- Change Role Modal -->
    <nz-modal
      [(nzVisible)]="roleModalVisible"
      nzTitle="Change Role"
      (nzOnCancel)="roleModalVisible = false"
      (nzOnOk)="submitRole()"
      [nzOkLoading]="saving"
    >
      <ng-container *nzModalContent>
        <form nz-form [formGroup]="roleForm" nzLayout="vertical">
          <nz-form-item>
            <nz-form-label nzRequired>Role Type</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-select formControlName="roleType" nzPlaceHolder="Select role" style="width:100%">
                @for (r of roleTypes; track r.id) {
                  <nz-option [nzValue]="r.roleType" [nzLabel]="r.roleType"></nz-option>
                }
              </nz-select>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Role Weight</nz-form-label>
            <nz-form-control nzErrorTip="Required" nzExtra="Auto-filled from role type — you can override it">
              <nz-input-number formControlName="roleWeight" [nzMin]="0" [nzMax]="1" [nzStep]="0.1" style="width:100%"></nz-input-number>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Effective From</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-date-picker formControlName="effectiveFrom" nzFormat="yyyy-MM-dd" style="width:100%"></nz-date-picker>
            </nz-form-control>
          </nz-form-item>
        </form>
      </ng-container>
    </nz-modal>

    <!-- Import CSV Modal -->
    <nz-modal
      [(nzVisible)]="importModalVisible"
      nzTitle="Import Assignments from CSV"
      (nzOnCancel)="importModalVisible = false"
      [nzFooter]="null"
    >
      <ng-container *nzModalContent>
        <p style="margin-bottom:8px;">
          Upload a CSV file with the following columns (header row required):
        </p>
        <code style="display:block; background:#f5f5f5; padding:8px; border-radius:4px; font-size:12px; margin-bottom:16px;">
          employeeEmail, teamName, allocationPct, roleType, roleWeight, startDate, endDate
        </code>
        <p style="font-size:12px; color:#888; margin-bottom:16px;">
          startDate and endDate format: yyyy-MM-dd. endDate is optional.
        </p>

        <button nz-button style="margin-bottom:16px;" (click)="downloadTemplate()">
          <span nz-icon nzType="download"></span> Download Template
        </button>

        <div style="border:2px dashed #d9d9d9; border-radius:4px; padding:24px; text-align:center; cursor:pointer;"
             (click)="fileInput.click()" (dragover)="$event.preventDefault()" (drop)="onFileDrop($event)">
          <span nz-icon nzType="inbox" style="font-size:32px; color:#40a9ff;"></span>
          <p style="margin:8px 0 4px;">Click or drag CSV file here</p>
          <p style="font-size:12px; color:#888;">{{ importFile ? importFile.name : 'No file selected' }}</p>
        </div>
        <input #fileInput type="file" accept=".csv" style="display:none" (change)="onFileSelect($event)">

        @if (importResult) {
          <div style="margin-top:16px;">
            <p>
              <span nz-icon nzType="check-circle" style="color:#52c41a;"></span>
              {{ importResult.successCount }} imported successfully
              @if (importResult.errorCount > 0) {
                &nbsp;·&nbsp;
                <span nz-icon nzType="close-circle" style="color:#ff4d4f;"></span>
                {{ importResult.errorCount }} failed
              }
            </p>
            @if (importResult.errors.length > 0) {
              <ul style="font-size:12px; color:#ff4d4f; max-height:150px; overflow-y:auto; padding-left:16px;">
                @for (e of importResult.errors; track e) {
                  <li>{{ e }}</li>
                }
              </ul>
            }
          </div>
        }

        <div style="margin-top:16px; display:flex; justify-content:flex-end; gap:8px;">
          <button nz-button (click)="importModalVisible = false">Cancel</button>
          <button nz-button nzType="primary" [disabled]="!importFile" [nzLoading]="importing" (click)="submitImport()">
            Import
          </button>
        </div>
      </ng-container>
    </nz-modal>
  `,
})
export class AssignmentsPageComponent implements OnInit {
  private assignmentService = inject(AssignmentService);
  private teamService = inject(TeamService);
  private employeeService = inject(EmployeeService);
  private roleTypeService = inject(RoleTypeService);
  private message = inject(NzMessageService);
  private fb = inject(FormBuilder);

  loading = false;
  saving = false;
  assignments: TeamAssignmentDTO[] = [];
  filteredAssignments: TeamAssignmentDTO[] = [];
  teams: TeamDTO[] = [];
  employees: EmployeeDTO[] = [];
  roleTypes: RoleTypeConfigDTO[] = [];

  filterTeamId: string | null = null;
  filterEmployeeId: string | null = null;
  selectedAssignment: TeamAssignmentDTO | null = null;

  createModalVisible = false;
  editModalVisible = false;
  roleModalVisible = false;
  importModalVisible = false;
  importFile: File | null = null;
  importing = false;
  importResult: { successCount: number; errorCount: number; errors: string[] } | null = null;

  createForm!: FormGroup;
  editForm!: FormGroup;
  roleForm!: FormGroup;

  ngOnInit(): void {
    this.createForm = this.fb.group({
      employeeId: [null, Validators.required],
      teamId: [null, Validators.required],
      allocationPct: [100, Validators.required],
      roleType: [null, Validators.required],
      roleWeight: [1, Validators.required],
      startDate: [null, Validators.required],
      endDate: [null],
    });
    this.editForm = this.fb.group({
      teamId: [null, Validators.required],
      allocationPct: [null, Validators.required],
      roleType: [null, Validators.required],
      roleWeight: [null, Validators.required],
      startDate: [null, Validators.required],
      endDate: [null],
    });
    this.editForm.get('roleType')!.valueChanges.subscribe(roleType => {
      if (roleType) {
        const weight = this.roleTypes.find(r => r.roleType === roleType)?.defaultWeight ?? 1;
        this.editForm.get('roleWeight')!.setValue(weight, { emitEvent: false });
      }
    });
    this.roleForm = this.fb.group({
      roleType: [null, Validators.required],
      roleWeight: [1, Validators.required],
      effectiveFrom: [null, Validators.required],
    });

    this.createForm.get('roleType')!.valueChanges.subscribe(roleType => {
      if (roleType) {
        const weight = this.roleTypes.find(r => r.roleType === roleType)?.defaultWeight ?? 1;
        this.createForm.get('roleWeight')!.setValue(weight, { emitEvent: false });
      }
    });

    this.roleForm.get('roleType')!.valueChanges.subscribe(roleType => {
      if (roleType) {
        const weight = this.roleTypes.find(r => r.roleType === roleType)?.defaultWeight ?? 1;
        this.roleForm.get('roleWeight')!.setValue(weight, { emitEvent: false });
      }
    });

    forkJoin({
      teams: this.teamService.getTeams(),
      employees: this.employeeService.getEmployees(),
      roleTypes: this.roleTypeService.getRoleTypes(),
    }).subscribe({
      next: ({ teams, employees, roleTypes }) => {
        this.teams = teams;
        this.employees = employees;
        this.roleTypes = roleTypes;
        this.loadAllAssignments();
      },
    });
  }

  loadAllAssignments(): void {
    this.loading = true;
    if (this.filterTeamId) {
      this.assignmentService.getByTeam(this.filterTeamId).subscribe({
        next: a => { this.assignments = a; this.filteredAssignments = a; this.loading = false; },
        error: () => { this.message.error('Failed to load'); this.loading = false; },
      });
    } else if (this.filterEmployeeId) {
      this.assignmentService.getByEmployee(this.filterEmployeeId).subscribe({
        next: a => { this.assignments = a; this.filteredAssignments = a; this.loading = false; },
        error: () => { this.message.error('Failed to load'); this.loading = false; },
      });
    } else {
      // Load all by fetching for each team
      const teamRequests = this.teams.map(t => this.assignmentService.getByTeam(t.id));
      if (teamRequests.length === 0) { this.loading = false; return; }
      forkJoin(teamRequests).subscribe({
        next: results => {
          const seen = new Set<string>();
          const all: TeamAssignmentDTO[] = [];
          results.flat().forEach(a => { if (!seen.has(a.id)) { seen.add(a.id); all.push(a); } });
          this.assignments = all;
          this.filteredAssignments = all;
          this.loading = false;
        },
        error: () => { this.message.error('Failed to load assignments'); this.loading = false; },
      });
    }
  }

  onFilterChange(): void {
    this.loadAllAssignments();
  }

  formatDate(date: Date | null): string {
    if (!date) return '';
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  openCreateModal(): void {
    this.createForm.reset({ allocationPct: 100, roleWeight: 1 });
    this.createModalVisible = true;
  }

  openEditModal(a: TeamAssignmentDTO): void {
    this.selectedAssignment = a;
    this.editForm.reset({
      teamId: a.teamId,
      allocationPct: a.allocationPct,
      roleType: a.roleType,
      roleWeight: a.roleWeight,
      startDate: a.startDate ? new Date(a.startDate + 'T00:00:00') : null,
      endDate: a.endDate ? new Date(a.endDate + 'T00:00:00') : null,
    }, { emitEvent: false });
    this.editModalVisible = true;
  }

  openRoleModal(a: TeamAssignmentDTO): void {
    this.selectedAssignment = a;
    this.roleForm.patchValue({ roleType: a.roleType, roleWeight: a.roleWeight, effectiveFrom: null }, { emitEvent: false });
    this.roleModalVisible = true;
  }

  submitCreate(): void {
    if (this.createForm.invalid) { Object.values(this.createForm.controls).forEach(c => { c.markAsDirty(); c.updateValueAndValidity(); }); return; }
    const v = this.createForm.value;
    const endDateStr = v.endDate ? this.formatDate(v.endDate) : null;
    const body = { ...v, startDate: this.formatDate(v.startDate), endDate: endDateStr };
    this.saving = true;
    this.assignmentService.createAssignment(body).subscribe({
      next: () => { this.message.success('Assignment created'); this.saving = false; this.createModalVisible = false; this.loadAllAssignments(); },
      error: () => { this.message.error('Failed to create'); this.saving = false; },
    });
  }

  submitEdit(): void {
    if (this.editForm.invalid || !this.selectedAssignment) return;
    const v = this.editForm.value;
    const body = {
      teamId: v.teamId,
      allocationPct: v.allocationPct,
      roleType: v.roleType,
      roleWeight: v.roleWeight,
      startDate: this.formatDate(v.startDate),
      endDate: v.endDate ? this.formatDate(v.endDate) : null,
    };
    this.saving = true;
    this.assignmentService.updateAssignment(this.selectedAssignment.id, body).subscribe({
      next: () => { this.message.success('Assignment updated'); this.saving = false; this.editModalVisible = false; this.loadAllAssignments(); },
      error: () => { this.message.error('Failed to update'); this.saving = false; },
    });
  }

  deleteAssignment(a: TeamAssignmentDTO): void {
    this.assignmentService.deleteAssignment(a.id).subscribe({
      next: () => { this.message.success('Assignment deleted'); this.loadAllAssignments(); },
      error: () => this.message.error('Failed to delete'),
    });
  }

  submitRole(): void {
    if (this.roleForm.invalid || !this.selectedAssignment) return;
    const v = this.roleForm.value;
    const body = { ...v, effectiveFrom: this.formatDate(v.effectiveFrom) };
    this.saving = true;
    this.assignmentService.updateRole(this.selectedAssignment.id, body).subscribe({
      next: () => { this.message.success('Role updated'); this.saving = false; this.roleModalVisible = false; this.loadAllAssignments(); },
      error: () => { this.message.error('Failed'); this.saving = false; },
    });
  }

  openImportModal(): void {
    this.importFile = null;
    this.importResult = null;
    this.importModalVisible = true;
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      this.importFile = input.files[0];
      this.importResult = null;
    }
  }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file) { this.importFile = file; this.importResult = null; }
  }

  submitImport(): void {
    if (!this.importFile) return;
    this.importing = true;
    this.assignmentService.importCsv(this.importFile).subscribe({
      next: result => {
        this.importResult = result;
        this.importing = false;
        if (result.successCount > 0) this.loadAllAssignments();
      },
      error: () => { this.message.error('Import failed'); this.importing = false; },
    });
  }

  downloadTemplate(): void {
    const csv = 'employeeEmail,teamName,allocationPct,roleType,roleWeight,startDate,endDate\njohn.doe@example.com,Team Alpha,100,Developer,0.8,2024-01-01,\n';
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'assignments-template.csv';
    a.click();
    URL.revokeObjectURL(url);
  }
}
