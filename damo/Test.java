package com.liu.simulation.damo;

import com.liu.simulation.damo.service.User;
import com.liu.simulation.damo.service.UserService;
import com.liu.simulation.spring.AppConfig;
import com.liu.simulation.spring.LiuApplicationContext;

/*
测试类
 */
public class Test {
    public static void main(String[] args) {
        //启动Spring，创建IOC容器
        LiuApplicationContext contrxt = new LiuApplicationContext(AppConfig.class);
        //
        UserService userService = (UserService) contrxt.getBean("userService");
        User user = (User) contrxt.getBean("user");

    }
}
