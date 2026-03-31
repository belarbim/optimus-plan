export interface CategoryBreakdownItem {
  categoryName: string;
  manDays: number;
}

export interface EmployeeContribution {
  employeeId: string;
  employeeName: string;
  teamName?: string;
  allocationPct: number;
  roleType: string;
  roleWeight: number;
  presenceFactor: number;
  totalManDays: number;
}

export interface CapacityResultDTO {
  teamId: string;
  teamName: string;
  month: string;
  totalCapacity: number;
  categoryBreakdown: CategoryBreakdownItem[];
  employeeContributions: EmployeeContribution[];
}

export interface RemainingCapacityDTO {
  date: string;
  adjustedDate: string;
  remainingBusinessDays: number;
  totalBusinessDays: number;
  totalRemaining: number;
  categoryBreakdown: CategoryBreakdownItem[];
}

export interface RollupCapacityDTO {
  ownCapacity: CapacityResultDTO;
  subTeamCapacities: CapacityResultDTO[];
  consolidatedTotal: number;
}

export interface SimulationRequest {
  [key: string]: unknown;
}

export interface SimulationResultDTO {
  baseline: CapacityResultDTO;
  simulated: CapacityResultDTO;
  deltas: Record<string, number>;
  warnings: string[];
}

