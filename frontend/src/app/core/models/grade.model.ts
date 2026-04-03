export interface GradeDTO {
  id: string;
  name: string;
  dailyCost: number;
}

export interface GradeCostHistoryDTO {
  id: string;
  dailyCost: number;
  effectiveFrom: string;
}

export interface GradeHistoryDTO {
  id: string;
  gradeId: string;
  gradeName: string;
  dailyCost: number;
  effectiveFrom: string;
}

export interface CostHistoryDTO {
  id: string;
  dailyCost: number;
  effectiveFrom: string;
}
