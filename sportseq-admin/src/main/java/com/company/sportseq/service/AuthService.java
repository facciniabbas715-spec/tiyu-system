package com.company.sportseq.service;

import com.company.sportseq.dto.LoginDTO;
import com.company.sportseq.dto.UpdatePasswordDTO;
import com.company.sportseq.vo.CaptchaVO;
import com.company.sportseq.vo.LoginVO;
import com.company.sportseq.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    CaptchaVO captcha();

    LoginVO login(LoginDTO dto, HttpServletRequest request);

    void logout(HttpServletRequest request);

    UserInfoVO info();

    void updatePassword(UpdatePasswordDTO dto, HttpServletRequest request);
}
