package com.company.sportseq.controller;

import com.company.sportseq.common.result.Result;
import com.company.sportseq.dto.LoginDTO;
import com.company.sportseq.dto.UpdatePasswordDTO;
import com.company.sportseq.service.AuthService;
import com.company.sportseq.vo.CaptchaVO;
import com.company.sportseq.vo.LoginVO;
import com.company.sportseq.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.success(authService.captcha());
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        return Result.success(authService.login(dto, request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.success(authService.info());
    }

    @PutMapping("/updatePassword")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto, HttpServletRequest request) {
        authService.updatePassword(dto, request);
        return Result.success();
    }
}
