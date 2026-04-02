import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule, FormArray } from '@angular/forms';
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
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { TeamService } from '../../core/services/team.service';
import { TeamTypeService } from '../../core/services/team-type.service';
import { TeamDTO } from '../../core/models/team.model';
import { TeamTypeDTO } from '../../core/models/team-type.model';
import { CategoryAllocationDTO } from '../../core/models/snapshot.model';
import { forkJoin } from 'rxjs';

interface CategoryRow {
  name: string;
  isPartOfTotalCapacity: boolean;
  isPartOfRemainingCapacity: boolean;
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
    NzSwitchModule,
    NzTabsModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header title="Teams" subtitle="Manage team hierarchy and team types"></app-page-header>

    <nz-tabs>

      <!-- ══════════════════════ TAB 1: TEAMS ══════════════════════ -->
      <nz-tab nzTitle="Teams">
        <div style="margin:16px 0; display:flex; justify-content:space-between; align-items:center;">
          <span>{{ flatTeams.length }} team(s) total</span>
          <div style="display:flex;gap:8px;">
            <button nz-button nzType="default" (click)="openImportModal()">
              <span nz-icon nzType="upload"></span> Import CSV
            </button>
            <button nz-button nzType="primary" (click)="openTeamModal()">
              <span nz-icon nzType="plus"></span> Add Team
            </button>
          </div>
        </div>

        <nz-spin [nzSpinning]="loading">
          <nz-table #table [nzData]="flatTeams" [nzBordered]="true" [nzSize]="'middle'" [nzPageSize]="20">
            <thead>
              <tr>
                <th>Name</th>
                <th>Kind</th>
                <th>Team Type</th>
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
                        <span style="color:#aaa;margin-right:6px;">└</span>
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
                    <nz-tag [nzColor]="team.parentId ? 'purple' : 'blue'">
                      {{ team.parentId ? 'Sub-team' : 'Root team' }}
                    </nz-tag>
                  </td>
                  <td>
                    @if (getTeamTypeName(team.teamTypeId)) {
                      <nz-tag nzColor="cyan">{{ getTeamTypeName(team.teamTypeId) }}</nz-tag>
                    } @else {
                      <span style="color:#bbb">—</span>
                    }
                  </td>
                  <td>
                    @if (team.parentId) {
                      <nz-tag nzColor="default">{{ getTeamName(team.parentId) }}</nz-tag>
                    } @else {
                      <span style="color:#bbb">—</span>
                    }
                  </td>
                  <td>{{ team.createdAt | date:'mediumDate' }}</td>
                  <td>
                    <button nz-button nzType="link" nz-tooltip nzTooltipTitle="Edit" (click)="openTeamModal(team)">
                      <span nz-icon nzType="edit"></span>
                    </button>
                    <button nz-button nzType="link" nz-tooltip nzTooltipTitle="Category allocations" (click)="openCategories(team)">
                      <span nz-icon nzType="pie-chart"></span>
                    </button>
                    <button nz-button nzType="link" nzDanger nz-tooltip nzTooltipTitle="Delete"
                      nz-popconfirm nzPopconfirmTitle="Delete this team?" (nzOnConfirm)="deleteTeam(team.id)">
                      <span nz-icon nzType="delete"></span>
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </nz-table>
        </nz-spin>
      </nz-tab>

      <!-- ══════════════════════ TAB 2: TEAM TYPES ══════════════════════ -->
      <nz-tab nzTitle="Team Types">
        <div style="margin:16px 0; display:flex; justify-content:space-between; align-items:center;">
          <span>{{ teamTypes.length }} type(s) defined</span>
          <button nz-button nzType="primary" (click)="openTeamTypeModal()">
            <span nz-icon nzType="plus"></span> Add Team Type
          </button>
        </div>

