import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { NzMessageService } from 'ng-zorro-antd/message';
import { TeamsPageComponent } from './teams-page.component';
import { TeamService } from '../../core/services/team.service';
import { TeamDTO } from '../../core/models/team.model';
import { CategoryAllocationDTO } from '../../core/models/snapshot.model';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

// ── Helpers ─────────────────────────────────────────────────────────────────

function makeTeam(overrides: Partial<TeamDTO> = {}): TeamDTO {
  return {
    id: 'root-1',
    name: 'Root Team',
    createdAt: '2024-01-01T00:00:00',
    updatedAt: '2024-01-01T00:00:00',
    children: [],
    ...overrides,
  };
}

function buildTree(): TeamDTO[] {
  const child1 = makeTeam({ id: 'child-1', name: 'Child 1', parentId: 'root-1', children: [] });
  const child2 = makeTeam({ id: 'child-2', name: 'Child 2', parentId: 'root-1', children: [] });
  const root = makeTeam({ id: 'root-1', name: 'Root', children: [child1, child2] });
  return [root];
}

// ── Test Suite ───────────────────────────────────────────────────────────────

describe('TeamsPageComponent', () => {
  let component: TeamsPageComponent;
  let teamService: jasmine.SpyObj<TeamService>;
  let messageService: jasmine.SpyObj<NzMessageService>;

  beforeEach(() => {
    teamService = jasmine.createSpyObj<TeamService>('TeamService', [
      'getTeams', 'createTeam', 'updateTeam', 'deleteTeam',
      'getCategories', 'updateCategories',
    ]);
    messageService = jasmine.createSpyObj<NzMessageService>('NzMessageService', [
      'success', 'error',
    ]);
    teamService.getTeams.and.returnValue(of([]));

    TestBed.configureTestingModule({
      imports: [TeamsPageComponent, ReactiveFormsModule],
      providers: [
        { provide: TeamService, useValue: teamService },
        { provide: NzMessageService, useValue: messageService },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    });

    const fixture = TestBed.createComponent(TeamsPageComponent);
    component = fixture.componentInstance;
  });

  // ── Initialization ─────────────────────────────────────────────────────────

  it('should call getTeams(true) on init', () => {
    component.ngOnInit();
    expect(teamService.getTeams).toHaveBeenCalledWith(true);
  });

  it('should flatten tree on init', fakeAsync(() => {
    teamService.getTeams.and.returnValue(of(buildTree()));
    component.loadTeams();
    tick();
    expect(component.flatTeams.length).toBe(3); // root + 2 children
  }));

  it('should show error message when getTeams fails', fakeAsync(() => {
    teamService.getTeams.and.returnValue(throwError(() => new Error('Network')));
    component.loadTeams();
    tick();
    expect(messageService.error).toHaveBeenCalledWith('Failed to load teams');
  }));

  // ── flattenTree ────────────────────────────────────────────────────────────

  it('flattenTree preserves depth-first order', () => {
    const child1 = makeTeam({ id: 'c1', name: 'C1', parentId: 'r1', children: [] });
    const child2 = makeTeam({ id: 'c2', name: 'C2', parentId: 'r1', children: [] });
    const grandchild = makeTeam({ id: 'gc1', name: 'GC1', parentId: 'c1', children: [] });
    const root = makeTeam({ id: 'r1', name: 'Root', children: [{ ...child1, children: [grandchild] }, child2] });

    // Call flattenTree via loadTeams
    teamService.getTeams.and.returnValue(of([root]));
    component.loadTeams();

    const ids = component.flatTeams.map(t => t.id);
    expect(ids).toEqual(['r1', 'c1', 'gc1', 'c2']);
  });

  it('flattenTree handles empty list', () => {
    teamService.getTeams.and.returnValue(of([]));
    component.loadTeams();
    expect(component.flatTeams).toEqual([]);
  });

  // ── getTeamName ───────────────────────────────────────────────────────────

  it('getTeamName returns name for known id', fakeAsync(() => {
    teamService.getTeams.and.returnValue(of([makeTeam({ id: 'root-1', name: 'Root' })]));
    component.loadTeams();
    tick();
    expect(component.getTeamName('root-1')).toBe('Root');
  }));

  it('getTeamName returns id when team not found', () => {
    component.flatTeams = [];
    expect(component.getTeamName('unknown-id')).toBe('unknown-id');
  });

  // ── openModal / closeModal ────────────────────────────────────────────────

  it('openModal with no team sets editingTeam to null', () => {
    component.ngOnInit();
    component.openModal();
    expect(component.editingTeam).toBeNull();
    expect(component.modalVisible).toBeTrue();
  });

  it('openModal with team patches form values', () => {
    component.ngOnInit();
    const team = makeTeam({ id: 't1', name: 'MyTeam', parentId: 'p1' });
    component.openModal(team);
    expect(component.form.value.name).toBe('MyTeam');
    expect(component.editingTeam).toBe(team);
  });

  it('closeModal resets form and hides modal', () => {
    component.ngOnInit();
    component.openModal();
    component.form.patchValue({ name: 'Test' });
    component.closeModal();
    expect(component.modalVisible).toBeFalse();
    expect(component.editingTeam).toBeNull();
    expect(component.form.value.name).toBeFalsy();
  });

  // ── selectableParents ─────────────────────────────────────────────────────

  it('selectableParents excludes the team being edited', () => {
    const t1 = makeTeam({ id: 't1', name: 'T1' });
    const t2 = makeTeam({ id: 't2', name: 'T2' });
    component.flatTeams = [t1, t2];
    component.editingTeam = t1;
    expect(component.selectableParents.map(t => t.id)).not.toContain('t1');
    expect(component.selectableParents.map(t => t.id)).toContain('t2');
  });

  it('selectableParents returns all teams when not editing', () => {
    component.flatTeams = [makeTeam({ id: 't1' }), makeTeam({ id: 't2' })];
    component.editingTeam = null;
    expect(component.selectableParents).toHaveSize(2);
  });

  // ── canSave ───────────────────────────────────────────────────────────────

  it('canSave is false when plannedTotal is not 100', () => {
    component.incidentRow = { ...component.incidentRow, allocationPct: 20 };
    component.plannedRows = [
      { categoryName: 'Project', label: 'Project', icon: 'project', color: '#1890ff', allocationPct: 40 },
      { categoryName: 'Continuous Improvement', label: 'CI', icon: 'rise', color: '#52c41a', allocationPct: 30 },
      { categoryName: 'IT for IT', label: 'IT4IT', icon: 'tool', color: '#722ed1', allocationPct: 10 },
      // sum = 80, not 100
    ];
    expect(component.canSave).toBeFalse();
  });

  it('canSave is true when incident in [0,100] and planned sums to 100', () => {
    component.incidentRow = { ...component.incidentRow, allocationPct: 20 };
    component.plannedRows = [
      { categoryName: 'Project', label: 'Project', icon: 'project', color: '#1890ff', allocationPct: 50 },
      { categoryName: 'Continuous Improvement', label: 'CI', icon: 'rise', color: '#52c41a', allocationPct: 30 },
      { categoryName: 'IT for IT', label: 'IT4IT', icon: 'tool', color: '#722ed1', allocationPct: 20 },
    ];
    expect(component.canSave).toBeTrue();
  });

  it('canSave is false when incident is negative', () => {
    component.incidentRow = { ...component.incidentRow, allocationPct: -5 };
    component.plannedRows = [
      { categoryName: 'Project', label: 'Project', icon: 'project', color: '#1890ff', allocationPct: 100 },
    ];
    expect(component.canSave).toBeFalse();
  });

  it('canSave is false when incident exceeds 100', () => {
    component.incidentRow = { ...component.incidentRow, allocationPct: 110 };
    component.plannedRows = [
      { categoryName: 'Project', label: 'Project', icon: 'project', color: '#1890ff', allocationPct: 100 },
    ];
    expect(component.canSave).toBeFalse();
  });

  // ── plannedTotal ──────────────────────────────────────────────────────────

  it('plannedTotal sums allocationPct of plannedRows', () => {
    component.plannedRows = [
      { categoryName: 'Project', label: 'P', icon: 'p', color: '', allocationPct: 40 },
      { categoryName: 'CI', label: 'CI', icon: 'ci', color: '', allocationPct: 35 },
      { categoryName: 'IT4IT', label: 'IT', icon: 'it', color: '', allocationPct: 25 },
    ];
    expect(component.plannedTotal).toBe(100);
  });

  it('plannedTotal returns 0 when plannedRows is empty', () => {
    component.plannedRows = [];
    expect(component.plannedTotal).toBe(0);
  });

  // ── openCategories ────────────────────────────────────────────────────────

  it('openCategories loads existing allocations from API', fakeAsync(() => {
    const team = makeTeam({ id: 't1', name: 'MyTeam' });
    const saved: CategoryAllocationDTO[] = [
      { id: 'c1', teamId: 't1', categoryName: 'Incident', allocationPct: 25 },
      { id: 'c2', teamId: 't1', categoryName: 'Project', allocationPct: 60 },
    ];
    teamService.getCategories.and.returnValue(of(saved));

    component.openCategories(team);
    tick();

    expect(component.drawerVisible).toBeTrue();
    expect(component.incidentRow.allocationPct).toBe(25);
    const projectRow = component.plannedRows.find(r => r.categoryName === 'Project');
    expect(projectRow?.allocationPct).toBe(60);
  }));

  it('openCategories defaults to 0 on API error', fakeAsync(() => {
    const team = makeTeam({ id: 't1' });
    teamService.getCategories.and.returnValue(throwError(() => new Error()));

    component.openCategories(team);
    tick();

    expect(component.incidentRow.allocationPct).toBe(0);
    component.plannedRows.forEach(r => expect(r.allocationPct).toBe(0));
  }));

  // ── closeCategories ───────────────────────────────────────────────────────

  it('closeCategories resets drawer state', () => {
    component.drawerVisible = true;
    component.selectedTeam = makeTeam();
    component.incidentRow = { ...component.incidentRow, allocationPct: 30 };

    component.closeCategories();

    expect(component.drawerVisible).toBeFalse();
    expect(component.selectedTeam).toBeNull();
    expect(component.incidentRow.allocationPct).toBe(0);
    expect(component.plannedRows).toEqual([]);
  });

  // ── saveCategories ────────────────────────────────────────────────────────

  it('saveCategories sends combined incident + planned payload', fakeAsync(() => {
    const team = makeTeam({ id: 't1' });
    component.selectedTeam = team;
    component.incidentRow = { categoryName: 'Incident', label: 'Incident', icon: 'alert', color: '#ff4d4f', allocationPct: 20 };
    component.plannedRows = [
      { categoryName: 'Project', label: 'Project', icon: 'project', color: '#1890ff', allocationPct: 50 },
      { categoryName: 'Continuous Improvement', label: 'CI', icon: 'rise', color: '#52c41a', allocationPct: 30 },
      { categoryName: 'IT for IT', label: 'IT4IT', icon: 'tool', color: '#722ed1', allocationPct: 20 },
    ];
    teamService.updateCategories.and.returnValue(of([]));
    teamService.getTeams.and.returnValue(of([]));

    component.saveCategories();
    tick();

    const [teamId, body] = teamService.updateCategories.calls.mostRecent().args;
    expect(teamId).toBe('t1');
    expect(body.categories).toHaveSize(4);
    expect(body.categories[0]).toEqual({ categoryName: 'Incident', allocationPct: 20 });
    expect(body.categories[1]).toEqual({ categoryName: 'Project', allocationPct: 50 });
    expect(messageService.success).toHaveBeenCalledWith('Category allocations saved');
  }));

  it('saveCategories does nothing when canSave is false', () => {
    component.selectedTeam = makeTeam();
    component.incidentRow = { ...component.incidentRow, allocationPct: 20 };
    component.plannedRows = [
      { categoryName: 'Project', label: 'P', icon: 'p', color: '', allocationPct: 40 }, // planned total = 40 ≠ 100
    ];

    component.saveCategories();

    expect(teamService.updateCategories).not.toHaveBeenCalled();
  });

  it('saveCategories shows error on API failure', fakeAsync(() => {
    const team = makeTeam({ id: 't1' });
    component.selectedTeam = team;
    component.incidentRow = { categoryName: 'Incident', label: 'Incident', icon: 'alert', color: '', allocationPct: 20 };
    component.plannedRows = [
      { categoryName: 'Project', label: 'P', icon: 'p', color: '', allocationPct: 50 },
      { categoryName: 'Continuous Improvement', label: 'CI', icon: 'ci', color: '', allocationPct: 30 },
      { categoryName: 'IT for IT', label: 'IT', icon: 'it', color: '', allocationPct: 20 },
    ];
    teamService.updateCategories.and.returnValue(throwError(() => new Error()));

    component.saveCategories();
    tick();

    expect(messageService.error).toHaveBeenCalledWith('Failed to save allocations');
  }));

  // ── deleteTeam ────────────────────────────────────────────────────────────

  it('deleteTeam calls service and reloads', fakeAsync(() => {
    teamService.deleteTeam.and.returnValue(of(undefined));
    teamService.getTeams.and.returnValue(of([]));

    component.deleteTeam('t1');
    tick();

    expect(teamService.deleteTeam).toHaveBeenCalledWith('t1');
    expect(messageService.success).toHaveBeenCalledWith('Team deleted');
    expect(teamService.getTeams).toHaveBeenCalled();
  }));

  it('deleteTeam shows error on failure', fakeAsync(() => {
    teamService.deleteTeam.and.returnValue(throwError(() => new Error()));

    component.deleteTeam('t1');
    tick();

    expect(messageService.error).toHaveBeenCalledWith('Failed to delete team');
  }));
});
