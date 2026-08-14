package com.company.sportseq.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.sportseq.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文档元数据（继承 BaseEntity：createTime/updateTime 自动填充、del_flag 逻辑删除）。
 * 文档正文分块存 {@link KnowledgeChunk}，向量存 Redis Stack，本表不存内容。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_document")
public class KnowledgeDocument extends BaseEntity {

    /** 状态：解析/嵌入处理中 */
    public static final int STATUS_PROCESSING = 0;

    /** 状态：已就绪，可参与检索 */
    public static final int STATUS_READY = 1;

    /** 状态：处理失败 */
    public static final int STATUS_FAILED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String filePath;

    private Integer status;

    private Integer chunkCount;

    private String errorMsg;

    private String remark;
}
