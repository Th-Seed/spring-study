package com.liu.simulation.spring;

import com.liu.simulation.spring.annotation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiuApplicationContext {
    private Class configClass;

    private Map<String,BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();   //beanName：beanDefinition
    private Map<String,Object> singletonObjects = new ConcurrentHashMap<>();   //beanName：Bean （单例池）
    private Map<String,BeanPostProcessor> beanPostProcessors = new ConcurrentHashMap<>(); //BeanPostProcessor集合

    public LiuApplicationContext(Class configClass){
        this.configClass = configClass;

        //扫描，得到class
        List<Class> classList = scan(configClass);

        //解析、过滤类 ---> BeanDefinition--->BeanDefinitionMap
        for(Class clazz : classList){
            //是否包含component注解 （是否为bean）
            if(clazz.isAnnotationPresent(Component.class)){
                //解析Component注解
                BeanDefinition beanDefinition = new BeanDefinition(clazz);
                Component componentAnnotation =(Component) clazz.getAnnotation(Component.class);
                String beanName = componentAnnotation.value();
                //是否设置了beanName， 如果没设置，默认为类名首字母小写
                if(!StringUtils.hasLength(beanName)) {
                    getDefaultBeanName(clazz.getSimpleName());
                    beanName = clazz.getSimpleName();
                    beanName = beanName.substring(0,1).toLowerCase()+beanName.substring(1);
                }

                //是否有scope注解，没有默认为sington
                if(clazz.isAnnotationPresent(Scope.class)){
                    Scope scopeAnnotation = (Scope) clazz.getAnnotation(Scope.class);
                    beanDefinition.setScope(scopeAnnotation.value());
                }else {
                    beanDefinition.setScope("singleton");
                }

                //是否有isLazy注解：默认为not Lazy
                if(clazz.isAnnotationPresent(Lazy.class)){
                    beanDefinition.setLazy(true);
                }

                //将当前beanDefinition放入map中
                beanDefinitionMap.put(beanName,beanDefinition);
            }
        }

        //基于class取创建实例bean
        instanceSingletonBean();

        //初始化bean
        InitSingletonBean();
    }

    private String getDefaultBeanName(String beanName) {
        return beanName.substring(0,1).toLowerCase()+beanName.substring(1);
    }

    private List<Class> scan(Class configClass) {
        List<Class> classList = new ArrayList<>();
        if(configClass.isAnnotationPresent(ComponentScan.class)){
            ComponentScan componentScanAnnotation =
                    (ComponentScan) configClass.getAnnotation(ComponentScan.class);
            String path = componentScanAnnotation.value();  //包路径 com.liu.simulation.spring
            path = path.replace(".","/"); // com/liu/simulation/spring

            ClassLoader classLoader = configClass.getClassLoader();//根据类加载器(注意：每个类加载器对应一定的资源目录）
            //获取该类加载器对应的资源目录（这里是classpath，即····/target/classes）
            URL url = classLoader.getResource(path);//在资源目录下搜索path目录:file:/D:/java/IDEA/IDEA%e9%a1%b9%e7%9b%ae/TestDamo01/Spring/target/classes/com/liu/simulation/spring
            File file = new File(url.getFile());
            //System.out.println(file);
            List<File> files = getFiles(file);
            //System.out.println(files);
            for(File f : files){
                String absolutePath = f.getAbsolutePath();
                absolutePath =
                        absolutePath.substring(absolutePath.indexOf("com"),absolutePath.indexOf(".class"));
                absolutePath = absolutePath.replace("\\", ".");
                //System.out.println(absolutePath);

                try {
                    Class<?> clazz = classLoader.loadClass(absolutePath);
                    classList.add(clazz);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return classList;
    }

    private void instanceSingletonBean() {
        for(Map.Entry<String,BeanDefinition> entry : beanDefinitionMap.entrySet()){

            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            if("singleton".equals(beanDefinition.getScope()) && singletonObjects.get(beanName) == null){   //是单例且非懒加载
                try {
                    Class beanClass = beanDefinition.getBeanClass();
                    Object bean = beanClass.getDeclaredConstructor().newInstance(); //实例化，但并未初始化
                    singletonObjects.put(beanName,bean);    //加入第一缓存
                    //BeanPostProcessor加入集合中
                    if(BeanPostProcessor.class.isAssignableFrom(beanClass))
                        beanPostProcessors.put(beanName,(BeanPostProcessor) bean);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void InitSingletonBean(){
        //BeanPostProcessor优先初始化
        for(Map.Entry<String,BeanPostProcessor> entry : beanPostProcessors.entrySet()){
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            createBean(beanName, bean.getClass());
        }

        for(Map.Entry<String,Object> entry : singletonObjects.entrySet()){
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            if(!(bean instanceof BeanPostProcessor))
            createBean(beanName, bean.getClass());
        }
    }

    private Object createBean(String beanName,Class beanClass) {
        Object bean = singletonObjects.get(beanName);
        if(bean == null) {
            try {
                bean = beanClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        try {
            //1、属性填充
            Field[] declaredFields = beanClass.getDeclaredFields();
            for(Field field : declaredFields){
                //如果有autowired  ,模拟autowired根据类型查找
                if(field.isAnnotationPresent(Autowired.class)){
                    Class fieldType = field.getType();
                    Object obj = getBeanFromSingletonObjects(fieldType);
                    field.setAccessible(true);
                    if(obj != null) field.set(bean,obj);
                    else field.set(bean,createBean(getDefaultBeanName(bean.getClass().getSimpleName()),fieldType));
                }
            }

            //2、aware
            if(bean instanceof BeanNameAware){
                ((BeanNameAware)bean).setBeanName(beanName);
            }
            //3、初始化之前：
            if(!(bean instanceof BeanPostProcessor)) {
                for (Map.Entry<String,BeanPostProcessor> entry : beanPostProcessors.entrySet()) {
                    bean = entry.getValue().postProcessBeforeInitialization(bean, beanName);
                }
            }

            //4、初始化
            if(bean instanceof InitializingBean){
                ((InitializingBean)bean).afterPropertiesSet();
            }

            //5、初始化之后：
            if(!(bean instanceof BeanPostProcessor)) {
                for (Map.Entry<String,BeanPostProcessor> entry : beanPostProcessors.entrySet()) {
                    bean = entry.getValue().postProcessAfterInitialization(bean, beanName);
                }
            }

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bean;
    }

    private Object getBeanFromSingletonObjects(Class clazz){
        for(Map.Entry<String, Object> entry : singletonObjects.entrySet()){
            Object obj = entry.getValue();
            if(obj.getClass() == clazz){
                return obj;
            }
        }
        return null;
    }

    public Object getBean(String beanName){
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if("singleton".equals(beanDefinition.getScope()))
            return singletonObjects.get(beanName);
        else if("prototype".equals(beanDefinition.getScope()))
            return createBean(beanName,beanDefinition.getBeanClass());
        return null;
    }

    private List<File> getFiles(File file){
        ArrayList<File> list = new ArrayList<>();
        if(file.isFile()) list.add(file);
        else {
            File[] files = file.listFiles();
            for (File f : files){
                if(f.isDirectory()) {
                    list.addAll(getFiles(f));
                }else {
                    list.add(f);
                }
            }
        }
        return list;
    }
}
