import { Component, OnInit, ViewChild, ElementRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzBadgeModule } from 'ng-zorro-antd/badge';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { SearchBarComponent } from '../../shared/molecules/search-bar/search-bar.component';
import { EmployeeService } from '../../core/services/employee.service';
import { AssignmentService } from '../../core/services/assignment.service';
import { EmployeeDTO, ImportResult } from '../../core/models/employee.model';
import { TeamAssignmentDTO } from '../../core/models/assignment.model';

@Component({
  selector: 'app-employees-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    NzTableModule,
    NzButtonModule,
    NzModalModule,
    NzFormModule,
    NzInputModule,
    NzPopconfirmModule,
    NzIconModule,
    NzSpinModule,
    NzDrawerModule,
    NzTagModule,
    NzBadgeModule,
    PageHeaderComponent,
    SearchBarComponent,
  ],
  template: `
    <app-page-header title="Employees" subtitle="Manage employees and their assignments"></app-page-header>

    <div style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap;">
      <app-search-bar placeholder="Search employees..." (searchChange)="onSearch($event)"></app-search-bar>
      <div style="display: flex; gap: 8px;">
        <button nz-button nzType="default" (click)="triggerFileInput()">
          <span nz-icon nzType="upload"></span> Import CSV
        </button>
        <input #csvInput type="file" accept=".csv" style="display:none" (change)="onFileSelected($event)" />
        <button nz-button nzType="primary" (click)="openModal()">
          <span nz-icon nzType="plus"></span> Add Employee
        </button>
      </div>
    </div>

    <nz-spin [nzSpinning]="loading">
      <nz-table
        #table
        [nzData]="filteredEmployees"
        [nzBordered]="true"
        [nzSize]="'middle'"
        [nzPageSize]="15"
      >
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Total Allocation</th>
            <th>Assignments</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (emp of table.data; track emp.id) {
            <tr style="cursor: pointer;" (click)="viewAssignments(emp)">
              <td>
                <span nz-icon nzType="user" style="margin-right:8px; color:#1890ff"></span>
                {{ emp.firstName }} {{ emp.lastName }}
              </td>
              <td>{{ emp.email }}</td>
              <td>
                <nz-tag [nzColor]="emp.totalAllocation > 100 ? 'red' : emp.totalAllocation === 100 ? 'green' : 'blue'">
                  {{ emp.totalAllocation }}%
                </nz-tag>
              </td>
              <td>{{ emp.assignments ? emp.assignments.length : 0 }}</td>
              <td (click)="$event.stopPropagation()">
                <button nz-button nzType="link" (click)="openModal(emp)">
                  <span nz-icon nzType="edit"></span>
                </button>
                <button
                  nz-button nzType="link" nzDanger
                  nz-popconfirm
                  nzPopconfirmTitle="Delete this employee?"
                  (nzOnConfirm)="deleteEmployee(emp.id)"
                >
                  <span nz-icon nzType="delete"></span>
                </button>
              </td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-spin>

    <!-- Add/Edit Modal -->
    <nz-modal
      [(nzVisible)]="modalVisible"
      [nzTitle]="editingEmployee ? 'Edit Employee' : 'Add Employee'"
      (nzOnCancel)="closeModal()"
      (nzOnOk)="submitForm()"
      [nzOkLoading]="saving"
    >
      <ng-container *nzModalContent>
        <form nz-form [formGroup]="form" nzLayout="vertical">
          <nz-form-item>
            <nz-form-label nzRequired>First Name</nz-form-label>
            <nz-form-control nzErrorTip="First name is required">
              <input nz-input formControlName="firstName" placeholder="First name" />
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Last Name</nz-form-label>
            <nz-form-control nzErrorTip="Last name is required">
              <input nz-input formControlName="lastName" placeholder="Last name" />
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Email</nz-form-label>
            <nz-form-control nzErrorTip="Valid email is required">
              <input nz-input formControlName="email" placeholder="Email address" type="email" />
            </nz-form-control>
          </nz-form-item>
        </form>
      </ng-container>
    </nz-modal>

    <!-- Import Result Modal -->
    <nz-modal
      [(nzVisible)]="importResultVisible"
      nzTitle="CSV Import Result"
      [nzFooter]="null"
      (nzOnCancel)="closeImportResult()"
    >
      <ng-container *nzModalContent>
        @if (importing) {
          <div style="text-align:center; padding: 24px;">
            <nz-spin nzSimple></nz-spin>
            <p style="margin-top: 12px;">Importing employees…</p>
          </div>
        } @else if (importResult) {
          <div style="margin-bottom: 16px; display: flex; gap: 24px;">
            <div><strong style="color:#52c41a; font-size:24px;">{{ importResult.imported }}</strong><br/>Imported</div>
            <div><strong style="color:#faad14; font-size:24px;">{{ importResult.skipped }}</strong><br/>Skipped (duplicate)</div>
            <div><strong style="color:#ff4d4f; font-size:24px;">{{ importResult.errors.length }}</strong><br/>Errors</div>
          </div>
          @if (importResult.errors.length > 0) {
            <nz-table [nzData]="importResult.errors" [nzSize]="'small'" [nzBordered]="true" [nzPageSize]="10">
              <thead>
                <tr><th>Row</th><th>Email</th><th>Reason</th></tr>
              </thead>
              <tbody>
                @for (err of importResult.errors; track err.row) {
                  <tr>
                    <td>{{ err.row }}</td>
                    <td>{{ err.email }}</td>
                    <td><nz-tag nzColor="error">{{ err.reason }}</nz-tag></td>
                  </tr>
                }
              </tbody>
            </nz-table>
          }
          <div style="text-align:right; margin-top:16px;">
            <button nz-button nzType="primary" (click)="closeImportResult()">Close</button>
          </div>
        }
      </ng-container>
    </nz-modal>

    <!-- Assignments Drawer -->
    <nz-drawer
      [nzVisible]="drawerVisible"
      nzTitle="Assignments"
      nzWidth="640"
      (nzOnClose)="closeDrawer()"
    >
      <ng-container *nzDrawerContent>
        @if (selectedEmployee) {
          <h3>{{ selectedEmployee.firstName }} {{ selectedEmployee.lastName }}</h3>
          <nz-spin [nzSpinning]="loadingAssignments">
            <nz-table [nzData]="employeeAssignments" [nzBordered]="true" [nzSize]="'small'">
              <thead>
                <tr>
                  <th>Team</th>
                  <th>Allocation</th>
                  <th>Role</th>
                  <th>Start</th>
                  <th>End</th>
                </tr>
              </thead>
              <tbody>
                @for (a of employeeAssignments; track a.id) {
                  <tr>
                    <td>{{ a.teamName }}</td>
                    <td>{{ a.allocationPct }}%</td>
                    <td>{{ a.roleType }}</td>
                    <td>{{ a.startDate }}</td>
                    <td>
                      @if (a.endDate) {
                        <nz-tag nzColor="error">{{ a.endDate }}</nz-tag>
                      } @else {
                        <nz-tag nzColor="success">Active</nz-tag>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </nz-table>
          </nz-spin>
        }
      </ng-container>
    </nz-drawer>
  `,
})
export class EmployeesPageComponent implements OnInit {
  @ViewChild('csvInput') csvInput!: ElementRef<HTMLInputElement>;

