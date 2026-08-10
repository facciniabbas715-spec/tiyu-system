package com.company.sportseq.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局异常处理器行为验证：业务异常返回业务码，参数校验返回 1001 + HTTP 400。
 */
@WebMvcTest(TestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void bizException_shouldReturnErrorCodeWithHttp200() throws Exception {
        mockMvc.perform(get("/test/biz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.STOCK_NOT_ENOUGH.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.STOCK_NOT_ENOUGH.getMessage()));
    }

    @Test
    void validationError_shouldReturn400AndCode1001() throws Exception {
        mockMvc.perform(post("/test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("name: 名称不能为空"));
    }
}