        <nz-table [nzData]="teamTypes" [nzBordered]="true" [nzSize]="'middle'" [nzShowPagination]="false">
          <thead>
            <tr>
              <th>Name</th>
              <th>Total-capacity categories</th>
              <th>Remaining-capacity categories</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (tt of teamTypes; track tt.id) {
              <tr>
                <td><strong>{{ tt.name }}</strong></td>
                <td>
                  @for (c of totalCats(tt); track c.name) {
                    <nz-tag nzColor="red">{{ c.name }}</nz-tag>
                  }
                </td>
                <td>
                  @for (c of remainingCats(tt); track c.name) {
                    <nz-tag nzColor="blue">{{ c.name }}</nz-tag>
                  }
                </td>
                <td>
                  <button nz-button nzType="link" (click)="openTeamTypeModal(tt)">
                    <span nz-icon nzType="edit"></span>
                  </button>
                  <button nz-button nzType="link" nzDanger
                    nz-popconfirm nzPopconfirmTitle="Delete this team type?"
                    (nzOnConfirm)="deleteTeamType(tt.id)">
                    <span nz-icon nzType="delete"></span>
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </nz-table>
      </nz-tab>

    </nz-tabs>

    <!-- ══════════════════ TEAM MODAL ══════════════════ -->
    <nz-modal
      [(nzVisible)]="teamModalVisible"
      [nzTitle]="editingTeam ? 'Edit Team' : 'Add Team'"
      (nzOnCancel)="closeTeamModal()"
      (nzOnOk)="submitTeamForm()"
      [nzOkLoading]="saving"
    >
      <ng-container *nzModalContent>
        <form nz-form [formGroup]="teamForm" nzLayout="vertical">
          <nz-form-item>
            <nz-form-label nzRequired>Team Name</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <input nz-input formControlName="name" placeholder="Enter team name" />
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label>Parent Team <span style="color:#999;font-size:12px">(leave empty to create a root team)</span></nz-form-label>
            <nz-form-control>
              <nz-select formControlName="parentId" nzAllowClear nzPlaceHolder="None — root team" style="width:100%" nzShowSearch>
                @for (t of selectableParents; track t.id) {
                  <nz-option [nzValue]="t.id" [nzLabel]="t.name"></nz-option>
                }
              </nz-select>
            </nz-form-control>
          </nz-form-item>
          @if (!teamForm.value.parentId) {
            <nz-form-item>
              <nz-form-label>Team Type <span style="color:#999;font-size:12px">(inherited by sub-teams)</span></nz-form-label>
              <nz-form-control>
                <nz-select formControlName="teamTypeId" nzAllowClear nzPlaceHolder="No type" style="width:100%">
                  @for (tt of teamTypes; track tt.id) {
                    <nz-option [nzValue]="tt.id" [nzLabel]="tt.name"></nz-option>
                  }
                </nz-select>
              </nz-form-control>
            </nz-form-item>
          }
        </form>
      </ng-container>
    </nz-modal>

    <!-- ══════════════════ TEAM TYPE MODAL ══════════════════ -->
    <nz-modal
      [(nzVisible)]="teamTypeModalVisible"
      [nzTitle]="editingTeamType ? 'Edit Team Type' : 'Add Team Type'"
      (nzOnCancel)="closeTeamTypeModal()"
      (nzOnOk)="submitTeamTypeForm()"
      [nzOkLoading]="savingTeamType"
      [nzWidth]="640"
    >
      <ng-container *nzModalContent>
        <form nz-form [formGroup]="teamTypeForm" nzLayout="vertical">
          <nz-form-item>
            <nz-form-label nzRequired>Type Name</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <input nz-input formControlName="name" placeholder="e.g. Development, Operations" />
            </nz-form-control>
          </nz-form-item>

          <nz-divider nzText="Category Definitions" nzOrientation="left"></nz-divider>

          <nz-alert nzType="info" nzShowIcon style="margin-bottom:12px;font-size:12px;"
            nzMessage="Each category is either part of total capacity (overhead, e.g. Incident) or remaining capacity (planned work, e.g. Project). Remaining categories must sum to 100% when setting allocations.">
          </nz-alert>

          <div formArrayName="categories">
            @for (cat of categoryForms.controls; track $index) {
              <div [formGroupName]="$index" style="display:flex;gap:8px;align-items:center;margin-bottom:8px;padding:8px;border:1px solid #f0f0f0;border-radius:4px;">
                <nz-form-control style="flex:1">
                  <input nz-input formControlName="name" placeholder="Category name" />
                </nz-form-control>
                <nz-select formControlName="capacityType" style="width:200px">
                  <nz-option nzValue="total" nzLabel="Total capacity (overhead)"></nz-option>
                  <nz-option nzValue="remaining" nzLabel="Remaining capacity"></nz-option>
                </nz-select>
                <button nz-button nzType="link" nzDanger nzSize="small" (click)="removeCategory($index)">
                  <span nz-icon nzType="minus-circle"></span>
                </button>
              </div>
            }
          </div>

          <button nz-button nzType="dashed" style="width:100%;margin-top:4px;" (click)="addCategory()">
            <span nz-icon nzType="plus"></span> Add Category
          </button>
        </form>
      </ng-container>
    </nz-modal>

    <!-- ══════════════════ IMPORT CSV MODAL ══════════════════ -->
    <nz-modal
      [(nzVisible)]="importModalVisible"
      nzTitle="Import Teams from CSV"
      (nzOnCancel)="importModalVisible = false"
      [nzFooter]="null"
    >
      <ng-container *nzModalContent>
        <p style="margin-bottom:8px;">Upload a CSV file with the following columns (header row required):</p>
        <code style="display:block;background:#f5f5f5;padding:8px;border-radius:4px;font-size:12px;margin-bottom:8px;">
          name, parentName, teamTypeName
        </code>
        <p style="font-size:12px;color:#888;margin-bottom:16px;">
          <strong>parentName</strong>: optional — name of an existing root team.<br>
          <strong>teamTypeName</strong>: optional — only applied to root teams.
        </p>

        <button nz-button style="margin-bottom:16px;" (click)="downloadTemplate()">
          <span nz-icon nzType="download"></span> Download Template
        </button>

        <div style="border:2px dashed #d9d9d9;border-radius:4px;padding:24px;text-align:center;cursor:pointer;"
             (click)="csvFileInput.click()" (dragover)="$event.preventDefault()" (drop)="onImportFileDrop($event)">
          <span nz-icon nzType="inbox" style="font-size:32px;color:#40a9ff;"></span>
          <p style="margin:8px 0 4px;">Click or drag CSV file here</p>
          <p style="font-size:12px;color:#888;">{{ importFile ? importFile.name : 'No file selected' }}</p>
        </div>
        <input #csvFileInput type="file" accept=".csv" style="display:none" (change)="onImportFileSelect($event)">

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
              <ul style="font-size:12px;color:#ff4d4f;max-height:150px;overflow-y:auto;padding-left:16px;">
                @for (e of importResult.errors; track e) {
                  <li>{{ e }}</li>
                }
              </ul>
            }
          </div>
        }

        <div style="margin-top:16px;display:flex;justify-content:flex-end;gap:8px;">
          <button nz-button (click)="importModalVisible = false">Cancel</button>
          <button nz-button nzType="primary" [disabled]="!importFile" [nzLoading]="importing" (click)="submitImport()">
            Import
          </button>
        </div>
      </ng-container>
    </nz-modal>

    <!-- ══════════════════ CATEGORY ALLOCATIONS DRAWER ══════════════════ -->
    <nz-drawer
      [nzVisible]="drawerVisible"
      nzWidth="480"
      [nzTitle]="drawerTitle"
      (nzOnClose)="closeCategories()"
    >
      <ng-container *nzDrawerContent>
        <nz-spin [nzSpinning]="loadingCategories">

          @if (totalCategoryRows.length > 0) {
            <nz-divider nzText="Total-capacity categories (% of total)" nzOrientation="left"></nz-divider>
            <nz-alert nzType="info" nzShowIcon style="margin-bottom:12px;font-size:12px;"
              nzMessage="These are deducted first from total capacity. Their sum must be less than 100%.">
            </nz-alert>
            <div style="display:flex;flex-direction:column;gap:10px;margin-bottom:20px;">
              @for (row of totalCategoryRows; track row.name) {
                <div style="display:flex;align-items:center;gap:12px;padding:12px;background:#fff1f0;border:1px solid #ffccc7;border-radius:6px;">
                  <span style="flex:1;font-weight:600;">{{ row.name }}</span>
                  <nz-tag nzColor="red">{{ row.allocationPct }}%</nz-tag>
                  <nz-input-number
                    [ngModel]="row.allocationPct"
                    (ngModelChange)="row.allocationPct = $event"
                    [nzMin]="0" [nzMax]="100" [nzStep]="5"
                    [nzFormatter]="pctFormatter" [nzParser]="pctParser"
                    style="width:110px">
                  </nz-input-number>
                </div>
              }
            </div>
          }

          @if (remainingCategoryRows.length > 0) {
            <nz-divider nzText="Remaining-capacity categories (% of remaining capacity)" nzOrientation="left"></nz-divider>
            <nz-alert nzType="info" nzShowIcon style="margin-bottom:12px;font-size:12px;"
              nzMessage="These percentages are of the remaining capacity (100% − total overhead). They must always sum to 100%.">
            </nz-alert>
            <div style="display:flex;justify-content:space-between;margin-bottom:6px;">
              <span style="font-size:12px;color:#666">Must sum to 100%</span>
              <span style="font-weight:600;" [style.color]="remainingTotal === 100 ? '#52c41a' : '#ff4d4f'">
                {{ remainingTotal }}%
              </span>
            </div>
            <nz-progress
              [nzPercent]="remainingTotal > 100 ? 100 : remainingTotal"
              [nzStatus]="remainingTotal === 100 ? 'success' : remainingTotal > 100 ? 'exception' : 'active'"
              [nzShowInfo]="false" style="margin-bottom:12px;">
            </nz-progress>

            @if (totalCatSum === 100) {
              <nz-alert nzType="warning" nzShowIcon style="margin-bottom:12px;"
                nzMessage="Total overhead is 100% — no remaining capacity. All remaining categories must be 0%.">
              </nz-alert>
            } @else if (remainingTotal !== 100 && remainingTotal > 0) {
              <nz-alert nzType="warning" nzShowIcon style="margin-bottom:12px;"
                [nzMessage]="remainingTotal > 100
                  ? 'Exceeds 100% by ' + (remainingTotal - 100) + '%'
                  : (100 - remainingTotal) + '% still unallocated'">
              </nz-alert>
            }

            <div style="display:flex;flex-direction:column;gap:10px;margin-bottom:24px;">
              @for (row of remainingCategoryRows; track row.name) {
                <div style="display:flex;align-items:center;gap:12px;padding:12px;background:#e6f7ff;border:1px solid #91d5ff;border-radius:6px;">
                  <span style="flex:1;font-weight:600;">{{ row.name }}</span>
                  <nz-tag nzColor="blue">{{ row.allocationPct }}%</nz-tag>
                  <nz-input-number
                    [ngModel]="row.allocationPct"
                    (ngModelChange)="row.allocationPct = $event"
                    [nzMin]="0" [nzMax]="100" [nzStep]="5"
                    [nzFormatter]="pctFormatter" [nzParser]="pctParser"
                    style="width:110px">
                  </nz-input-number>
                </div>
              }
            </div>
          }

          <nz-divider></nz-divider>
          <div style="display:flex;gap:8px;justify-content:flex-end;">
            <button nz-button (click)="closeCategories()">Cancel</button>
            <button nz-button nzType="primary" [disabled]="!canSave" [nzLoading]="savingCategories"
              (click)="saveCategories()">Save allocations</button>
          </div>

        </nz-spin>
      </ng-container>
    </nz-drawer>
  `,
})
export class TeamsPageComponent implements OnInit {
  private teamService     = inject(TeamService);
  private teamTypeService = inject(TeamTypeService);
  private message         = inject(NzMessageService);
  private fb              = inject(FormBuilder);

  loading = false;
  saving  = false;

  // CSV import
  importModalVisible = false;
  importFile: File | null = null;
  importing = false;
  importResult: { successCount: number; errorCount: number; errors: string[] } | null = null;

  // Teams
  flatTeams: TeamDTO[] = [];
  editingTeam: TeamDTO | null = null;
  teamModalVisible = false;
  teamForm!: FormGroup;

  // Team types
  teamTypes: TeamTypeDTO[] = [];
  editingTeamType: TeamTypeDTO | null = null;
  teamTypeModalVisible = false;
  savingTeamType = false;
  teamTypeForm!: FormGroup;

  // Categories drawer
  drawerVisible      = false;
  drawerTitle        = '';
  loadingCategories  = false;
  savingCategories   = false;
  selectedTeam: TeamDTO | null = null;
  totalCategoryRows: CategoryRow[]     = [];
  remainingCategoryRows: CategoryRow[] = [];

  get remainingTotal(): number {
    return this.remainingCategoryRows.reduce((s, r) => s + (r.allocationPct ?? 0), 0);
  }

  get totalCatSum(): number {
    return this.totalCategoryRows.reduce((s, r) => s + (r.allocationPct ?? 0), 0);
  }

  get canSave(): boolean {
    const hasTotalCats     = this.totalCategoryRows.length > 0;
    const hasRemainingCats = this.remainingCategoryRows.length > 0;
    if (this.totalCatSum > 100) return false;
    if (hasTotalCats && hasRemainingCats) {
      const noRemainingCapacity = this.totalCatSum === 100;
      return noRemainingCapacity ? this.remainingTotal === 0 : this.remainingTotal === 100;
    }
    if (hasTotalCats)     return true; // sum already checked <= 100 above
    if (hasRemainingCats) return this.remainingTotal === 100;
    return true;
  }

  readonly pctFormatter = (v: number) => `${v}%`;
  readonly pctParser    = (v: string) => Number(v.replace('%', ''));

  get categoryForms(): FormArray {
    return this.teamTypeForm.get('categories') as FormArray;
  }

  get selectableParents(): TeamDTO[] {
    return this.flatTeams.filter(t => !t.parentId && (!this.editingTeam || t.id !== this.editingTeam.id));
  }

  ngOnInit(): void {
    this.teamForm = this.fb.group({ name: ['', Validators.required], parentId: [null], teamTypeId: [null] });
    this.teamTypeForm = this.fb.group({ name: ['', Validators.required], categories: this.fb.array([]) });
    forkJoin({ teams: this.teamService.getTeams(true), types: this.teamTypeService.getAll() }).subscribe({
      next: ({ teams, types }) => {
        this.flatTeams = this.flattenTree(teams);
        this.teamTypes = types;
      },
    });
  }

  // ── CSV Import ───────────────────────────────────────────────────────────

  openImportModal(): void {
    this.importFile = null;
    this.importResult = null;
    this.importModalVisible = true;
  }

  onImportFileSelect(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) { this.importFile = file; this.importResult = null; }
  }

  onImportFileDrop(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file) { this.importFile = file; this.importResult = null; }
  }

  submitImport(): void {
    if (!this.importFile) return;
    this.importing = true;
    this.teamService.importCsv(this.importFile).subscribe({
      next: result => {
        this.importResult = result;
        this.importing = false;
        if (result.successCount > 0) this.loadData();
      },
      error: () => { this.message.error('Import failed'); this.importing = false; },
    });
  }

  downloadTemplate(): void {
    const csv = 'name,parentName,teamTypeName\nEngineering,,Development\nFrontend,Engineering,\nBackend,Engineering,\n';
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = 'teams-template.csv'; a.click();
    URL.revokeObjectURL(url);
  }

  // ── Teams ────────────────────────────────────────────────────────────────

  loadData(): void {
    this.loading = true;
    forkJoin({ teams: this.teamService.getTeams(true), types: this.teamTypeService.getAll() }).subscribe({
      next: ({ teams, types }) => {
        this.flatTeams = this.flattenTree(teams);
        this.teamTypes = types;
        this.loading = false;
      },
      error: () => { this.message.error('Failed to load'); this.loading = false; },
    });
  }

  private flattenTree(teams: TeamDTO[]): TeamDTO[] {
    const result: TeamDTO[] = [];
    const visit = (list: TeamDTO[]) => {
      for (const t of list) { result.push(t); if (t.children?.length) visit(t.children); }
    };
    visit(teams);
    return result;
  }

  getTeamName(id?: string): string {
    return this.flatTeams.find(t => t.id === id)?.name ?? id ?? '';
  }

  getTeamTypeName(id?: string): string {
    return this.teamTypes.find(tt => tt.id === id)?.name ?? '';
  }

  totalCats(tt: TeamTypeDTO) { return tt.categories.filter(c => c.isPartOfTotalCapacity); }
  remainingCats(tt: TeamTypeDTO) { return tt.categories.filter(c => c.isPartOfRemainingCapacity); }

  openTeamModal(team?: TeamDTO): void {
    this.editingTeam = team ?? null;
    this.teamForm.reset({ name: team?.name ?? '', parentId: team?.parentId ?? null, teamTypeId: team?.teamTypeId ?? null });
    this.teamModalVisible = true;
  }

  closeTeamModal(): void { this.teamModalVisible = false; this.editingTeam = null; this.teamForm.reset(); }

  submitTeamForm(): void {
    if (this.teamForm.invalid) {
      Object.values(this.teamForm.controls).forEach(c => { c.markAsDirty(); c.updateValueAndValidity(); });
      return;
    }
    const { name, parentId, teamTypeId } = this.teamForm.value;
    const body: Record<string, unknown> = { name };
    if (parentId)    body['parentId']    = parentId;
    if (teamTypeId)  body['teamTypeId']  = teamTypeId;
    this.saving = true;
    const req = this.editingTeam
      ? this.teamService.updateTeam(this.editingTeam.id, body as any)
      : this.teamService.createTeam(body as any);
    req.subscribe({
      next: () => { this.message.success(this.editingTeam ? 'Team updated' : 'Team created'); this.saving = false; this.closeTeamModal(); this.loadData(); },
      error: () => { this.message.error('Operation failed'); this.saving = false; },
    });
  }

  deleteTeam(id: string): void {
    this.teamService.deleteTeam(id).subscribe({
      next: () => { this.message.success('Team deleted'); this.loadData(); },
      error: () => this.message.error('Failed to delete team'),
    });
  }

  // ── Team Types ──────────────────────────────────────────────────────────

  openTeamTypeModal(tt?: TeamTypeDTO): void {
    this.editingTeamType = tt ?? null;
    this.teamTypeForm.reset({ name: tt?.name ?? '' });
    while (this.categoryForms.length) this.categoryForms.removeAt(0);
    (tt?.categories ?? []).forEach(c => this.categoryForms.push(this.fb.group({
      name: [c.name, Validators.required],
      capacityType: [c.isPartOfTotalCapacity ? 'total' : 'remaining', Validators.required],
    })));
    this.teamTypeModalVisible = true;
  }

  closeTeamTypeModal(): void { this.teamTypeModalVisible = false; this.editingTeamType = null; }

  addCategory(): void {
    this.categoryForms.push(this.fb.group({ name: ['', Validators.required], capacityType: ['remaining', Validators.required] }));
  }

  removeCategory(i: number): void { this.categoryForms.removeAt(i); }

  submitTeamTypeForm(): void {
    if (this.teamTypeForm.invalid) {
      this.teamTypeForm.markAllAsTouched();
      return;
    }
    const { name } = this.teamTypeForm.value;
    const categories = (this.categoryForms.value as { name: string; capacityType: string }[]).map(c => ({
      name: c.name,
      isPartOfTotalCapacity: c.capacityType === 'total',
      isPartOfRemainingCapacity: c.capacityType === 'remaining',
    }));
    const body = { name, categories };
    this.savingTeamType = true;
    const req = this.editingTeamType
      ? this.teamTypeService.update(this.editingTeamType.id, body)
      : this.teamTypeService.create(body);
    req.subscribe({
      next: () => { this.message.success(this.editingTeamType ? 'Team type updated' : 'Team type created'); this.savingTeamType = false; this.closeTeamTypeModal(); this.loadData(); },
      error: () => { this.message.error('Operation failed'); this.savingTeamType = false; },
    });
  }

  deleteTeamType(id: string): void {
    this.teamTypeService.delete(id).subscribe({
      next: () => { this.message.success('Team type deleted'); this.loadData(); },
      error: () => this.message.error('Failed to delete team type'),
    });
  }

  // ── Categories drawer ───────────────────────────────────────────────────

  openCategories(team: TeamDTO): void {
    this.selectedTeam = team;
    this.drawerTitle = `Category allocations — ${team.name}`;
    this.drawerVisible = true;
    this.loadingCategories = true;

    const teamType = this.teamTypes.find(tt => tt.id === team.teamTypeId);

    this.teamService.getCategories(team.id).subscribe({
      next: (saved: CategoryAllocationDTO[]) => {
        const pctOf = (name: string) => saved.find(s => s.categoryName === name)?.allocationPct ?? 0;
        if (teamType) {
          this.totalCategoryRows = teamType.categories
            .filter(c => c.isPartOfTotalCapacity)
            .map(c => ({ name: c.name, isPartOfTotalCapacity: true, isPartOfRemainingCapacity: false, allocationPct: pctOf(c.name) }));
          this.remainingCategoryRows = teamType.categories
            .filter(c => c.isPartOfRemainingCapacity)
            .map(c => ({ name: c.name, isPartOfTotalCapacity: false, isPartOfRemainingCapacity: true, allocationPct: pctOf(c.name) }));
        } else {
          this.totalCategoryRows = [];
          this.remainingCategoryRows = saved.map(s => ({ name: s.categoryName, isPartOfTotalCapacity: false, isPartOfRemainingCapacity: true, allocationPct: s.allocationPct }));
        }
        this.loadingCategories = false;
      },
      error: () => {
        this.initEmptyRows(teamType);
        this.loadingCategories = false;
      },
    });
  }

  private initEmptyRows(teamType?: TeamTypeDTO): void {
    if (teamType) {
      this.totalCategoryRows     = teamType.categories.filter(c => c.isPartOfTotalCapacity).map(c => ({ name: c.name, isPartOfTotalCapacity: true, isPartOfRemainingCapacity: false, allocationPct: 0 }));
      this.remainingCategoryRows = teamType.categories.filter(c => c.isPartOfRemainingCapacity).map(c => ({ name: c.name, isPartOfTotalCapacity: false, isPartOfRemainingCapacity: true, allocationPct: 0 }));
    } else {
      this.totalCategoryRows = [];
      this.remainingCategoryRows = [];
    }
  }

  closeCategories(): void {
    this.drawerVisible = false;
    this.selectedTeam = null;
    this.totalCategoryRows = [];
    this.remainingCategoryRows = [];
  }

  saveCategories(): void {
    if (!this.selectedTeam || !this.canSave) return;
    this.savingCategories = true;
    const categories = [
      ...this.totalCategoryRows.map(r => ({ categoryName: r.name, allocationPct: r.allocationPct })),
      ...this.remainingCategoryRows.map(r => ({ categoryName: r.name, allocationPct: r.allocationPct })),
    ];
    this.teamService.updateCategories(this.selectedTeam.id, { categories }).subscribe({
      next: () => { this.message.success('Allocations saved'); this.savingCategories = false; this.closeCategories(); },
      error: () => { this.message.error('Failed to save allocations'); this.savingCategories = false; },
    });
  }
}
