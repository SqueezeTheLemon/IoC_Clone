package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Bean;
import com.ll.framework.ioc.annotations.Component;
import com.ll.framework.ioc.annotations.Configuration;
import com.ll.standard.util.Ut;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationContext {

    final String basePackage; // IoC 컨테이너가 어디를 뒤질지 지정
    private Map<String, Object> beanMap = new HashMap<>(); // 창고 : 이름표 -> 객체
    private Map<String, Method> methodMap = new HashMap<>();

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
    }

    public void init() {
        // com.ll 이하에서 @Component 배지를 단 클래스만
        Set<Class<?>> componentClasses = scanComponentClasses();

        scanBeanMethods(componentClasses);

        // componentClasses에 들어있는 clazz들을 bean으로 만들고 beanMap에 그 이름과 함께 넣음
        for (Class<?> clazz : componentClasses){
            getOrCreateBean(clazz);
        }

        // 장부에 적힌 레시피들도 전부 빈으로 만들기
        for (Method method : methodMap.values()) {
            getOrCreateBeanByMethod(method);
        }



    }

    // 지정된 basePackage 아래의 클래스들 중 실제로 IoC가 관리해야할 클래스만 골라서 반환
    private Set<Class<?>> scanComponentClasses() {
        Reflections reflections = new Reflections(basePackage);

        return reflections.getTypesAnnotatedWith(Component.class)
                .stream()
                .filter(clazz -> !clazz.isAnnotation()) // 꼬리표 자체는 빼기
                .collect(Collectors.toSet());

    }

    // @Configuration 클래스 안의 @Bean 메소드를 찾아서 장부(methodMap)에 기록
    private void scanBeanMethods (Set<Class<?>> componentClasses) {
        // 1. 이미 뽑아둔 클래스들 중 @Configuration 붙은 것만
        for (Class<?> clazz : componentClasses) {
            if(!clazz.isAnnotationPresent(Configuration.class)) continue;

            // 2. 그 클래스의 메소드 중 @Bean 붙은 것만
            for (Method method : clazz.getDeclaredMethods()) {
                if(!method.isAnnotationPresent(Bean.class)) continue;

                methodMap.put(method.getName(), method);
            }
        }
    }


// 클래스한테서 simple name을 받아와 lowercase로 변환한 string을 반환
private String toBeanName(Class<?> clazz){
    return Ut.str.lcfirst(clazz.getSimpleName());
}

// 싱글톤 유지를 위한 get or create
private Object getOrCreateBean(Class<?> clazz){
    Method beanMethod = findBeanMethodByType(clazz);
    if (beanMethod != null) {
        return getOrCreateBeanByMethod(beanMethod);
    }

    String beanName = toBeanName(clazz);

    // beanMap 안에 clazz가 있으면 그대로 반환
    if (beanMap.containsKey(beanName)){
        return beanMap.get(beanName);
    }

    Object bean = createBean(clazz);
    beanMap.put(beanName, bean);
    return bean;

}

// method로 만드는 bean의 get or create
    private Object getOrCreateBeanByMethod(Method method){
        String beanName = method.getName();
        if (beanMap.containsKey(beanName)){
            return beanMap.get(beanName);
        }
        Object bean = createBeanByMethod(method);
        beanMap.put(beanName, bean);
        return bean;
    }
// clazz라는 설계도를 받아서 생성자가 원하는 재료들을 모두 args에 저장 후 new
private Object createBean(Class<?> clazz){

    try {
        // 클래스의 생성자 목록 중 첫번째
        Constructor<?>constructor = clazz.getDeclaredConstructors()[0];

        // 생성자가 원하는 타입마다 빈을 구해온다.
        Object[] args = Arrays.stream(constructor.getParameterTypes()) // 생성자에게 필요한 타입들
                .map(this::getOrCreateBean)
                .toArray();

        constructor.setAccessible(true);
        return constructor.newInstance(args); // 자료 배열을 하나씩 넣어서 객체 마들기
    }
    catch (ReflectiveOperationException e) {
        throw new RuntimeException("빈 생성 실패: " + clazz.getName(), e);
    }
}

// method를 실행하여 그 반환값을 빈으로 나눈다.
private Object createBeanByMethod(Method method){
    try {
        // 메서드의 주인 객체 (TestJacksonConfig) → 창고에서 꺼내거나 만들기
        Object owner = getOrCreateBean(method.getDeclaringClass());

        // 메서드가 원하는 재료들 구해오기 (createBean이랑 똑같죠?)
        Object[] args = Arrays.stream(method.getParameterTypes())
                .map(this::getOrCreateBean)
                .toArray();

        method.setAccessible(true);
        return method.invoke(owner, args);   // = owner.메서드(args...)
    } catch (ReflectiveOperationException e) {
        throw new RuntimeException("빈 생성 실패: " + method.getName(), e);
    }
}

    // 이 타입을 반환하는 @Bean 레시피가 장부에 있으면 돌려주고, 없으면 null
    private Method findBeanMethodByType(Class<?> type) {
        for (Method method : methodMap.values()) {
            if (method.getReturnType() == type) return method;
        }
        return null;
    }


@SuppressWarnings("unchecked")
public <T> T genBean(String beanName) {
    return (T) beanMap.get(beanName);
}
}
