package com.humanrsc.datamodel.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateEmployeeAssignmentDTO {
    private String employeeId;
    private String positionId;
    private String unitId;
    private String managerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal salary;
    private String currency;
    private String movementReason;
    private String notes;
}
