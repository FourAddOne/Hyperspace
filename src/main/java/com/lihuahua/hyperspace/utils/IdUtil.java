package com.lihuahua.hyperspace.utils;

import cn.hutool.core.lang.Snowflake;

public class IdUtil {


/**
 * 获取基于雪花算法生成的唯一ID
 * @return 返回生成的雪花算法ID，以字符串形式返回
 */
    public static String getId(){

        Snowflake snowflake=cn.hutool.core.util.IdUtil.getSnowflake(0,0);

        return snowflake.nextIdStr();
    }
}
