package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Component;
import com.ll.standard.util.Ut;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationContext {
    final String basePackage;
    private Map<String, Object> beanMap = new HashMap<>();

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
    }

    public void init() {
        Set<Class<?>> componentClasses = scanComponentClasses();

        // componentClasses에 들어있는 clazz들을 bean으로 만들고 beanMap에 그 이름과 함께 넣음
        for (Class<?> clazz : componentClasses){

            getOrCreateBean(clazz);
        }
    }

    // basePackage 안에서 @Component 게열이 붙은 진짜 클래스만 골라오기
    private Set<Class<?>> scanComponentClasses() {
        Reflections reflections = new Reflections(basePackage); // 1. "com.ll"아래를 스캔

        return reflections.getTypesAnnotatedWith(Component.class) // 2. 그 중에 @Component가 붙은 것만 줘
                .stream()
                .filter(clazz -> !clazz.isAnnotation())// @Service와 같은 annotation(해당 클래스가 어떤 종류인지 알려주는 꼬리표)는 빼오기
                .collect(Collectors.toSet());
    }

    // 이름 규칙을 지정하는 메소드 - 클래스의 simple name을 받아와 그를 lowercase로 수정
    private String toBeanName(Class<?> clazz){
        return Ut.str.lcfirst(clazz.getSimpleName());
    }

    private Object getOrCreateBean(Class<?> clazz){
        String beanName = toBeanName(clazz);

        // 이미 만든 적 있으면 beanMap에서 조회 후 생성하지 않고 그대로 반환
        if (beanMap.containsKey(beanName)){
            return beanMap.get(beanName);
        }

        Object bean = createBean(clazz);
        beanMap.put(beanName, bean);
        return bean;
    }
    // reflection으로 bean 생성
    private Object createBean(Class<?> clazz){
        try{
            // 클래스가 가진 생성자 (일단은 하나뿐이라고 가정)를 가져옴
            Constructor<?> constructor = clazz.getDeclaredConstructors()[0];

            // 생성자가 원하는 타입마다 빈을 구해온다.
            Object[] args = Arrays.stream(constructor.getParameterTypes())
                            .map(this::getOrCreateBean)
                            .toArray();

            constructor.setAccessible(true); // private 생성자여도 만들어줌
            // 모든 재료를 넣어서 생성
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("빈 생성 실패: " + clazz.getName(), e);
        }
    }

    // 경고하지 말라는 표시
    @SuppressWarnings("unchecked")
    public <T> T genBean(String beanName) {
        return (T) beanMap.get(beanName);
    }
}
