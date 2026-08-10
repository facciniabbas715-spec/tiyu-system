package com.company.sportseq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.dto.MenuDTO;
import com.company.sportseq.entity.SysMenu;
import com.company.sportseq.entity.SysRole;
import com.company.sportseq.entity.SysRoleMenu;
import com.company.sportseq.mapper.SysMenuMapper;
import com.company.sportseq.mapper.SysRoleMapper;
import com.company.sportseq.mapper.SysRoleMenuMapper;
import com.company.sportseq.security.LoginUser;
import com.company.sportseq.security.SecurityUtils;
import com.company.sportseq.service.SysMenuService;
import com.company.sportseq.vo.MenuVO;
import com.company.sportseq.vo.RouterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<MenuVO> tree(String menuName) {
        List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .like(StrUtil.isNotBlank(menuName), SysMenu::getMenuName, menuName)
                .orderByAsc(SysMenu::getOrderNum));
        Map<Long, MenuVO> voMap = menus.stream()
                .collect(Collectors.toMap(SysMenu::getId, this::toVo));
        List<MenuVO> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            MenuVO vo = voMap.get(menu.getId());
            MenuVO parent = voMap.get(menu.getParentId());
            if (parent == null) {
                roots.add(vo);
            } else {
                appendChild(parent, vo);
            }
        }
        return roots;
    }

    @Override
    public void add(MenuDTO dto) {
        SysMenu menu = new SysMenu();
        applyMenuFields(menu, dto);
        menuMapper.insert(menu);
    }

    @Override
    public void update(MenuDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "菜单ID不能为空");
        }
        if (dto.getId().equals(dto.getParentId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "父菜单不能是自己");
        }
        validateNoCycle(dto.getId(), dto.getParentId());
        SysMenu menu = new SysMenu();
        menu.setId(dto.getId());
        applyMenuFields(menu, dto);
        menuMapper.updateById(menu);
    }

    /**
     * 沿新父级祖先链校验，禁止把菜单挂到自身子孙下，避免树成环导致 routers() 栈溢出。
     */
    private void validateNoCycle(Long menuId, Long parentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        Long cursor = parentId;
        Set<Long> visited = new HashSet<>();
        while (cursor != null && cursor != 0) {
            if (cursor.equals(menuId)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "父菜单不能是自身或其子菜单");
            }
            if (!visited.add(cursor)) {
                break;
            }
            SysMenu parent = menuMapper.selectById(cursor);
            if (parent == null) {
                break;
            }
            cursor = parent.getParentId();
        }
    }

    @Override
    public void remove(Long id) {
        Long children = menuMapper.selectCount(
                Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, id));
        if (children > 0) {
            throw new BizException(ErrorCode.CATEGORY_HAS_CHILDREN, "存在子菜单，无法删除");
        }
        Long roleCount = roleMenuMapper.selectCount(
                Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getMenuId, id));
        if (roleCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "菜单已分配给角色，无法删除");
        }
        menuMapper.deleteById(id);
    }

    @Override
    public List<RouterVO> routers() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        List<SysMenu> menus;
        if (loginUser.getRoles().contains("admin")) {
            menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                    .in(SysMenu::getMenuType, "M", "C")
                    .eq(SysMenu::getVisible, 1)
                    .eq(SysMenu::getStatus, 1)
                    .orderByAsc(SysMenu::getOrderNum));
        } else {
            List<Long> roleIds = roleMapper.selectList(
                            Wrappers.<SysRole>lambdaQuery().in(SysRole::getRoleKey, loginUser.getRoles()))
                    .stream().map(SysRole::getId).toList();
            if (roleIds.isEmpty()) {
                return List.of();
            }
            List<Long> menuIds = roleMenuMapper.selectList(
                            Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, roleIds))
                    .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
            if (menuIds.isEmpty()) {
                return List.of();
            }
            menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                    .in(SysMenu::getId, menuIds)
                    .in(SysMenu::getMenuType, "M", "C")
                    .eq(SysMenu::getVisible, 1)
                    .eq(SysMenu::getStatus, 1)
                    .orderByAsc(SysMenu::getOrderNum));
        }
        return buildRouterTree(menus);
    }

    private List<RouterVO> buildRouterTree(List<SysMenu> menus) {
        Map<Long, SysMenu> map = menus.stream()
                .collect(Collectors.toMap(SysMenu::getId, m -> m));
        Set<Long> rootIds = new LinkedHashSet<>();
        for (SysMenu menu : menus) {
            SysMenu parent = map.get(menu.getParentId());
            if (parent == null) {
                rootIds.add(menu.getId());
            }
        }
        return rootIds.stream()
                .map(map::get)
                .map(menu -> toRouter(menu, map))
                .toList();
    }

    private RouterVO toRouter(SysMenu menu, Map<Long, SysMenu> all) {
        List<SysMenu> children = all.values().stream()
                .filter(m -> menu.getId().equals(m.getParentId()))
                .sorted(Comparator.comparing(SysMenu::getOrderNum))
                .toList();
        List<RouterVO> childRouters = children.stream()
                .map(child -> toRouter(child, all))
                .toList();
        String component = "M".equals(menu.getMenuType()) ? "Layout" : menu.getComponent();
        return new RouterVO(
                menu.getPath(),
                component,
                "Menu_" + menu.getId(),
                new RouterVO.Meta(menu.getMenuName(), menu.getIcon()),
                childRouters.isEmpty() ? null : childRouters);
    }

    private void applyMenuFields(SysMenu menu, MenuDTO dto) {
        menu.setParentId(dto.getParentId());
        menu.setMenuName(dto.getMenuName());
        menu.setOrderNum(dto.getOrderNum() == null ? 0 : dto.getOrderNum());
        menu.setMenuType(dto.getMenuType());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setQuery(dto.getQuery());
        menu.setPerms(dto.getPerms());
        menu.setIcon(dto.getIcon());
        menu.setVisible(dto.getVisible() == null ? 1 : dto.getVisible());
        menu.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    private MenuVO toVo(SysMenu menu) {
        return new MenuVO(menu.getId(), menu.getParentId(), menu.getMenuName(), menu.getOrderNum(),
                menu.getMenuType(), menu.getPath(), menu.getComponent(), menu.getPerms(),
                menu.getIcon(), menu.getVisible(), menu.getStatus(), new ArrayList<>());
    }

    private void appendChild(MenuVO parent, MenuVO child) {
        parent.children().add(child);
    }
}
