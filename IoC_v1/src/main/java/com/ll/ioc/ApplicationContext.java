package com.ll.ioc;

import com.ll.domain.testPost.testPost.repository.TestPostRepository;
import com.ll.domain.testPost.testPost.service.TestFacadePostService;
import com.ll.domain.testPost.testPost.service.TestPostService;

import java.util.HashMap;
import java.util.Map;

public class ApplicationContext {
    private Map<String, Object> display = new HashMap<>();


    public ApplicationContext() {
        TestPostRepository repo = new TestPostRepository();
        TestPostService service = new TestPostService(repo);
        TestFacadePostService facade = new TestFacadePostService(service, repo);

        display.put("testPostRepository", repo);
        display.put("testPostService", service);
        display.put("testFacadePostService", facade);
    }

    public <T> T genBean(String beanName) {

       return (T) display.get(beanName);
    }
}
