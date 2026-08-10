package com.company.sportseq.service;

import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.ResetPasswordDTO;
import com.company.sportseq.dto.UserDTO;
import com.company.sportseq.dto.UserStatusDTO;
import com.company.sportseq.vo.UserVO;

public interface SysUserService {

    PageResult<UserVO> page(long current, long size, String username, String phone, Integer status, Long deptId);

    UserVO detail(Long id);

    void add(UserDTO dto);

    void update(UserDTO dto);

    void remove(Long id);

    void resetPassword(ResetPasswordDTO dto);

    void changeStatus(UserStatusDTO dto);
}
