package com.company.sportseq.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.dto.DeptDTO;
import com.company.sportseq.entity.SysDept;
import com.company.sportseq.entity.SysUser;
import com.company.sportseq.mapper.SysDeptMapper;
import com.company.sportseq.mapper.SysUserMapper;
import com.company.sportseq.service.SysDeptService;
import com.company.sportseq.vo.DeptVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl implements SysDeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<DeptVO> tree(String deptName) {
        List<SysDept> depts = deptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .like(deptName != null && !deptName.isBlank(), SysDept::getDeptName, deptName)
                .orderByAsc(SysDept::getOrderNum));
        return buildTree(depts);
    }

    @Override
    public void add(DeptDTO dto) {
        SysDept dept = new SysDept();
        dept.setParentId(dto.getParentId());
        dept.setDeptName(dto.getDeptName());
        dept.setOrderNum(dto.getOrderNum() == null ? 0 : dto.getOrderNum());
        dept.setLeader(dto.getLeader());
        dept.setPhone(dto.getPhone());
        dept.setEmail(dto.getEmail());
        dept.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        dept.setAncestors(buildAncestors(dto.getParentId()));
        deptMapper.insert(dept);
    }

    @Override
    public void update(DeptDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "部门ID不能为空");
        }
        if (dto.getId().equals(dto.getParentId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "父部门不能是自己");
        }
        SysDept dept = new SysDept();
        dept.setId(dto.getId());
        dept.setParentId(dto.getParentId());
        dept.setDeptName(dto.getDeptName());
        dept.setOrderNum(dto.getOrderNum() == null ? 0 : dto.getOrderNum());
        dept.setLeader(dto.getLeader());
        dept.setPhone(dto.getPhone());
        dept.setEmail(dto.getEmail());
        dept.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        dept.setAncestors(buildAncestors(dto.getParentId()));
        deptMapper.updateById(dept);
    }

    @Override
    public void remove(Long id) {
        Long children = deptMapper.selectCount(
                Wrappers.<SysDept>lambdaQuery().eq(SysDept::getParentId, id));
        if (children > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "存在子部门，无法删除");
        }
        Long userCount = userMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getDeptId, id));
        if (userCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "部门下存在用户，无法删除");
        }
        deptMapper.deleteById(id);
    }

    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId == 0) {
            return "0";
        }
        SysDept parent = deptMapper.selectById(parentId);
        if (parent == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "父部门不存在");
        }
        return parent.getAncestors() + "," + parentId;
    }

    private List<DeptVO> buildTree(List<SysDept> depts) {
        Map<Long, DeptVO> voMap = depts.stream()
                .collect(Collectors.toMap(SysDept::getId, this::toVo));
        List<DeptVO> roots = new ArrayList<>();
        for (SysDept dept : depts) {
            DeptVO vo = voMap.get(dept.getId());
            DeptVO parent = voMap.get(dept.getParentId());
            if (parent == null) {
                roots.add(vo);
            } else {
                addChild(parent, vo);
            }
        }
        return roots;
    }

    private void addChild(DeptVO parent, DeptVO child) {
        List<DeptVO> children = parent.children();
        if (children == null) {
            children = new ArrayList<>();
            parent = new DeptVO(parent.id(), parent.parentId(), parent.deptName(), parent.orderNum(),
                    parent.leader(), parent.phone(), parent.email(), parent.status(), children);
        }
        children.add(child);
    }

    private DeptVO toVo(SysDept dept) {
        return new DeptVO(dept.getId(), dept.getParentId(), dept.getDeptName(), dept.getOrderNum(),
                dept.getLeader(), dept.getPhone(), dept.getEmail(), dept.getStatus(), new ArrayList<>());
    }
}
