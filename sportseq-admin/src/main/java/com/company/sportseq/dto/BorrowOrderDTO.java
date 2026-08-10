package com.company.sportseq.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BorrowOrderDTO {

    @NotNull(message = "借用类型不能为空")
    private Integer borrowType;

    @NotBlank(message = "借用用途不能为空")
    @Size(max = 200, message = "用途长度不能超过200")
    private String purpose;

    @NotNull(message = "预计归还日期不能为空")
    @FutureOrPresent(message = "预计归还日期不能早于今天")
    private LocalDate expectedReturnDate;

    private String remark;

    @Valid
    @NotEmpty(message = "借用明细不能为空")
    private List<BorrowItemDTO> items;
}
