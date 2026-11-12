package com.ai.demo.util;

import lombok.experimental.UtilityClass;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@UtilityClass
public class PromptUtil {

    private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();
    private static final String PATH = "classpath:/prompts/";



    public static Resource getPrompt(String name) {
        String promptPath = PATH + name + ".st";


        Resource resource = RESOURCE_LOADER.getResource(promptPath);

        if (!resource.exists()){
            throw new RuntimeException("Resource not available");
        }
        return resource;
    }
}
