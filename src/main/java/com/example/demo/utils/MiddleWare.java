package com.example.demo.utils;

/**
 * 它接受一个以“bearer”开头的字符串并返回“bearer”之后的部分
 */
public class MiddleWare {
    
    /**
     * 如果授权头以“bearer”开头，则返回token
     *
     * @param authorization 授权标头的值。
     * @return 来自授权标头的令牌。
     */
    public static String getTokenFrom(String authorization) {
        if (authorization != null && authorization.toLowerCase().startsWith(
                "bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
