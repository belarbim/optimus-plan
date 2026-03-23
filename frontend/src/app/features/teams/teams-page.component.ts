import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { TeamService } from '../../core/services/team.service';
import { TeamDTO } from '../../core/models/team.model';
import { CategoryAllocationDTO } from '../../core/models/snapshot.model';

interface CategoryRow {
  categoryName: string;
  label: string;
  icon: string;
  color: string;
  allocationPct: number;
}

@Component({
  selector: 'app-teams-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    NzTableModule,
    NzButtonModule,
    NzModalModule,
    NzFormModule,
    NzInputModule,
    NzInputNumberModule,
    NzSelectModule,
    NzPopconfirmModule,
    NzIconModule,
    NzSpinModule,
    NzTagModule,
    NzTooltipModule,
    NzDrawerModule,
    NzDividerModule,
    NzProgressModule,
    NzAlertModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header title="Teams" subtitle="Manage team hierarchy"></app-page-header>

    <div style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center;">
      <span>{{ flatTeams.length }} team(s) total</span>
      <button nz-button nzType="primary" (click)="openModal()">
        <span nz-icon nzType="plus"></span> Add Team
      </button>
    </div>

    <nz-spin [nzSpinning]="loading">
      <nz-table
        #table
        [nzData]="flatTeams"
        [nzBordered]="true"
        [nzSize]="'middle'"
        [nzPageSize]="20"
      >
        <thead>
          <tr>
            <th>Name</th>
            <th>Type</th>
            <th>Parent Team</th>
            <th>Created At</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (team of table.data; track team.id) {
            <tr [style.background]="team.parentId ? '#fafafa' : '#fff'">
              <td>
                @if (team.parentId) {
                  <span style="display:inline-flex;align-items:center;padding-left:20px;">
                    <span style="color:#aaa;margin-right:6px;font-size:14px;">└</span>
                    <span nz-icon nzType="apartment" style="margin-right:6px;color:#722ed1"></span>
                    {{ team.name }}
                  </span>
                } @else {
                  <span style="display:inline-flex;align-items:center;">
                    <span nz-icon nzType="cluster" style="margin-right:6px;color:#1890ff"></span>
                    <strong>{{ team.name }}</strong>
                  </span>
                }
              </td>
              <td>
                @if (team.parentId) {
                  <nz-tag nzColor="purple">Sub-team</nz-tag>
                } @else {
                  <nz-tag nzColor="blue">Root team</nz-tag>
                }
              </td>
              <td>
                @if (team.parentId) {
                  <nz-tag nzColor="default">
                    <span nz-icon nzType="cluster" style="margin-right:4px"></span>
                    {{ getTeamName(team.parentId) }}
                  </nz-tag>
                } @else {
                  <span style="color:#bbb">—</span>
                }
              </td>
              <td>{{ team.createdAt | date:'mediumDate' }}</td>
              <td>
                <button nz-button nzType="link" nz-tooltip nzTooltipTitle="Edit" (click)="openModal(team)">
                  <span nz-icon nzType="edit"></span>
                </button>
                <button nz-button nzType="link" nz-tooltip nzTooltipTitle="Category allocations" (click)="openCategories(team)">
                  <span nz-icon nzType="pie-chart"></span>
                </button>
                <button
                  nz-button nzType="link" nzDanger
                  nz-tooltip nzTooltipTitle="Delete"
                  nz-popconfirm
                  nzPopconfirmTitle="Delete this team?"
                  (nzOnConfirm)="deleteTeam(team.id)"
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
      [nzTitle]="editingTeam ? 'Edit Team' : 'Add Team'"
      (nzOnCancel)="closeModal()"
      (nzOnOk)="submitForm()"
      [nzOkLoading]="saving"
    >
      <ng-container *nzModalContent>
        <form nz-form [formGroup]="form" nzLayout="vertical">
          <nz-form-item>
            <nz-form-label nzRequired>Team Name</nz-form-label>
            <nz-form-control nzErrorTip="Team name is required">
              <input nz-input formControlName="name" placeholder="Enter team name" />
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label>Parent Team <span style="color:#999;font-size:12px">(leave empty to create a root team)</span></nz-form-label>
            <nz-form-control>
              <nz-select
                formControlName="parentId"
                nzAllowClear
                nzPlaceHolder="None — create as root team"
                style="width:100%"
                nzShowSearch
              >
                @for (t of selectableParents; track t.id) {
                  <nz-option [nzValue]="t.id" [nzLabel]="t.name"></nz-option>
                }
              </nz-select>
            </nz-form-control>
          </nz-form-item>
        </form>
      </ng-container>
    </nz-modal>

    <!-- Category allocations drawer -->
    <nz-drawer
      [nzVisible]="drawerVisible"
      nzWidth="460"
      [nzTitle]="drawerTitle"
      (nzOnClose)="closeCategories()"
    >
      <ng-container *nzDrawerContent>
        <nz-spin [nzSpinning]="loadingCategories">

          <!-- ── Section 1: Incident ─────────────────────────────────────── -->
          <nz-divider nzText="Incident capacity (% of total)" nzOrientation="left"></nz-divider>

          <nz-alert
            nzType="info"
            nzMessage="Incident capacity is deducted first from the team's total capacity. The remaining capacity is then distributed among planned work categories."
            nzShowIcon
            style="margin-bottom:16px;font-size:12px"
          ></nz-alert>

          <div style="background:#fff1f0;border:1px solid #ffccc7;border-radius:8px;padding:16px;margin-bottom:24px">
            <div style="display:flex;align-items:center;margin-bottom:10px;gap:8px">
              <span nz-icon nzType="alert" style="font-size:18px;color:#ff4d4f"></span>
              <span style="font-weight:600;font-size:14px">Incident</span>
              <nz-tag nzColor="red" style="margin-left:auto">{{ incidentRow.allocationPct }}% of total</nz-tag>
            </div>
            <nz-input-number
              [(ngModel)]="incidentRow.allocationPct"
              [nzMin]="0"
              [nzMax]="100"
              [nzStep]="5"
              [nzFormatter]="pctFormatter"
              [nzParser]="pctParser"
              style="width:100%"
            ></nz-input-number>
          </div>

          <!-- ── Section 2: Planned work ─────────────────────────────────── -->
          <nz-divider nzText="Planned work (% of remaining capacity)" nzOrientation="left"></nz-divider>

          <div style="margin-bottom:12px">
            <div style="display:flex;justify-content:space-between;margin-bottom:4px">
              <span style="font-size:12px;color:#666">Must sum to 100%</span>
              <span style="font-weight:600" [style.color]="plannedTotal === 100 ? '#52c41a' : '#ff4d4f'">
                {{ plannedTotal }}%
              </span>
            </div>
            <nz-progress
              [nzPercent]="plannedTotal > 100 ? 100 : plannedTotal"
              [nzStatus]="plannedTotal === 100 ? 'success' : plannedTotal > 100 ? 'exception' : 'active'"
              [nzShowInfo]="false"
            ></nz-progress>
          </div>

          @if (plannedTotal !== 100 && plannedTotal > 0) {
            <nz-alert
              nzType="warning"
              [nzMessage]="plannedTotal > 100
                ? 'Exceeds 100% by ' + (plannedTotal - 100) + '%. Reduce one category.'
                : (100 - plannedTotal) + '% still unallocated across planned categories.'"
              nzShowIcon
              style="margin-bottom:12px"
            ></nz-alert>
          }

          <div style="display:flex;flex-direction:column;gap:12px;margin-bottom:24px">
            @for (row of plannedRows; track row.categoryName) {
              <div style="background:#fafafa;border:1px solid #f0f0f0;border-radius:8px;padding:14px">
                <div style="display:flex;align-items:center;margin-bottom:10px;gap:8px">
                  <span nz-icon [nzType]="row.icon" [style.color]="row.color" style="font-size:17px"></span>
                  <span style="font-weight:600;font-size:13px">{{ row.label }}</span>
                  <nz-tag [nzColor]="row.color" style="margin-left:auto">{{ row.allocationPct }}%</nz-tag>
                </div>
                <nz-input-number
                  [(ngModel)]="row.allocationPct"
                  [nzMin]="0"
                  [nzMax]="100"
                  [nzStep]="5"
                  [nzFormatter]="pctFormatter"
                  [nzParser]="pctParser"
                  style="width:100%"
                ></nz-input-number>
              </div>
            }
          </div>

          <nz-divider></nz-divider>

          <div style="display:flex;gap:8px;justify-content:flex-end">
            <button nz-button (click)="closeCategories()">Cancel</button>
            <button
              nz-button nzType="primary"
              [disabled]="!canSave"
              [nzLoading]="savingCategories"
              (click)="saveCategories()"
            >Save allocations</button>
          </div>

        </nz-spin>
      </ng-container>
    </nz-drawer>
  `,
})
export class TeamsPageComponent implements OnInit {
  private teamService = inject(TeamService);
  private message = inject(NzMessageService);
  private fb = inject(FormBuilder);

  private static readonly INCIDENT: Pick<CategoryRow, 'categoryName' | 'label' | 'icon' | 'color'> =
    { categoryName: 'Incident', label: 'Incident', icon: 'alert', color: '#ff4d4f' };

  private static readonly PLANNED: Pick<CategoryRow, 'categoryName' | 'label' | 'icon' | 'color'>[] = [
    { categoryName: 'Project',                label: 'Project',                icon: 'project', color: '#1890ff' },
    { categoryName: 'Continuous Improvement', label: 'Continuous Improvement', icon: 'rise',    color: '#52c41a' },
    { categoryName: 'IT for IT',              label: 'IT for IT',              icon: 'tool',    color: '#722ed1' },
  ];

  loading = false;
  saving = false;
  modalVisible = false;
  /** All teams flattened in hierarchy order (roots first, then their children). */
  flatTeams: TeamDTO[] = [];
  editingTeam: TeamDTO | null = null;

  // Categories drawer
  drawerVisible = false;
  drawerTitle = '';
  loadingCategories = false;
  savingCategories = false;
  selectedTeam: TeamDTO | null = null;
  incidentRow: CategoryRow = { ...TeamsPageComponent.INCIDENT, allocationPct: 0 };
  plannedRows: CategoryRow[] = [];

  /** Planned categories (Project + CI + IT4IT) must sum to 100. */
  get plannedTotal(): number {
    return this.plannedRows.reduce((sum, r) => sum + (r.allocationPct ?? 0), 0);
  }

  /** True when both constraints are satisfied and the form can be saved. */
  get canSave(): boolean {
    return this.incidentRow.allocationPct >= 0
        && this.incidentRow.allocationPct <= 100
        && this.plannedTotal === 100;
  }

  readonly pctFormatter = (v: number) => `${v}%`;
  readonly pctParser   = (v: string)  => Number(v.replace('%', ''));

  form!: FormGroup;

  get selectableParents(): TeamDTO[] {
    return this.flatTeams.filter(t => !this.editingTeam || t.id !== this.editingTeam.id);
  }

  ngOnInit(): void {
    this.form = this.fb.group({ name: ['', Validators.required], parentId: [null] });
    this.loadTeams();
  }

  loadTeams(): void {
    this.loading = true;
    // Request tree=true so children are nested; then flatten preserving hierarchy order.
    this.teamService.getTeams(true).subscribe({
      next: roots => {
        this.flatTeams = this.flattenTree(roots);
        this.loading = false;
      },
      error: () => {
        this.message.error('Failed to load teams');
        this.loading = false;
      },
    });
  }

  /** Depth-first flatten: each root followed immediately by its descendants. */
  private flattenTree(teams: TeamDTO[]): TeamDTO[] {
    const result: TeamDTO[] = [];
    const visit = (list: TeamDTO[]) => {
      for (const team of list) {
        result.push(team);
        if (team.children?.length) visit(team.children);
      }
    };
    visit(teams);
    return result;
  }

  getTeamName(id: string): string {
    return this.flatTeams.find(t => t.id === id)?.name ?? id;
  }

  openModal(team?: TeamDTO): void {
    this.editingTeam = team ?? null;
    this.form.patchValue({ name: team?.name ?? '', parentId: team?.parentId ?? null });
    this.modalVisible = true;
  }

  closeModal(): void {
    this.modalVisible = false;
    this.editingTeam = null;
    this.form.reset();
  }

  submitForm(): void {
    if (this.form.invalid) {
      Object.values(this.form.controls).forEach(c => { c.markAsDirty(); c.updateValueAndValidity(); });
      return;
    }
    const { name, parentId } = this.form.value;
    const body = { name, ...(parentId ? { parentId } : {}) };
    this.saving = true;

    const req = this.editingTeam
      ? this.teamService.updateTeam(this.editingTeam.id, body)
      : this.teamService.createTeam(body);

    req.subscribe({
      next: () => {
        this.message.success(this.editingTeam ? 'Team updated' : 'Team created');
        this.saving = false;
        this.closeModal();
        this.loadTeams();
      },
      error: () => {
        this.message.error('Operation failed');
        this.saving = false;
      },
    });
  }

  openCategories(team: TeamDTO): void {
    this.selectedTeam = team;
    this.drawerTitle = `Category allocations — ${team.name}`;
    this.drawerVisible = true;
    this.loadingCategories = true;

    this.teamService.getCategories(team.id).subscribe({
      next: (saved: CategoryAllocationDTO[]) => {
        const inc = TeamsPageComponent.INCIDENT;
        this.incidentRow = {
          ...inc,
          allocationPct: saved.find(s => s.categoryName === inc.categoryName)?.allocationPct ?? 0,
        };
        this.plannedRows = TeamsPageComponent.PLANNED.map(cat => ({
          ...cat,
          allocationPct: saved.find(s => s.categoryName === cat.categoryName)?.allocationPct ?? 0,
        }));
        this.loadingCategories = false;
      },
      error: () => {
        this.incidentRow = { ...TeamsPageComponent.INCIDENT, allocationPct: 0 };
        this.plannedRows = TeamsPageComponent.PLANNED.map(cat => ({ ...cat, allocationPct: 0 }));
        this.loadingCategories = false;
      },
    });
  }

  closeCategories(): void {
    this.drawerVisible = false;
    this.selectedTeam = null;
    this.incidentRow = { ...TeamsPageComponent.INCIDENT, allocationPct: 0 };
    this.plannedRows = [];
  }

  saveCategories(): void {
    if (!this.selectedTeam || !this.canSave) return;
    this.savingCategories = true;
    const categories = [
      { categoryName: this.incidentRow.categoryName, allocationPct: this.incidentRow.allocationPct },
      ...this.plannedRows.map(r => ({ categoryName: r.categoryName, allocationPct: r.allocationPct })),
    ];
    this.teamService.updateCategories(this.selectedTeam.id, { categories }).subscribe({
      next: () => {
        this.message.success('Category allocations saved');
        this.savingCategories = false;
        this.closeCategories();
      },
      error: () => {
        this.message.error('Failed to save allocations');
        this.savingCategories = false;
      },
    });
  }

  deleteTeam(id: string): void {
    this.teamService.deleteTeam(id).subscribe({
      next: () => { this.message.success('Team deleted'); this.loadTeams(); },
      error: () => this.message.error('Failed to delete team'),
    });
  }
}
