package com.company.sportseq.vo;

import java.util.List;

public record ImportResultVO(int successCount, int failCount, List<String> errors) {
}
