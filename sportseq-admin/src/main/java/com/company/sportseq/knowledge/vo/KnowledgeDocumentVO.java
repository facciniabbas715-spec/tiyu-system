package com.company.sportseq.knowledge.vo;

import java.time.LocalDateTime;

/**
 * 知识文档分页行。
 */
public record KnowledgeDocumentVO(
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
        LocalDateTime updateTime) {
}
