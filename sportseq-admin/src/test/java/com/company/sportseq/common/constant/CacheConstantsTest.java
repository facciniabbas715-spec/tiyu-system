package com.company.sportseq.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 缓存键参数哈希的防碰撞与稳定性单元测试。
 */
class CacheConstantsTest {

    @Test
    void hash_shouldNotCollideWhenValuesContainDelimiter() {
        String shiftedBoundary = CacheConstants.hash("a|b", "c");
        String otherSplit = CacheConstants.hash("a", "b|c");

        assertNotEquals(shiftedBoundary, otherSplit,
                "包含 '|' 的参数组合不应与另一种切分方式产生相同哈希");
    }

    @Test
    void hash_shouldBeDeterministic() {
        assertEquals(CacheConstants.hash(1L, "器材A", null),
                CacheConstants.hash(1L, "器材A", null));
    }

    @Test
    void hash_shouldTreatNullAndEmptyAsSameValue() {
        assertEquals(CacheConstants.hash((Object) null), CacheConstants.hash(""));
    }

    @Test
    void hash_shouldNotThrowOnNullVarargs() {
        assertDoesNotThrow(() -> CacheConstants.hash((Object[]) null),
                "整个参数数组为 null 时应按空输入处理，而不是抛异常");
    }
}
