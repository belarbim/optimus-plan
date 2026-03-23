export interface CapacitySnapshotDTO {
  id: string;
  teamId: string;
  snapshotMonth: string;
  categoryName: string;
  capacityManDays: number;
  createdAt: string;
}

export interface CategoryAllocationDTO {
  id: string;
  teamId: string;
  categoryName: string;
  allocationPct: number;
}

export interface UpdateCategoriesRequest {
  categories: { categoryName: string; allocationPct: number }[];
}
