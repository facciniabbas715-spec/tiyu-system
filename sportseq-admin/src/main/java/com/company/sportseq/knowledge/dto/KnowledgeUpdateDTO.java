package com.company.sportseq.knowledge.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识文档更新入参（PUT multipart：file 可空；file 为空时仅更新标题/备注）。
 */
@Data
public class KnowledgeUpdateDTO {

    @Size(max = 200, message = "标题不能超过200字")
    private String title;

    @Size(max = 500, message = "备注不能超过500字")
    private String remark;
}
