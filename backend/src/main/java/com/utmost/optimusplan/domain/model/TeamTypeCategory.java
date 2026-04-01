package com.utmost.optimusplan.domain.model;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamTypeCategory {

    private UUID    id;
    private UUID    teamTypeId;
    private String  name;

    /** True → the allocation % is expressed as % of total capacity (e.g. Incident). */
    private boolean isPartOfTotalCapacity;

    /** True → the allocation % is expressed as % of remaining capacity (e.g. Project, CI). */
    private boolean isPartOfRemainingCapacity;
}
