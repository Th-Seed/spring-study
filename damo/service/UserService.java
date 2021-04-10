package com.liu.simulation.damo.service;

import com.liu.simulation.spring.annotation.*;

@Component("userService")
@Scope("prototype")
public class UserService implements BeanNameAware,InitializingBean {
    @Autowired
    private User user;

    private String beanName;

    private String userName;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.userName = user.getUserName();
    }
}
