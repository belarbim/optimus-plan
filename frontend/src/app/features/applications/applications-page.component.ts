import { Component, OnInit, inject } from '@angular/core';
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
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { ApplicationService } from '../../core/services/application.service';
import { TeamService } from '../../core/services/team.service';
import { ApplicationDTO } from '../../core/models/application.model';
import { TeamDTO } from '../../core/models/team.model';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-applications-page',
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
    NzTagModule,
    NzSelectModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header
      title="Applications"
      subtitle="Manage applications and their responsible teams">
    </app-page-header>

    <div style="margin-bottom:16px; display:flex; justify-content:space-between; align-items:center; gap:12px;">
      <div style="flex:1; max-width:360px;">
        <nz-input-group [nzPrefix]="searchIcon">
          <input
            nz-input
            placeholder="Search applications..."
            [value]="searchQuery"
            (input)="onSearchInput($event)"
          />
        </nz-input-group>
        <ng-template #searchIcon><span nz-icon nzType="search"></span></ng-template>
      </div>
      <div style="display:flex; gap:8px;">
        <button nz-button nzType="default" (click)="openImportModal()">
          <span nz-icon nzType="upload"></span> Import CSV
        </button>
        <button nz-button nzType="primary" (click)="openModal()">
          <span nz-icon nzType="plus"></span> Add Application
        </button>
      </div>
    </div>

    <nz-spin [nzSpinning]="loading">
      <nz-table
        #table
        [nzData]="applications"
        [nzBordered]="true"
        [nzSize]="'middle'"
        [nzPageSize]="15"
      >
        <thead>
          <tr>
            <th [nzSortFn]="sortByName">Name</th>
            <th>Description</th>
            <th>Responsible Team</th>
            <th style="width:100px;">Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (app of table.data; track app.id) {
            <tr>
              <td><strong>{{ app.name }}</strong></td>
              <td style="color:#666;">{{ app.description || '—' }}</td>
              <td>
                @if (app.teamName) {
                  <nz-tag nzColor="blue">
                    <span nz-icon nzType="team"></span> {{ app.teamName }}
                  </nz-tag>
                } @else {
                  <span style="color:#bbb;">Unassigned</span>
                }
              </td>
              <td>
                <button nz-button nzType="link" nzSize="small" (click)="openModal(app)">
                  <span nz-icon nzType="edit"></span>
                </button>
                <button
                  nz-button nzType="link" nzDanger nzSize="small"
                  nz-popconfirm
                  nzPopconfirmTitle="Delete this application?"
                  (nzOnConfirm)="deleteApp(app.id)"
                >
                  <span nz-icon nzType="delete"></span>
                </button>
              </td>
            </tr>
          }
          @if (!loading && applications.length === 0) {
            <tr>
              <td colspan="4" style="text-align:center; padding:32px; color:#999;">
                @if (searchQuery) {
                  No applications found for "<strong>{{ searchQuery }}</strong>"
                } @else {
                  No applications yet. Click "Add Application" to create one.
                }
              </td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-spin>

    <!-- Create / Edit Modal -->
    <nz-modal
      [(nzVisible)]="modalVisible"
      [nzTitle]="editingApp ? 'Edit Application' : 'Add Application'"
      (nzOnCancel)="closeModal()"
      (nzOnOk)="submitForm()"
      [nzOkLoading]="saving"
      [nzOkText]="editingApp ? 'Save' : 'Create'"
    >
      <ng-container *nzModalContent>
        <form nz-form [formGroup]="form" nzLayout="vertical">
          <nz-form-item>
            <nz-form-label nzRequired>Name</nz-form-label>
            <nz-form-control nzErrorTip="Name is required">
              <input nz-input formControlName="name" placeholder="e.g. Customer Portal" />
            </nz-form-control>
          </nz-form-item>

          <nz-form-item>
            <nz-form-label>Description</nz-form-label>
            <nz-form-control>
              <textarea
                nz-input
                formControlName="description"
                placeholder="Brief description of this application"
                [nzAutosize]="{ minRows: 2, maxRows: 5 }"
              ></textarea>
            </nz-form-control>
          </nz-form-item>

          <nz-form-item>
            <nz-form-label>Responsible Team</nz-form-label>
            <nz-form-control>
              <nz-select
                formControlName="teamId"
                nzShowSearch
                nzAllowClear
                nzPlaceHolder="Select a team (optional)"
                style="width:100%"
              >
                @for (team of teams; track team.id) {
                  <nz-option [nzValue]="team.id" [nzLabel]="team.name"></nz-option>
                }
              </nz-select>
            </nz-form-control>
          </nz-form-item>
        </form>
      </ng-container>
    </nz-modal>

    <!-- Import CSV Modal -->
    <nz-modal
      [(nzVisible)]="importModalVisible"
      nzTitle="Import Applications from CSV"
      (nzOnCancel)="importModalVisible = false"
      [nzFooter]="null"
    >
      <ng-container *nzModalContent>
        <p style="margin-bottom:8px;">Upload a CSV file with the following columns (header row required):</p>
        <code style="display:block; background:#f5f5f5; padding:8px; border-radius:4px; font-size:12px; margin-bottom:8px;">
          name, description, teamName
        </code>
        <p style="font-size:12px; color:#888; margin-bottom:16px;">
          <em>description</em> and <em>teamName</em> are optional.
          Teams are matched by name (case-insensitive); unmatched teams are skipped gracefully.
        </p>

        <button nz-button style="margin-bottom:16px;" (click)="downloadTemplate()">
          <span nz-icon nzType="download"></span> Download Template
        </button>

        <div
          style="border:2px dashed #d9d9d9; border-radius:4px; padding:24px; text-align:center; cursor:pointer;"
          (click)="fileInput.click()"
          (dragover)="$event.preventDefault()"
          (drop)="onFileDrop($event)"
        >
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
                {{ importResult.errorCount }} with issues
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
export class ApplicationsPageComponent implements OnInit {
  private svc = inject(ApplicationService);
  private teamSvc = inject(TeamService);
  private fb = inject(FormBuilder);
  private message = inject(NzMessageService);

  applications: ApplicationDTO[] = [];
  teams: TeamDTO[] = [];
  loading = false;
  saving = false;
  modalVisible = false;
  editingApp: ApplicationDTO | null = null;
  searchQuery = '';

  importModalVisible = false;
  importFile: File | null = null;
  importing = false;
  importResult: { successCount: number; errorCount: number; errors: string[] } | null = null;

  private searchSubject = new Subject<string>();

  form: FormGroup = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    teamId: [null],
  });

  sortByName = (a: ApplicationDTO, b: ApplicationDTO) => a.name.localeCompare(b.name);

  ngOnInit(): void {
    this.loadApplications();
    this.loadTeams();
    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe(q => {
      this.fetchApplications(q);
    });
  }

  loadApplications(): void {
    this.fetchApplications(this.searchQuery);
  }

  private fetchApplications(query: string): void {
    this.loading = true;
    const obs$ = query ? this.svc.search(query) : this.svc.getAll();
    obs$.subscribe({
      next: apps => { this.applications = apps; this.loading = false; },
      error: () => { this.message.error('Failed to load applications'); this.loading = false; },
    });
  }

  loadTeams(): void {
    this.teamSvc.getTeams().subscribe({ next: t => this.teams = t });
  }

  onSearchInput(event: Event): void {
    const query = (event.target as HTMLInputElement).value;
    this.searchQuery = query;
    this.searchSubject.next(query);
  }

  openModal(app?: ApplicationDTO): void {
    this.editingApp = app ?? null;
    this.form.reset({
      name: app?.name ?? '',
      description: app?.description ?? '',
      teamId: app?.teamId ?? null,
    });
    this.modalVisible = true;
  }

  closeModal(): void {
    this.modalVisible = false;
    this.editingApp = null;
    this.form.reset();
  }

  submitForm(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    const { name, description, teamId } = this.form.value;
    const body = { name, description: description || null, teamId: teamId || null };

    const obs$ = this.editingApp
      ? this.svc.update(this.editingApp.id, body)
      : this.svc.create(body);

    obs$.subscribe({
      next: () => {
        this.saving = false;
        this.message.success(this.editingApp ? 'Application updated' : 'Application created');
        this.closeModal();
        this.loadApplications();
      },
      error: (err) => {
        this.saving = false;
        this.message.error(err?.error?.message ?? 'An error occurred');
      },
    });
  }

  deleteApp(id: string): void {
    this.svc.delete(id).subscribe({
      next: () => { this.message.success('Application deleted'); this.loadApplications(); },
      error: () => this.message.error('Failed to delete application'),
    });
  }

  // ── Import ──────────────────────────────────────────────────────────────────

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
    this.svc.importCsv(this.importFile).subscribe({
      next: result => {
        this.importResult = result;
        this.importing = false;
        if (result.successCount > 0) this.loadApplications();
      },
      error: () => { this.message.error('Import failed'); this.importing = false; },
    });
  }

  downloadTemplate(): void {
    const csv = 'name,description,teamName\nCustomer Portal,Main customer-facing web application,Team Alpha\nInternal Dashboard,Back-office reporting tool,\n';
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'applications-template.csv';
    a.click();
    URL.revokeObjectURL(url);
  }
}
