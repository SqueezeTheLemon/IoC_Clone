package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Component;
import com.ll.standard.util.Ut;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ApplicationContext {
    private final String basePackage;
    // 빈 이름 -> 빈 클래스 (빈 생성 정보)
    private final Map<String, Class<?>> beanDefinitions = new HashMap<>();
    // 빈 이름 -> 생성된 빈 (싱글톤 저장소)
    private final Map<String, Object> beans = new HashMap<>();
    // 순환 의존 감지용
    private final Set<String> beansInCreation = new HashSet<>();

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
    }

    public void init() {
        Reflections reflections = new Reflections(basePackage);

        // @Component 및 @Component 가 붙은 애너테이션(@Service, @Repository, ...) 이 붙은 타입
        reflections.getTypesAnnotatedWith(Component.class)
                .stream()
                .filter(this::isInstantiable)
                .forEach(clazz -> beanDefinitions.put(beanNameOf(clazz), clazz));

        beanDefinitions.keySet().forEach(this::genBean);
    }

    @SuppressWarnings("unchecked")
    public <T> T genBean(String beanName) {
        Object bean = beans.get(beanName);
        if (bean != null) return (T) bean;

        Class<?> clazz = beanDefinitions.get(beanName);
        if (clazz == null) throw new IllegalArgumentException("등록되지 않은 빈입니다: " + beanName);

        if (!beansInCreation.add(beanName)) {
            throw new IllegalStateException("순환 의존이 발견되었습니다: " + beanName);
        }

        try {
            bean = createBean(clazz);
        } finally {
            beansInCreation.remove(beanName);
        }

        beans.put(beanName, bean);

        return (T) bean;
    }

    private Object createBean(Class<?> clazz) {
        // 파라미터가 가장 많은 생성자로 주입 (@RequiredArgsConstructor 가 만든 생성자)
        Constructor<?> constructor = Arrays.stream(clazz.getDeclaredConstructors())
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow();

        Object[] args = Arrays.stream(constructor.getParameterTypes())
                .map(paramType -> genBean(beanNameOf(paramType)))
                .toArray();

        try {
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("빈 생성에 실패했습니다: " + clazz.getName(), e);
        }
    }

    private boolean isInstantiable(Class<?> clazz) {
        return !clazz.isInterface()
                && !clazz.isAnnotation()
                && !Modifier.isAbstract(clazz.getModifiers());
    }

    private String beanNameOf(Class<?> clazz) {
        return Ut.str.lcfirst(clazz.getSimpleName());
    }

    @Override
    public String toString() {
        return "ApplicationContext{basePackage='%s', beans=%s}".formatted(basePackage, beanDefinitions.keySet());
    }
}
