package com.company.sportseq.common.constant;

import cn.hutool.crypto.digest.DigestUtil;

import java.time.Duration;

/**
 * Redis 缓存键与过期时间常量（统一管理，业务代码禁止直接拼接 Redis Key 字符串）。
 */
public final class CacheConstants {

    /** 登录会话键前缀：sportseq:login:token:{jti} -> LoginUser JSON */
    public static final String LOGIN_TOKEN_KEY = "sportseq:login:token:";

    /** 用户会话索引（Set 存 jti）：sportseq:login:user:{userId}，用于停用/踢出 */
    public static final String LOGIN_USER_KEY = "sportseq:login:user:";

    /** 图形验证码键前缀：sportseq:captcha:{uuid} -> 验证码文本 */
    public static final String CAPTCHA_CODE_KEY = "sportseq:captcha:";

    /** 验证码有效期（分钟） */
    public static final long CAPTCHA_EXPIRATION_MINUTES = 5;

    /** 器材编码流水键前缀：sportseq:equipment:code:{分类码}:{年} */
    public static final String EQUIPMENT_CODE_KEY = "sportseq:equipment:code:";

    // ==================== 业务数据缓存（Cache Aside） ====================

    /** 器材分类全量列表：sportseq:category:list:all -> List<EquipmentCategory>（分类树回源数据） */
    public static final String CATEGORY_LIST_KEY = "sportseq:category:list:all";

    /** 器材分类单条键前缀：sportseq:category:{id} -> EquipmentCategory */
    public static final String CATEGORY_KEY_PREFIX = "sportseq:category:";

    /** 器材详情键前缀：sportseq:equipment:detail:{id} -> EquipmentVO */
    public static final String EQUIPMENT_DETAIL_PREFIX = "sportseq:equipment:detail:";

    /** 器材分页键前缀：sportseq:equipment:list:{paramsMd5} -> PageResult<EquipmentVO> */
    public static final String EQUIPMENT_LIST_PREFIX = "sportseq:equipment:list:";

    /** 库存分页键前缀：sportseq:stock:page:{paramsMd5} -> PageResult<StockVO> */
    public static final String STOCK_PAGE_PREFIX = "sportseq:stock:page:";

    /** 热门器材排行键前缀：sportseq:statistics:usage:{rangeMd5} -> List<EquipmentUsageVO> */
    public static final String USAGE_TOP_PREFIX = "sportseq:statistics:usage:";

    /** 器材分页批量失效模式 */
    public static final String EQUIPMENT_LIST_PATTERN = EQUIPMENT_LIST_PREFIX + "*";

    /** 库存分页批量失效模式 */
    public static final String STOCK_PAGE_PATTERN = STOCK_PAGE_PREFIX + "*";

    /** 热门器材排行批量失效模式 */
    public static final String USAGE_TOP_PATTERN = USAGE_TOP_PREFIX + "*";

    /** 仪表盘汇总：sportseq:dashboard:summary -> DashboardSummaryVO */
    public static final String DASHBOARD_SUMMARY_KEY = "sportseq:dashboard:summary";

    /** Spring Cache 命名空间：系统配置（键格式 sportseq:config::{configKey}） */
    public static final String CONFIG_CACHE = "sportseq:config";

    /** Spring Cache 命名空间：字典数据（键格式 sportseq:dict::{key}） */
    public static final String DICT_CACHE = "sportseq:dict";

    /** 分类数据缓存时长：1 小时 */
    public static final Duration CATEGORY_TTL = Duration.ofHours(1);

    /** 器材详情缓存时长：30 分钟 */
    public static final Duration EQUIPMENT_DETAIL_TTL = Duration.ofMinutes(30);

    /** 器材分页缓存时长：30 分钟 */
    public static final Duration EQUIPMENT_LIST_TTL = Duration.ofMinutes(30);

    /** 库存分页缓存时长：60 秒（实时数据，短 TTL + 主动失效双保险） */
    public static final Duration STOCK_PAGE_TTL = Duration.ofSeconds(60);

    /** 热门器材排行缓存时长：5 分钟 */
    public static final Duration USAGE_TOP_TTL = Duration.ofMinutes(5);

    /** 仪表盘汇总缓存时长：30 秒 */
    public static final Duration DASHBOARD_SUMMARY_TTL = Duration.ofSeconds(30);

    public static String categoryKey(Long id) {
        return CATEGORY_KEY_PREFIX + id;
    }

    public static String equipmentDetailKey(Long id) {
        return EQUIPMENT_DETAIL_PREFIX + id;
    }

    public static String equipmentListKey(String paramsHash) {
        return EQUIPMENT_LIST_PREFIX + paramsHash;
    }

    public static String stockPageKey(String paramsHash) {
        return STOCK_PAGE_PREFIX + paramsHash;
    }

    public static String usageTopKey(String rangeHash) {
        return USAGE_TOP_PREFIX + rangeHash;
    }

    /** 参数串 MD5：用于带参数字段的分页缓存键，null 按空串处理 */
    public static String hash(Object... values) {
        StringBuilder builder = new StringBuilder();
        for (Object value : values) {
            builder.append(value == null ? "" : value.toString()).append('|');
        }
        return DigestUtil.md5Hex(builder.toString());
    }

    private CacheConstants() {
    }
}
