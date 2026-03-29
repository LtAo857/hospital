package com.example.hospital.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Value("${storage.local.root-path:D:/hospital-storage}")
    private String storageRootPath;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "DELETE", "PUT", "PATCH")
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        List<String> locations = new ArrayList<>();
        locations.add(toResourceLocation(Paths.get(storageRootPath)));
        discoverFallbackDirs().forEach(path -> locations.add(toResourceLocation(path)));
        registry.addResourceHandler("/file/**")
                .addResourceLocations(locations.toArray(new String[0]));
    }

    private Set<Path> discoverFallbackDirs() {
        Path currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Set<Path> dirs = new LinkedHashSet<>();
        dirs.add(currentDir.resolve("Minio").normalize());
        dirs.add(currentDir.resolve("../Minio").normalize());
        dirs.remove(Paths.get(storageRootPath).toAbsolutePath().normalize());
        dirs.removeIf(path -> !Files.isDirectory(path));
        return dirs;
    }

    private String toResourceLocation(Path path) {
        return path.toAbsolutePath().normalize().toUri().toString();
    }
}