package com.company.sportseq.knowledge.controller;

import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.knowledge.service.KnowledgeDocumentService;
import com.company.sportseq.knowledge.vo.KnowledgeDocumentDetailVO;
import com.company.sportseq.knowledge.vo.KnowledgeDocumentVO;
import com.company.sportseq.knowledge.vo.SeedResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库管理接口：上传 / 查看 / 更新 / 删除 / 重建向量 / 导入内置知识库。
 */
@RestController
@RequestMapping("/api/knowledge/documents")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeDocumentService knowledgeDocumentService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public Result<PageResult<KnowledgeDocumentVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) Integer status) {
        return Result.success(knowledgeDocumentService.page(current, size, keyword, fileType, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public Result<KnowledgeDocumentDetailVO> detail(@PathVariable Long id) {
        return Result.success(knowledgeDocumentService.detail(id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Log(title = "知识库管理", businessType = 1)
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public Result<KnowledgeDocumentVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String remark) {
        return Result.success(knowledgeDocumentService.upload(file, title, remark));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Log(title = "知识库管理", businessType = 2)
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public Result<KnowledgeDocumentVO> update(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String remark) {
        return Result.success(knowledgeDocumentService.update(id, file, title, remark));
    }

    @DeleteMapping("/{id}")
    @Log(title = "知识库管理", businessType = 3)
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeDocumentService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/rebuild")
    @Log(title = "知识库管理", businessType = 0)
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public Result<KnowledgeDocumentVO> rebuild(@PathVariable Long id) {
        return Result.success(knowledgeDocumentService.rebuild(id));
    }

    @PostMapping("/seed")
    @Log(title = "知识库管理", businessType = 6)
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public Result<SeedResultVO> seed() {
        return Result.success(knowledgeDocumentService.seed());
    }
}
