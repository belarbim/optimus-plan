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
      <button nz-button nzType="primary" (click)="openCreateModal()">
        <span nz-icon nzType="plus"></span> New Assignment
      </button>
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
                @if (!a.endDate) {
                  <button nz-button nzType="link" nzSize="small" (click)="openEndModal(a)" title="End Assignment">
                    <span nz-icon nzType="close-circle"></span>
                  </button>
                  <nz-divider nzType="vertical"></nz-divider>
                  <button nz-button nzType="link" nzSize="small" (click)="openAllocationModal(a)" title="Update Allocation">
                    <span nz-icon nzType="edit"></span>
                  </button>
                  <nz-divider nzType="vertical"></nz-divider>
                  <button nz-button nzType="link" nzSize="small" (click)="openRoleModal(a)" title="Change Role">
                    <span nz-icon nzType="setting"></span>
                  </button>
                }
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
        </form>
      </ng-container>
    </nz-modal>

    <!-- End Assignment Modal -->
    <nz-modal
      [(nzVisible)]="endModalVisible"
      nzTitle="End Assignment"
      (nzOnCancel)="endModalVisible = false"
      (nzOnOk)="submitEnd()"
      [nzOkLoading]="saving"
    >
      <ng-container *nzModalContent>
        <form nz-form [formGroup]="endForm" nzLayout="vertical">
          <nz-form-item>
            <nz-form-label nzRequired>End Date</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-date-picker formControlName="endDate" nzFormat="yyyy-MM-dd" style="width:100%"></nz-date-picker>
            </nz-form-control>
          </nz-form-item>
        </form>
      </ng-container>
    </nz-modal>

    <!-- Update Allocation Modal -->
    <nz-modal
      [(nzVisible)]="allocationModalVisible"
      nzTitle="Update Allocation"
      (nzOnCancel)="allocationModalVisible = false"
      (nzOnOk)="submitAllocation()"
      [nzOkLoading]="saving"
    >
      <ng-container *nzModalContent>
        <form nz-form [formGroup]="allocationForm" nzLayout="vertical">
          <nz-form-item>
            <nz-form-label nzRequired>Allocation %</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-input-number formControlName="allocationPct" [nzMin]="1" [nzMax]="100" style="width:100%"></nz-input-number>
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
  endModalVisible = false;
  allocationModalVisible = false;
  roleModalVisible = false;

  createForm!: FormGroup;
  endForm!: FormGroup;
  allocationForm!: FormGroup;
  roleForm!: FormGroup;

  ngOnInit(): void {
    this.createForm = this.fb.group({
      employeeId: [null, Validators.required],
      teamId: [null, Validators.required],
      allocationPct: [100, Validators.required],
      roleType: [null, Validators.required],
      roleWeight: [1, Validators.required],
      startDate: [null, Validators.required],
    });
    this.endForm = this.fb.group({ endDate: [null, Validators.required] });
    this.allocationForm = this.fb.group({ allocationPct: [null, Validators.required] });
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
    return date.toISOString().split('T')[0];
  }

  openCreateModal(): void {
    this.createForm.reset({ allocationPct: 100, roleWeight: 1 });
    this.createModalVisible = true;
  }

  openEndModal(a: TeamAssignmentDTO): void {
    this.selectedAssignment = a;
    this.endForm.reset();
    this.endModalVisible = true;
  }

  openAllocationModal(a: TeamAssignmentDTO): void {
    this.selectedAssignment = a;
    this.allocationForm.patchValue({ allocationPct: a.allocationPct });
    this.allocationModalVisible = true;
  }

  openRoleModal(a: TeamAssignmentDTO): void {
    this.selectedAssignment = a;
    this.roleForm.patchValue({ roleType: a.roleType, roleWeight: a.roleWeight, effectiveFrom: null }, { emitEvent: false });
    this.roleModalVisible = true;
  }

  submitCreate(): void {
    if (this.createForm.invalid) { Object.values(this.createForm.controls).forEach(c => { c.markAsDirty(); c.updateValueAndValidity(); }); return; }
    const v = this.createForm.value;
    const body = { ...v, startDate: this.formatDate(v.startDate) };
    this.saving = true;
    this.assignmentService.createAssignment(body).subscribe({
      next: () => { this.message.success('Assignment created'); this.saving = false; this.createModalVisible = false; this.loadAllAssignments(); },
      error: () => { this.message.error('Failed to create'); this.saving = false; },
    });
  }

  submitEnd(): void {
    if (this.endForm.invalid || !this.selectedAssignment) return;
    const endDate = this.formatDate(this.endForm.value.endDate);
    this.saving = true;
    this.assignmentService.endAssignment(this.selectedAssignment.id, endDate).subscribe({
      next: () => { this.message.success('Assignment ended'); this.saving = false; this.endModalVisible = false; this.loadAllAssignments(); },
      error: () => { this.message.error('Failed'); this.saving = false; },
    });
  }

  submitAllocation(): void {
    if (this.allocationForm.invalid || !this.selectedAssignment) return;
    this.saving = true;
    this.assignmentService.updateAllocation(this.selectedAssignment.id, this.allocationForm.value).subscribe({
      next: () => { this.message.success('Allocation updated'); this.saving = false; this.allocationModalVisible = false; this.loadAllAssignments(); },
      error: () => { this.message.error('Failed'); this.saving = false; },
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
}
