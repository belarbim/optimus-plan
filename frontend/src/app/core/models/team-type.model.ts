export interface TeamTypeCategoryDTO {
  id: string;
  name: string;
  isPartOfTotalCapacity: boolean;
  isPartOfRemainingCapacity: boolean;
}

export interface TeamTypeDTO {
  id: string;
  name: string;
  categories: TeamTypeCategoryDTO[];
}

export interface TeamTypeCategoryRequest {
  name: string;
  isPartOfTotalCapacity: boolean;
  isPartOfRemainingCapacity: boolean;
}

export interface TeamTypeRequest {
  name: string;
  categories: TeamTypeCategoryRequest[];
}
