package com.company.sportseq.knowledge.service;

import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.knowledge.vo.KnowledgeDocumentDetailVO;
import com.company.sportseq.knowledge.vo.KnowledgeDocumentVO;
import com.company.sportseq.knowledge.vo.SeedResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档管理：上传（解析→切分→Embedding→向量）、查看、更新、删除、重建向量、导入内置知识库。
 */
public interface KnowledgeDocumentService {

    PageResult<KnowledgeDocumentVO> page(long current, long size, String keyword, String fileType, Integer status);

    KnowledgeDocumentDetailVO detail(Long id);

    KnowledgeDocumentVO upload(MultipartFile file, String title, String remark);

    KnowledgeDocumentVO update(Long id, MultipartFile file, String title, String remark);

    void delete(Long id);

    KnowledgeDocumentVO rebuild(Long id);

    SeedResultVO seed();
}
