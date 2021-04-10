package com.liu.simulation.damo.service;

import com.liu.simulation.spring.annotation.Component;
import com.liu.simulation.spring.annotation.Lazy;

@Component
@Lazy
public class User {
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    private String userName;
}
