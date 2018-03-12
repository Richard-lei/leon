package com.leon.common;

public class Const {
    public static final String CURRENT_USER = "currentUser";

    //用于实时校验用户输入的用户名或邮箱是否有效
    public static final String EMAIL = "email";
    public static final String USERNAME = "username";

    public interface Role{
        int ROLE_CUSTOMER = 0;//普通用户
        int ROLE_ADMIN = 1;//管理员
    }

    public interface Cart{
        int CHECKED = 1;//选中状态
        int UN_CHECKED = 0;

        //购物车库存数量限制
        String BUY_NUM_SUCCESS = "BUY_NUM_SUCCESS";
        String BUY_NUM_FAIL = "BUY_NUM_FAIL";
    }

}
