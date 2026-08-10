package com.company.sportseq.common.exception;

import com.company.sportseq.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅用于全局异常处理器测试的控制器（不进入生产代码）。
 */
@Validated
@RestController
public class TestController {

    @GetMapping("/test/biz")
    public Result<String> biz() {
        throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
    }

    @PostMapping("/test/valid")
    public Result<String> valid(@Valid @RequestBody TestDto dto) {
        return Result.success("ok");
    }

    public record TestDto(@NotBlank(message = "名称不能为空") String name) {
    }
}
