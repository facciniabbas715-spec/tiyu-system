package com.company.sportseq.controller;

import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.dto.BorrowAuditDTO;
import com.company.sportseq.dto.BorrowExtendDTO;
import com.company.sportseq.dto.BorrowOrderDTO;
import com.company.sportseq.service.BorrowService;
import com.company.sportseq.vo.BorrowOrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('borrow:list')")
    public Result<PageResult<BorrowOrderVO>> page(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String orderNo,
                                                  @RequestParam(required = false) String username,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) Long borrowUserId) {
        return Result.success(borrowService.page(current, size, orderNo, username, status, borrowUserId));
    }

    @GetMapping("/my")
    public Result<PageResult<BorrowOrderVO>> myPage(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Integer status) {
        return Result.success(borrowService.myPage(current, size, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('borrow:list')")
    public Result<BorrowOrderVO> detail(@PathVariable Long id) {
        return Result.success(borrowService.detail(id));
    }

    @PostMapping
    @Log(title = "借用单管理", businessType = 1)
    @PreAuthorize("hasAuthority('borrow:add')")
    public Result<Void> create(@Valid @RequestBody BorrowOrderDTO dto) {
        borrowService.create(dto);
        return Result.success();
    }

    @PutMapping("/audit")
    @Log(title = "借用审核", businessType = 4)
    @PreAuthorize("hasAuthority('borrow:audit')")
    public Result<Void> audit(@Valid @RequestBody BorrowAuditDTO dto) {
        borrowService.audit(dto);
        return Result.success();
    }

    @PutMapping("/{id}/issue")
    @Log(title = "领用发放", businessType = 7)
    @PreAuthorize("hasAuthority('borrow:issue')")
    public Result<Void> issue(@PathVariable Long id) {
        borrowService.issue(id);
        return Result.success();
    }

    @PutMapping("/extend")
    @Log(title = "续借", businessType = 2)
    @PreAuthorize("hasAuthority('borrow:extend')")
    public Result<Void> extend(@Valid @RequestBody BorrowExtendDTO dto) {
        borrowService.extend(dto);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @Log(title = "取消借用", businessType = 2)
    @PreAuthorize("hasAuthority('borrow:cancel')")
    public Result<Void> cancel(@PathVariable Long id) {
        borrowService.cancel(id);
        return Result.success();
    }
}
