package com.liu.simulation.spring.annotation;

import org.springframework.beans.BeansException;

public interface BeanPostProcessor {
    default Object postProcessBeforeInitialization(Object bean, String beanName){
        return bean;
    }

    default Object postProcessAfterInitialization(Object bean, String beanName){
        return bean;
    }
}
