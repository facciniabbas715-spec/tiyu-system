package com.company.sportseq.knowledge.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识文档详情（含分块预览）。
 */
public record KnowledgeDocumentDetailVO(
        Long id,
        String title,
        String fileName,
        String fileType,
        Long fileSize,
        Integer status,
        Integer chunkCount,
        String errorMsg,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        List<KnowledgeChunkVO> chunks) {
}
