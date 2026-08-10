package com.company.sportseq.service;

import com.company.sportseq.dto.MenuDTO;
import com.company.sportseq.vo.MenuVO;
import com.company.sportseq.vo.RouterVO;

import java.util.List;

public interface SysMenuService {

    List<MenuVO> tree(String menuName);

    void add(MenuDTO dto);

    void update(MenuDTO dto);

    void remove(Long id);

    List<RouterVO> routers();
}
