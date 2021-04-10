package com.liu.simulation.damo.service;

import com.liu.simulation.spring.annotation.BeanPostProcessor;
import com.liu.simulation.spring.annotation.Component;

@Component
public class LiuBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("初始化之前！");
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("初始化之后！");
        return bean;
    }
}