  private employeeService = inject(EmployeeService);
  private assignmentService = inject(AssignmentService);
  private message = inject(NzMessageService);
  private fb = inject(FormBuilder);

  loading = false;
  saving = false;
  loadingAssignments = false;
  importing = false;
  modalVisible = false;
  drawerVisible = false;
  importResultVisible = false;
  employees: EmployeeDTO[] = [];
  filteredEmployees: EmployeeDTO[] = [];
  editingEmployee: EmployeeDTO | null = null;
  selectedEmployee: EmployeeDTO | null = null;
  employeeAssignments: TeamAssignmentDTO[] = [];
  importResult: ImportResult | null = null;

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
    });
    this.loadEmployees();
  }

  loadEmployees(): void {
    this.loading = true;
    this.employeeService.getEmployees().subscribe({
      next: emps => {
        this.employees = emps;
        this.filteredEmployees = emps;
        this.loading = false;
      },
      error: () => {
        this.message.error('Failed to load employees');
        this.loading = false;
      },
    });
  }

  onSearch(term: string): void {
    const lower = term.toLowerCase();
    this.filteredEmployees = this.employees.filter(e =>
      `${e.firstName} ${e.lastName}`.toLowerCase().includes(lower) ||
      e.email.toLowerCase().includes(lower)
    );
  }

  openModal(emp?: EmployeeDTO): void {
    this.editingEmployee = emp ?? null;
    if (emp) {
      this.form.patchValue({ firstName: emp.firstName, lastName: emp.lastName, email: emp.email });
    } else {
      this.form.reset();
    }
    this.modalVisible = true;
  }

  closeModal(): void {
    this.modalVisible = false;
    this.editingEmployee = null;
    this.form.reset();
  }

  submitForm(): void {
    if (this.form.invalid) {
      Object.values(this.form.controls).forEach(c => { c.markAsDirty(); c.updateValueAndValidity(); });
      return;
    }
    const body = this.form.value;
    this.saving = true;
    const req = this.editingEmployee
      ? this.employeeService.updateEmployee(this.editingEmployee.id, body)
      : this.employeeService.createEmployee(body);

    req.subscribe({
      next: () => {
        this.message.success(this.editingEmployee ? 'Employee updated' : 'Employee created');
        this.saving = false;
        this.closeModal();
        this.loadEmployees();
      },
      error: () => {
        this.message.error('Operation failed');
        this.saving = false;
      },
    });
  }

  deleteEmployee(id: string): void {
    this.employeeService.deleteEmployee(id).subscribe({
      next: () => {
        this.message.success('Employee deleted');
        this.loadEmployees();
      },
      error: () => this.message.error('Failed to delete employee'),
    });
  }

  viewAssignments(emp: EmployeeDTO): void {
    this.selectedEmployee = emp;
    this.drawerVisible = true;
    this.loadingAssignments = true;
    this.assignmentService.getByEmployee(emp.id).subscribe({
      next: assignments => {
        this.employeeAssignments = assignments;
        this.loadingAssignments = false;
      },
      error: () => {
        this.message.error('Failed to load assignments');
        this.loadingAssignments = false;
      },
    });
  }

  closeDrawer(): void {
    this.drawerVisible = false;
    this.selectedEmployee = null;
    this.employeeAssignments = [];
  }

  triggerFileInput(): void {
    this.csvInput.nativeElement.value = '';
    this.csvInput.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.importing = true;
    this.importResult = null;
    this.importResultVisible = true;
    this.employeeService.importEmployees(file).subscribe({
      next: result => {
        this.importing = false;
        this.importResult = result;
        if (result.imported > 0) this.loadEmployees();
      },
      error: () => {
        this.importing = false;
        this.importResultVisible = false;
        this.message.error('Failed to import CSV');
      },
    });
  }

  closeImportResult(): void {
    this.importResultVisible = false;
    this.importResult = null;
  }
}
