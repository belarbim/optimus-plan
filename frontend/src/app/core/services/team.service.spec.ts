import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TeamService } from './team.service';
import { TeamDTO } from '../models/team.model';
import { CategoryAllocationDTO, UpdateCategoriesRequest } from '../models/snapshot.model';
import { environment } from '../../../environments/environment';

describe('TeamService', () => {
  let service: TeamService;
  let http: HttpTestingController;
  const base = `${environment.apiUrl}/teams`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TeamService],
    });
    service = TestBed.inject(TeamService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ── getTeams ───────────────────────────────────────────────────────────────

  it('getTeams() sends GET with tree=false param', () => {
    service.getTeams(false).subscribe();
    const req = http.expectOne(r => r.url === base && r.params.get('tree') === 'false');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getTeams() sends GET with tree=true param', () => {
    service.getTeams(true).subscribe();
    const req = http.expectOne(r => r.url === base && r.params.get('tree') === 'true');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getTeams() emits the response array', () => {
    const teams: TeamDTO[] = [
      { id: '1', name: 'Alpha', createdAt: '2024-01-01', updatedAt: '2024-01-01' },
    ];
    let result: TeamDTO[] | undefined;
    service.getTeams().subscribe(r => (result = r));
    http.expectOne(r => r.url === base).flush(teams);
    expect(result).toEqual(teams);
  });

  // ── getTeam ────────────────────────────────────────────────────────────────

  it('getTeam() sends GET to /teams/:id', () => {
    const id = 'abc-123';
    service.getTeam(id).subscribe();
    const req = http.expectOne(`${base}/${id}`);
    expect(req.request.method).toBe('GET');
    req.flush({ id, name: 'Alpha', createdAt: '', updatedAt: '' });
  });

  // ── createTeam ─────────────────────────────────────────────────────────────

  it('createTeam() sends POST with body', () => {
    const body = { name: 'New Team' };
    service.createTeam(body).subscribe();
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ id: '1', name: 'New Team', createdAt: '', updatedAt: '' });
  });

  it('createTeam() sends parentId when provided', () => {
    const body = { name: 'Child', parentId: 'parent-id' };
    service.createTeam(body).subscribe();
    const req = http.expectOne(base);
    expect(req.request.body.parentId).toBe('parent-id');
    req.flush({ id: '2', name: 'Child', parentId: 'parent-id', createdAt: '', updatedAt: '' });
  });

  it('createTeam() emits the created team', () => {
    const created: TeamDTO = { id: '1', name: 'Alpha', createdAt: '2024-01-01', updatedAt: '2024-01-01' };
    let result: TeamDTO | undefined;
    service.createTeam({ name: 'Alpha' }).subscribe(r => (result = r));
    http.expectOne(base).flush(created);
    expect(result).toEqual(created);
  });

  // ── updateTeam ─────────────────────────────────────────────────────────────

  it('updateTeam() sends PUT to /teams/:id', () => {
    const id = 'team-1';
    const body = { name: 'Updated' };
    service.updateTeam(id, body).subscribe();
    const req = http.expectOne(`${base}/${id}`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush({ id, name: 'Updated', createdAt: '', updatedAt: '' });
  });

  // ── deleteTeam ─────────────────────────────────────────────────────────────

  it('deleteTeam() sends DELETE to /teams/:id', () => {
    const id = 'team-1';
    service.deleteTeam(id).subscribe();
    const req = http.expectOne(`${base}/${id}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  // ── getCategories ──────────────────────────────────────────────────────────

  it('getCategories() sends GET to /teams/:id/categories', () => {
    const teamId = 'team-1';
    service.getCategories(teamId).subscribe();
    const req = http.expectOne(`${base}/${teamId}/categories`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getCategories() emits category allocations', () => {
    const teamId = 'team-1';
    const categories: CategoryAllocationDTO[] = [
      { id: 'c1', teamId, categoryName: 'Project', allocationPct: 50 },
      { id: 'c2', teamId, categoryName: 'Incident', allocationPct: 20 },
    ];
    let result: CategoryAllocationDTO[] | undefined;
    service.getCategories(teamId).subscribe(r => (result = r));
    http.expectOne(`${base}/${teamId}/categories`).flush(categories);
    expect(result).toEqual(categories);
  });

  // ── updateCategories ───────────────────────────────────────────────────────

  it('updateCategories() sends PUT to /teams/:id/categories', () => {
    const teamId = 'team-1';
    const body: UpdateCategoriesRequest = {
      categories: [
        { categoryName: 'Incident', allocationPct: 20 },
        { categoryName: 'Project', allocationPct: 50 },
        { categoryName: 'Continuous Improvement', allocationPct: 20 },
        { categoryName: 'IT for IT', allocationPct: 10 },
      ],
    };
    service.updateCategories(teamId, body).subscribe();
    const req = http.expectOne(`${base}/${teamId}/categories`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush(body.categories.map((c, i) => ({ id: `c${i}`, teamId, ...c })));
  });
});
