package com.dolcecandela.storage;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Sert les images uploadées en lecture seule sous /uploads/** */
@Configuration
public class StorageWebConfig implements WebMvcConfigurer {

    private final FileStorageService storageService;

    public StorageWebConfig(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(FileStorageService.PUBLIC_PREFIX + "**")
                .addResourceLocations(storageService.getRoot().toUri().toString());
    }
}
