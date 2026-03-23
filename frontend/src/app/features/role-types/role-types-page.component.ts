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
import { NzTagModule } from 'ng-zorro-antd/tag';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { RoleTypeService } from '../../core/services/role-type.service';
import { RoleTypeConfigDTO } from '../../core/models/role-type.model';

@Component({
  selector: 'app-role-types-page',
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
    NzTagModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header title="Role Types" subtitle="Configure role types and weights"></app-page-header>

    <div style="margin-bottom: 16px; display: flex; justify-content: flex-end;">
      <button nz-button nzType="primary" (click)="openModal()">
        <span nz-icon nzType="plus"></span> Add Role Type
      </button>
    </div>

    <nz-spin [nzSpinning]="loading">
      <nz-table
        #table
        [nzData]="roleTypes"
        [nzBordered]="true"
        [nzSize]="'middle'"
      >
        <thead>
          <tr>
            <th>Role Type</th>
            <th>Default Weight</th>
            <th>Description</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (r of table.data; track r.id) {
            <tr>
              <td><nz-tag nzColor="blue">{{ r.roleType }}</nz-tag></td>
              <td>{{ r.defaultWeight }}</td>
              <td>{{ r.description }}</td>
              <td>
                <button nz-button nzType="link" (click)="openModal(r)">
                  <span nz-icon nzType="edit"></span>
                </button>
                <button
                  nz-button nzType="link" nzDanger
                  nz-popconfirm
                  nzPopconfirmTitle="Delete this role type?"
                  (nzOnConfirm)="deleteRoleType(r.id!)"
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
      [nzTitle]="editingRole ? 'Edit Role Type' : 'Add Role Type'"
      (nzOnCancel)="closeModal()"
      (nzOnOk)="submitForm()"
      [nzOkLoading]="saving"
    >
      <ng-container *nzModalContent>
        <form nz-form [formGroup]="form" nzLayout="vertical">
          <nz-form-item>
            <nz-form-label nzRequired>Role Type</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <input nz-input formControlName="roleType" placeholder="e.g. DEVELOPER" />
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Default Weight</nz-form-label>
            <nz-form-control nzErrorTip="Required">
              <nz-input-number formControlName="defaultWeight" [nzMin]="0" [nzMax]="1" [nzStep]="0.1" style="width:100%"></nz-input-number>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label>Description</nz-form-label>
            <nz-form-control>
              <textarea nz-input formControlName="description" [nzAutosize]="{ minRows: 2, maxRows: 5 }" placeholder="Description..."></textarea>
            </nz-form-control>
          </nz-form-item>
        </form>
      </ng-container>
    </nz-modal>
  `,
})
export class RoleTypesPageComponent implements OnInit {
  private roleTypeService = inject(RoleTypeService);
  private message = inject(NzMessageService);
  private fb = inject(FormBuilder);

  loading = false;
  saving = false;
  modalVisible = false;
  roleTypes: RoleTypeConfigDTO[] = [];
  editingRole: RoleTypeConfigDTO | null = null;
  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      roleType: ['', Validators.required],
      defaultWeight: [1, Validators.required],
      description: [''],
    });
    this.loadRoleTypes();
  }

  loadRoleTypes(): void {
    this.loading = true;
    this.roleTypeService.getRoleTypes().subscribe({
      next: r => { this.roleTypes = r; this.loading = false; },
      error: () => { this.message.error('Failed to load'); this.loading = false; },
    });
  }

  openModal(role?: RoleTypeConfigDTO): void {
    this.editingRole = role ?? null;
    if (role) {
      this.form.patchValue({ roleType: role.roleType, defaultWeight: role.defaultWeight, description: role.description });
    } else {
      this.form.reset({ defaultWeight: 1 });
    }
    this.modalVisible = true;
  }

  closeModal(): void {
    this.modalVisible = false;
    this.editingRole = null;
  }

  submitForm(): void {
    if (this.form.invalid) { Object.values(this.form.controls).forEach(c => { c.markAsDirty(); c.updateValueAndValidity(); }); return; }
    this.saving = true;
    const req = this.editingRole
      ? this.roleTypeService.updateRoleType(this.editingRole.id!, this.form.value)
      : this.roleTypeService.createRoleType(this.form.value);

    req.subscribe({
      next: () => { this.message.success(this.editingRole ? 'Updated' : 'Created'); this.saving = false; this.closeModal(); this.loadRoleTypes(); },
      error: () => { this.message.error('Operation failed'); this.saving = false; },
    });
  }

  deleteRoleType(id: string): void {
    this.roleTypeService.deleteRoleType(id).subscribe({
      next: () => { this.message.success('Deleted'); this.loadRoleTypes(); },
      error: () => this.message.error('Failed to delete'),
    });
  }
}
