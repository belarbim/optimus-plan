import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { PageHeaderComponent } from '../../shared/atoms/page-header/page-header.component';
import { AlertService } from '../../core/services/alert.service';
import { TeamService } from '../../core/services/team.service';
import { CapacityAlertDTO } from '../../core/models/alert.model';
import { TeamDTO } from '../../core/models/team.model';

@Component({
  selector: 'app-alerts-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    NzButtonModule,
    NzFormModule,
    NzInputModule,
    NzInputNumberModule,
    NzSelectModule,
    NzSwitchModule,
    NzPopconfirmModule,
    NzIconModule,
    NzSpinModule,
    NzCardModule,
    NzTagModule,
    NzDividerModule,
    NzAlertModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header title="Alerts" subtitle="Configure capacity threshold alerts per team"></app-page-header>

    <div nz-row [nzGutter]="24">
      <!-- Team Selector -->
      <div nz-col [nzXs]="24" [nzMd]="8">
        <nz-card nzTitle="Select Team">
          <nz-select
            [(ngModel)]="selectedTeamId"
            nzPlaceHolder="Select a team"
            style="width:100%"
            (ngModelChange)="onTeamSelect()"
          >
            @for (t of teams; track t.id) {
              <nz-option [nzValue]="t.id" [nzLabel]="t.name"></nz-option>
            }
          </nz-select>
        </nz-card>
      </div>

      <!-- Alert Config -->
      <div nz-col [nzXs]="24" [nzMd]="16">
        <nz-card nzTitle="Alert Configuration">
          <nz-spin [nzSpinning]="loading">
            @if (!selectedTeamId) {
              <nz-alert nzType="info" nzMessage="Please select a team to manage its alert." nzShowIcon></nz-alert>
            } @else {
              @if (currentAlert) {
                <div style="margin-bottom:16px;">
                  <nz-tag nzColor="blue">Existing Alert</nz-tag>
                  <span style="margin-left:8px;">ID: {{ currentAlert.id }}</span>
                </div>
              }

              <form nz-form [formGroup]="form" nzLayout="vertical">
                <nz-form-item>
                  <nz-form-label nzRequired>Threshold (Man Days)</nz-form-label>
                  <nz-form-control nzErrorTip="Required">
                    <nz-input-number
                      formControlName="thresholdManDays"
                      [nzMin]="0"
                      [nzStep]="0.5"
                      style="width:100%"
                      nzPlaceHolder="e.g. 10"
                    ></nz-input-number>
                  </nz-form-control>
                </nz-form-item>
                <nz-form-item>
                  <nz-form-label>Enabled</nz-form-label>
                  <nz-form-control>
                    <nz-switch formControlName="enabled" nzCheckedChildren="On" nzUnCheckedChildren="Off"></nz-switch>
                  </nz-form-control>
                </nz-form-item>

                <div style="display:flex; gap:12px; margin-top:8px;">
                  <button nz-button nzType="primary" (click)="saveAlert()" [nzLoading]="saving">
                    <span nz-icon nzType="check-circle"></span>
                    {{ currentAlert ? 'Update Alert' : 'Create Alert' }}
                  </button>
                  @if (currentAlert) {
                    <button
                      nz-button nzType="default" nzDanger
                      nz-popconfirm
                      nzPopconfirmTitle="Delete this alert?"
                      (nzOnConfirm)="deleteAlert()"
                    >
                      <span nz-icon nzType="delete"></span> Delete Alert
                    </button>
                  }
                </div>
              </form>
            }
          </nz-spin>
        </nz-card>
      </div>
    </div>
  `,
})
export class AlertsPageComponent implements OnInit {
  private alertService = inject(AlertService);
  private teamService = inject(TeamService);
  private message = inject(NzMessageService);
  private fb = inject(FormBuilder);

  teams: TeamDTO[] = [];
  selectedTeamId: string | null = null;
  currentAlert: CapacityAlertDTO | null = null;
  loading = false;
  saving = false;

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      thresholdManDays: [null, Validators.required],
      enabled: [true],
    });
    this.teamService.getTeams().subscribe({ next: t => (this.teams = t) });
  }

  onTeamSelect(): void {
    if (!this.selectedTeamId) return;
    this.loading = true;
    this.currentAlert = null;
    this.alertService.getByTeam(this.selectedTeamId).subscribe({
      next: alert => {
        this.currentAlert = alert;
        this.form.patchValue({ thresholdManDays: alert.thresholdManDays, enabled: alert.enabled });
        this.loading = false;
      },
      error: () => {
        // No alert exists yet
        this.form.reset({ enabled: true });
        this.loading = false;
      },
    });
  }

  saveAlert(): void {
    if (this.form.invalid || !this.selectedTeamId) {
      Object.values(this.form.controls).forEach(c => { c.markAsDirty(); c.updateValueAndValidity(); });
      return;
    }
    this.saving = true;
    const body = { teamId: this.selectedTeamId, ...this.form.value };
    this.alertService.createAlert(body).subscribe({
      next: alert => {
        this.currentAlert = alert;
        this.message.success('Alert saved');
        this.saving = false;
      },
      error: () => { this.message.error('Failed to save alert'); this.saving = false; },
    });
  }

  deleteAlert(): void {
    if (!this.currentAlert?.id) return;
    this.alertService.deleteAlert(this.currentAlert.id).subscribe({
      next: () => {
        this.message.success('Alert deleted');
        this.currentAlert = null;
        this.form.reset({ enabled: true });
      },
      error: () => this.message.error('Failed to delete alert'),
    });
  }
}
