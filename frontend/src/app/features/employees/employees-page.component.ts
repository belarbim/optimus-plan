import { Component, OnInit, ViewChild, ElementRef, inject } from '@angular/core';
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
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzBadgeModule } from 'ng-zorro-antd/badge';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzTimelineModule } from 'ng-zorro-antd/timeline';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { SearchBarComponent } from '../../shared/molecules/search-bar/search-bar.component';
import { EmployeeService } from '../../core/services/employee.service';
import { AssignmentService } from '../../core/services/assignment.service';
import { GradeService } from '../../core/services/grade.service';
import { EmployeeCostService } from '../../core/services/employee-cost.service';
import { EmployeeDTO, EmployeeTypeHistoryDTO, ImportResult } from '../../core/models/employee.model';
import { TeamAssignmentDTO } from '../../core/models/assignment.model';
import { GradeDTO, GradeHistoryDTO, CostHistoryDTO } from '../../core/models/grade.model';

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
    NzInputNumberModule,
    NzPopconfirmModule,
    NzIconModule,
    NzSpinModule,
    NzDrawerModule,
    NzTagModule,
    NzBadgeModule,
    NzSelectModule,
    NzDatePickerModule,
    NzTimelineModule,
    NzDividerModule,
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
            <th [nzSortFn]="sortByName">Name</th>
            <th [nzSortFn]="sortByEmail">Email</th>
            <th>Type</th>
            <th [nzSortFn]="sortByAllocation">Total Allocation</th>
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
                <nz-tag [nzColor]="emp.type === 'EXTERNAL' ? 'orange' : 'blue'">
                  {{ emp.type || 'INTERNAL' }}
                </nz-tag>
              </td>
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
      nzTitle="Employee Details"
      nzWidth="720"
      (nzOnClose)="closeDrawer()"
    >
      <ng-container *nzDrawerContent>
        @if (selectedEmployee) {
          <h3>{{ selectedEmployee.firstName }} {{ selectedEmployee.lastName }}
            <nz-tag style="margin-left:8px" [nzColor]="selectedEmployee.type === 'EXTERNAL' ? 'orange' : 'blue'">
              {{ selectedEmployee.type || 'INTERNAL' }}
            </nz-tag>
          </h3>

          <!-- Assignments section -->
          <nz-divider nzText="Assignments" nzOrientation="left"></nz-divider>
          <nz-spin [nzSpinning]="loadingAssignments">
            <nz-table [nzData]="employeeAssignments" [nzBordered]="true" [nzSize]="'small'">
              <thead>
                <tr>
                  <th>Team</th>
                  <th>Allocation</th>
                  <th>Role</th>
                  <th>Start</th>
                  <th>End</th>
                  <th></th>
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
                    <td>
                      <button
                        nz-button nzType="link" nzDanger nzSize="small"
                        nz-popconfirm
                        nzPopconfirmTitle="Delete this assignment?"
                        (nzOnConfirm)="deleteAssignment(a.id)"
                      >
                        <span nz-icon nzType="delete"></span>
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </nz-table>
          </nz-spin>

          <!-- Type History (always visible) -->
          <nz-divider nzText="Type History" nzOrientation="left"></nz-divider>
          <nz-spin [nzSpinning]="loadingTypeHistory">
            @if (typeHistory.length > 0) {
              <nz-timeline>
                @for (entry of typeHistory; track entry.id) {
                  <nz-timeline-item [nzColor]="entry.type === 'EXTERNAL' ? 'orange' : 'blue'">
                    @if (editingTypeEntryId === entry.id) {
                      <form nz-form [formGroup]="typeEntryEditForm" nzLayout="inline" style="display:inline-flex;gap:4px;align-items:center">
                        <nz-select formControlName="type" style="width:120px" nzPlaceHolder="Type">
                          <nz-option nzValue="INTERNAL" nzLabel="Internal"></nz-option>
                          <nz-option nzValue="EXTERNAL" nzLabel="External"></nz-option>
                        </nz-select>
                        <nz-date-picker formControlName="effectiveFrom" nzFormat="yyyy-MM-dd" style="width:140px"></nz-date-picker>
                        <button nz-button nzType="primary" nzSize="small" [disabled]="typeEntryEditForm.invalid || savingHistory" (click)="saveTypeEntry(entry.id)">Save</button>
                        <button nz-button nzSize="small" (click)="cancelTypeEntry()">Cancel</button>
                      </form>
                    } @else {
                      <nz-tag [nzColor]="entry.type === 'EXTERNAL' ? 'orange' : 'blue'">{{ entry.type }}</nz-tag>
                      <small style="color:#888"> Effective from: {{ entry.effectiveFrom }}</small>
                      <button nz-button nzType="link" nzSize="small" style="padding:0 4px" (click)="startEditTypeEntry(entry)"><span nz-icon nzType="edit"></span></button>
                      <button nz-button nzType="link" nzDanger nzSize="small" style="padding:0 4px"
                        nz-popconfirm nzPopconfirmTitle="Delete this type entry?"
                        (nzOnConfirm)="deleteTypeEntry(entry.id)"><span nz-icon nzType="delete"></span></button>
                    }
                  </nz-timeline-item>
                }
              </nz-timeline>
            } @else {
              <p style="color:#aaa">No type history yet.</p>
            }
          </nz-spin>

          <!-- Add Type History form -->
          <div style="margin-top:12px; padding:12px; border:1px dashed #d9d9d9; border-radius:4px;">
            <h4 style="margin-bottom:8px">Add Type Entry</h4>
            <form nz-form [formGroup]="typeHistoryForm" nzLayout="inline">
              <nz-form-item>
                <nz-form-label>Type</nz-form-label>
                <nz-form-control>
                  <nz-select formControlName="type" style="width:140px" nzPlaceHolder="Select type">
                    <nz-option nzValue="INTERNAL" nzLabel="Internal"></nz-option>
                    <nz-option nzValue="EXTERNAL" nzLabel="External"></nz-option>
                  </nz-select>
                </nz-form-control>
              </nz-form-item>
              <nz-form-item>
                <nz-form-label>Effective From</nz-form-label>
                <nz-form-control>
                  <nz-date-picker formControlName="effectiveFrom" nzFormat="yyyy-MM-dd"></nz-date-picker>
                </nz-form-control>
              </nz-form-item>
              <nz-form-item>
                <nz-form-control>
                  <button nz-button nzType="primary" [disabled]="typeHistoryForm.invalid || savingHistory" (click)="addTypeHistory()">Add</button>
                </nz-form-control>
              </nz-form-item>
            </form>
          </div>

          <!-- Grade History -->
          <nz-divider nzText="Grade History" nzOrientation="left"></nz-divider>

            <nz-spin [nzSpinning]="loadingHistory">
              @if (gradeHistory.length > 0) {
                <nz-timeline>
                  @for (entry of gradeHistory; track entry.id) {
                    <nz-timeline-item>
                      @if (editingGradeEntryId === entry.id) {
                        <form nz-form [formGroup]="gradeEntryEditForm" nzLayout="inline" style="display:inline-flex;gap:4px;align-items:center">
                          <nz-select formControlName="gradeId" style="width:160px" nzPlaceHolder="Grade">
                            @for (g of grades; track g.id) {
                              <nz-option [nzValue]="g.id" [nzLabel]="g.name + ' (' + g.dailyCost + '/day)'"></nz-option>
                            }
                          </nz-select>
                          <nz-date-picker formControlName="effectiveFrom" nzFormat="yyyy-MM-dd" style="width:140px"></nz-date-picker>
                          <button nz-button nzType="primary" nzSize="small" [disabled]="gradeEntryEditForm.invalid || savingHistory" (click)="saveGradeEntry(entry.id)">Save</button>
                          <button nz-button nzSize="small" (click)="cancelGradeEntry()">Cancel</button>
                        </form>
                      } @else {
                        <strong>{{ entry.gradeName }}</strong> — {{ entry.dailyCost | currency:'EUR':'symbol':'1.2-2' }}/day
                        <small style="color:#888"> Effective from: {{ entry.effectiveFrom }}</small>
                        <button nz-button nzType="link" nzSize="small" style="padding:0 4px" (click)="startEditGradeEntry(entry)"><span nz-icon nzType="edit"></span></button>
                        <button nz-button nzType="link" nzDanger nzSize="small" style="padding:0 4px"
                          nz-popconfirm nzPopconfirmTitle="Delete this grade entry?"
                          (nzOnConfirm)="deleteGradeEntry(entry.id)"><span nz-icon nzType="delete"></span></button>
                      }
                    </nz-timeline-item>
                  }
                </nz-timeline>
              } @else {
                <p style="color:#aaa">No grade history yet.</p>
              }
            </nz-spin>

            <!-- Add Grade History form -->
            <div style="margin-top:12px; padding:12px; border:1px dashed #d9d9d9; border-radius:4px;">
              <h4 style="margin-bottom:8px">Add Grade Entry</h4>
              <form nz-form [formGroup]="gradeHistoryForm" nzLayout="inline">
                <nz-form-item>
                  <nz-form-label>Grade</nz-form-label>
                  <nz-form-control>
                    <nz-select formControlName="gradeId" style="width:160px" nzPlaceHolder="Select grade">
                      @for (g of grades; track g.id) {
                        <nz-option [nzValue]="g.id" [nzLabel]="g.name + ' (' + g.dailyCost + '/day)'"></nz-option>
                      }
                    </nz-select>
                  </nz-form-control>
                </nz-form-item>
                <nz-form-item>
                  <nz-form-label>Effective From</nz-form-label>
                  <nz-form-control>
                    <nz-date-picker formControlName="effectiveFrom" nzFormat="yyyy-MM-dd"></nz-date-picker>
                  </nz-form-control>
                </nz-form-item>
                <nz-form-item>
                  <nz-form-control>
                    <button nz-button nzType="primary" [disabled]="gradeHistoryForm.invalid || savingHistory" (click)="addGradeHistory()">Add</button>
                  </nz-form-control>
                </nz-form-item>
              </form>
            </div>

          <!-- Cost History -->
          <nz-divider nzText="Cost History" nzOrientation="left"></nz-divider>
            <nz-spin [nzSpinning]="loadingHistory">
              @if (costHistory.length > 0) {
                <nz-timeline>
                  @for (entry of costHistory; track entry.id) {
                    <nz-timeline-item>
                      @if (editingCostEntryId === entry.id) {
                        <form nz-form [formGroup]="costEntryEditForm" nzLayout="inline" style="display:inline-flex;gap:4px;align-items:center">
                          <nz-input-number formControlName="dailyCost" [nzMin]="0" [nzStep]="10" style="width:120px"></nz-input-number>
                          <nz-date-picker formControlName="effectiveFrom" nzFormat="yyyy-MM-dd" style="width:140px"></nz-date-picker>
                          <button nz-button nzType="primary" nzSize="small" [disabled]="costEntryEditForm.invalid || savingHistory" (click)="saveCostEntry(entry.id)">Save</button>
                          <button nz-button nzSize="small" (click)="cancelCostEntry()">Cancel</button>
                        </form>
                      } @else {
                        <strong>{{ entry.dailyCost | currency:'EUR':'symbol':'1.2-2' }}/day</strong>
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

            <!-- Add Cost History form -->
            <div style="margin-top:12px; padding:12px; border:1px dashed #d9d9d9; border-radius:4px;">
              <h4 style="margin-bottom:8px">Add Cost Entry</h4>
              <form nz-form [formGroup]="costHistoryForm" nzLayout="inline">
                <nz-form-item>
                  <nz-form-label>Daily Cost</nz-form-label>
                  <nz-form-control>
                    <nz-input-number formControlName="dailyCost" [nzMin]="0" [nzStep]="10" style="width:120px"></nz-input-number>
                  </nz-form-control>
                </nz-form-item>
                <nz-form-item>
                  <nz-form-label>Effective From</nz-form-label>
                  <nz-form-control>
                    <nz-date-picker formControlName="effectiveFrom" nzFormat="yyyy-MM-dd"></nz-date-picker>
                  </nz-form-control>
                </nz-form-item>
                <nz-form-item>
                  <nz-form-control>
                    <button nz-button nzType="primary" [disabled]="costHistoryForm.invalid || savingHistory" (click)="addCostHistory()">Add</button>
                  </nz-form-control>
                </nz-form-item>
              </form>
            </div>
        }
      </ng-container>
    </nz-drawer>
  `,
})
export class EmployeesPageComponent implements OnInit {
  @ViewChild('csvInput') csvInput!: ElementRef<HTMLInputElement>;

  private employeeService = inject(EmployeeService);
  private assignmentService = inject(AssignmentService);
  private gradeService = inject(GradeService);
  private employeeCostService = inject(EmployeeCostService);
  private message = inject(NzMessageService);
  private fb = inject(FormBuilder);

  loading = false;
  saving = false;
  loadingAssignments = false;
  loadingHistory = false;
  loadingTypeHistory = false;
  savingHistory = false;
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
  grades: GradeDTO[] = [];
  gradeHistory: GradeHistoryDTO[] = [];
  costHistory: CostHistoryDTO[] = [];
  typeHistory: EmployeeTypeHistoryDTO[] = [];

  form!: FormGroup;
  gradeHistoryForm!: FormGroup;
  costHistoryForm!: FormGroup;
  typeHistoryForm!: FormGroup;

  // Inline editing state
  editingTypeEntryId: string | null = null;
  editingGradeEntryId: string | null = null;
  editingCostEntryId: string | null = null;
  typeEntryEditForm!: FormGroup;
  gradeEntryEditForm!: FormGroup;
  costEntryEditForm!: FormGroup;

  sortByName = (a: EmployeeDTO, b: EmployeeDTO) =>
    `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`);
  sortByEmail = (a: EmployeeDTO, b: EmployeeDTO) => a.email.localeCompare(b.email);
  sortByAllocation = (a: EmployeeDTO, b: EmployeeDTO) => a.totalAllocation - b.totalAllocation;

  ngOnInit(): void {
    this.form = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
    });
    this.gradeHistoryForm = this.fb.group({
      gradeId: [null, Validators.required],
      effectiveFrom: [null, Validators.required],
    });
    this.costHistoryForm = this.fb.group({
      dailyCost: [null, [Validators.required, Validators.min(0)]],
      effectiveFrom: [null, Validators.required],
    });
    this.typeHistoryForm = this.fb.group({
      type: [null, Validators.required],
      effectiveFrom: [null, Validators.required],
    });
    this.typeEntryEditForm = this.fb.group({
      type: [null, Validators.required],
      effectiveFrom: [null, Validators.required],
    });
    this.gradeEntryEditForm = this.fb.group({
      gradeId: [null, Validators.required],
      effectiveFrom: [null, Validators.required],
    });
    this.costEntryEditForm = this.fb.group({
      dailyCost: [null, [Validators.required, Validators.min(0)]],
      effectiveFrom: [null, Validators.required],
    });
    this.loadEmployees();
    this.loadGrades();
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

  loadGrades(): void {
    this.gradeService.getAll().subscribe({
      next: g => { this.grades = g; },
      error: () => {},
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
    this.gradeHistory = [];
    this.costHistory = [];
    this.typeHistory = [];
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
    this.loadHistory(emp);
    this.loadTypeHistory(emp.id);
  }

  loadHistory(emp: EmployeeDTO): void {
    this.loadingHistory = true;
    let done = 0;
    const finish = () => { if (++done === 2) this.loadingHistory = false; };
    this.employeeCostService.getGradeHistory(emp.id).subscribe({
      next: h => { this.gradeHistory = h; finish(); },
      error: () => finish(),
    });
    this.employeeCostService.getCostHistory(emp.id).subscribe({
      next: h => { this.costHistory = h; finish(); },
      error: () => finish(),
    });
  }

  loadTypeHistory(employeeId: string): void {
    this.loadingTypeHistory = true;
    this.employeeCostService.getTypeHistory(employeeId).subscribe({
      next: h => { this.typeHistory = h; this.loadingTypeHistory = false; },
      error: () => { this.loadingTypeHistory = false; },
    });
  }

  addTypeHistory(): void {
    if (this.typeHistoryForm.invalid || !this.selectedEmployee) return;
    this.savingHistory = true;
    const val = this.typeHistoryForm.value;
    const effectiveFrom = val.effectiveFrom instanceof Date
      ? val.effectiveFrom.toISOString().split('T')[0]
      : val.effectiveFrom;
    this.employeeCostService.addTypeHistory(this.selectedEmployee.id, { type: val.type, effectiveFrom }).subscribe({
      next: () => {
        this.message.success('Type entry added');
        this.savingHistory = false;
        this.typeHistoryForm.reset();
        if (this.selectedEmployee) {
          this.loadTypeHistory(this.selectedEmployee.id);
          // Reload employees to pick up updated type
          this.loadEmployees();
          // Refresh selected employee's type from updated list
          const empId = this.selectedEmployee.id;
          this.employeeService.getEmployees().subscribe({
            next: emps => {
              const updated = emps.find(e => e.id === empId);
              if (updated) {
                this.selectedEmployee = updated;
                this.loadHistory(updated);
              }
            },
            error: () => {},
          });
        }
      },
      error: () => {
        this.message.error('Failed to add type entry');
        this.savingHistory = false;
      },
    });
  }

  addGradeHistory(): void {
    if (this.gradeHistoryForm.invalid || !this.selectedEmployee) return;
    this.savingHistory = true;
    const val = this.gradeHistoryForm.value;
    const effectiveFrom = val.effectiveFrom instanceof Date
      ? val.effectiveFrom.toISOString().split('T')[0]
      : val.effectiveFrom;
    this.employeeCostService.addGradeHistory(this.selectedEmployee.id, { gradeId: val.gradeId, effectiveFrom }).subscribe({
      next: () => {
        this.message.success('Grade entry added');
        this.savingHistory = false;
        this.gradeHistoryForm.reset();
        if (this.selectedEmployee) this.loadHistory(this.selectedEmployee);
      },
      error: () => {
        this.message.error('Failed to add grade entry');
        this.savingHistory = false;
      },
    });
  }

  addCostHistory(): void {
    if (this.costHistoryForm.invalid || !this.selectedEmployee) return;
    this.savingHistory = true;
    const val = this.costHistoryForm.value;
    const effectiveFrom = val.effectiveFrom instanceof Date
      ? val.effectiveFrom.toISOString().split('T')[0]
      : val.effectiveFrom;
    this.employeeCostService.addCostHistory(this.selectedEmployee.id, { dailyCost: val.dailyCost, effectiveFrom }).subscribe({
      next: () => {
        this.message.success('Cost entry added');
        this.savingHistory = false;
        this.costHistoryForm.reset();
        if (this.selectedEmployee) this.loadHistory(this.selectedEmployee);
      },
      error: () => {
        this.message.error('Failed to add cost entry');
        this.savingHistory = false;
      },
    });
  }

  // Type entry inline edit/delete
  startEditTypeEntry(entry: EmployeeTypeHistoryDTO): void {
    this.editingTypeEntryId = entry.id;
    const date = entry.effectiveFrom ? new Date(entry.effectiveFrom) : null;
    this.typeEntryEditForm.setValue({ type: entry.type, effectiveFrom: date });
  }

  cancelTypeEntry(): void {
    this.editingTypeEntryId = null;
    this.typeEntryEditForm.reset();
  }

  saveTypeEntry(entryId: string): void {
    if (this.typeEntryEditForm.invalid || !this.selectedEmployee) return;
    this.savingHistory = true;
    const val = this.typeEntryEditForm.value;
    const effectiveFrom = val.effectiveFrom instanceof Date
      ? val.effectiveFrom.toISOString().split('T')[0]
      : val.effectiveFrom;
    this.employeeCostService.updateTypeHistory(this.selectedEmployee.id, entryId, { type: val.type, effectiveFrom }).subscribe({
      next: () => {
        this.message.success('Type entry updated');
        this.savingHistory = false;
        this.editingTypeEntryId = null;
        this.typeEntryEditForm.reset();
        if (this.selectedEmployee) {
          this.loadTypeHistory(this.selectedEmployee.id);
          this.loadEmployees();
          const empId = this.selectedEmployee.id;
          this.employeeService.getEmployees().subscribe({
            next: emps => {
              const updated = emps.find(e => e.id === empId);
              if (updated) { this.selectedEmployee = updated; this.loadHistory(updated); }
            },
            error: () => {},
          });
        }
      },
      error: () => { this.message.error('Failed to update type entry'); this.savingHistory = false; },
    });
  }

  deleteTypeEntry(entryId: string): void {
    if (!this.selectedEmployee) return;
    this.employeeCostService.deleteTypeHistory(this.selectedEmployee.id, entryId).subscribe({
      next: () => {
        this.message.success('Type entry deleted');
        if (this.selectedEmployee) {
          this.loadTypeHistory(this.selectedEmployee.id);
          this.loadEmployees();
        }
      },
      error: () => this.message.error('Failed to delete type entry'),
    });
  }

  // Grade entry inline edit/delete
  startEditGradeEntry(entry: GradeHistoryDTO): void {
    this.editingGradeEntryId = entry.id;
    const date = entry.effectiveFrom ? new Date(entry.effectiveFrom) : null;
    this.gradeEntryEditForm.setValue({ gradeId: entry.gradeId, effectiveFrom: date });
  }

  cancelGradeEntry(): void {
    this.editingGradeEntryId = null;
    this.gradeEntryEditForm.reset();
  }

  saveGradeEntry(entryId: string): void {
    if (this.gradeEntryEditForm.invalid || !this.selectedEmployee) return;
    this.savingHistory = true;
    const val = this.gradeEntryEditForm.value;
    const effectiveFrom = val.effectiveFrom instanceof Date
      ? val.effectiveFrom.toISOString().split('T')[0]
      : val.effectiveFrom;
    this.employeeCostService.updateGradeHistory(this.selectedEmployee.id, entryId, { gradeId: val.gradeId, effectiveFrom }).subscribe({
      next: () => {
        this.message.success('Grade entry updated');
        this.savingHistory = false;
        this.editingGradeEntryId = null;
        this.gradeEntryEditForm.reset();
        if (this.selectedEmployee) this.loadHistory(this.selectedEmployee);
      },
      error: () => { this.message.error('Failed to update grade entry'); this.savingHistory = false; },
    });
  }

  deleteGradeEntry(entryId: string): void {
    if (!this.selectedEmployee) return;
    this.employeeCostService.deleteGradeHistory(this.selectedEmployee.id, entryId).subscribe({
      next: () => {
        this.message.success('Grade entry deleted');
        if (this.selectedEmployee) this.loadHistory(this.selectedEmployee);
      },
      error: () => this.message.error('Failed to delete grade entry'),
    });
  }

  // Cost entry inline edit/delete
  startEditCostEntry(entry: CostHistoryDTO): void {
    this.editingCostEntryId = entry.id;
    const date = entry.effectiveFrom ? new Date(entry.effectiveFrom) : null;
    this.costEntryEditForm.setValue({ dailyCost: entry.dailyCost, effectiveFrom: date });
  }

  cancelCostEntry(): void {
    this.editingCostEntryId = null;
    this.costEntryEditForm.reset();
  }

  saveCostEntry(entryId: string): void {
    if (this.costEntryEditForm.invalid || !this.selectedEmployee) return;
    this.savingHistory = true;
    const val = this.costEntryEditForm.value;
    const effectiveFrom = val.effectiveFrom instanceof Date
      ? val.effectiveFrom.toISOString().split('T')[0]
      : val.effectiveFrom;
    this.employeeCostService.updateCostHistory(this.selectedEmployee.id, entryId, { dailyCost: val.dailyCost, effectiveFrom }).subscribe({
      next: () => {
        this.message.success('Cost entry updated');
        this.savingHistory = false;
        this.editingCostEntryId = null;
        this.costEntryEditForm.reset();
        if (this.selectedEmployee) this.loadHistory(this.selectedEmployee);
      },
      error: () => { this.message.error('Failed to update cost entry'); this.savingHistory = false; },
    });
  }

  deleteCostEntry(entryId: string): void {
    if (!this.selectedEmployee) return;
    this.employeeCostService.deleteCostHistory(this.selectedEmployee.id, entryId).subscribe({
      next: () => {
        this.message.success('Cost entry deleted');
        if (this.selectedEmployee) this.loadHistory(this.selectedEmployee);
      },
      error: () => this.message.error('Failed to delete cost entry'),
    });
  }

  deleteAssignment(id: string): void {
    this.assignmentService.deleteAssignment(id).subscribe({
      next: () => {
        this.message.success('Assignment deleted');
        this.employeeAssignments = this.employeeAssignments.filter(a => a.id !== id);
        this.loadEmployees();
      },
      error: () => this.message.error('Failed to delete assignment'),
    });
  }

  closeDrawer(): void {
    this.drawerVisible = false;
    this.selectedEmployee = null;
    this.employeeAssignments = [];
    this.gradeHistory = [];
    this.costHistory = [];
    this.typeHistory = [];
    this.editingTypeEntryId = null;
    this.editingGradeEntryId = null;
    this.editingCostEntryId = null;
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
