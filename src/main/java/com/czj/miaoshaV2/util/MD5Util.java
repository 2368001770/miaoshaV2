package com.czj.miaoshaV2.util;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5Util {

    public static String md5(String string){
        return DigestUtils.md5Hex(string);
    }

    private static final String salt = "abc123";

    //把输入框中的数据加密
    public static String inputPassToFormPass(String input){
        String str = "" + salt.charAt(0) + salt.charAt(2) + input +salt.charAt(5) + salt
                .charAt(3);
        return md5(str);
    }

    //把后台获取到的表单数据再一次加密
    public static String formPassToDBPass(String formPass,String salt){
        String str = "" + salt.charAt(0) + salt.charAt(2) + formPass +salt.charAt(5) + salt
                .charAt(3);
        return md5(str);
    }

    //把输入框中的数据两次加密
    public static String inputPassToDBPass(String input,String saltDB){
        String formPass = inputPassToFormPass(input);
        String dbPass = formPassToDBPass(formPass,saltDB);
        return dbPass;
    }

    public static void main(String[] args) {
        String formPass = inputPassToFormPass("18312717821");
        String dbPass = formPassToDBPass(formPass,"123456");
        String dbPassV2 = inputPassToDBPass("18312717821","123456");
        System.out.println(formPass);
        System.out.println(dbPass);
        System.out.println(dbPassV2);
    }

}
