package com.kampotourism.config;

import com.kampotourism.media.MediaStorage;
import java.time.Duration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties cors;
    private final MediaStorage media;

    public WebConfig(CorsProperties cors, MediaStorage media) {
        this.cors = cors;
        this.media = media;
    }

    /**
     * Public content is read-only cross-origin. The admin and auth APIs additionally allow writes
     * and the Authorization header. The more specific mapping must be registered first.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = cors.allowedOrigins().toArray(String[]::new);
        for (String path : new String[] {"/api/v1/admin/**", "/api/v1/auth/**"}) {
            registry.addMapping(path)
                    .allowedOrigins(origins)
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("Authorization", "Content-Type")
                    .maxAge(3600);
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "HEAD", "OPTIONS")
                .maxAge(3600);
    }

    /**
     * With local storage, uploaded images are served from the media directory (file names are
     * random UUIDs). With Cloudinary they are served by its CDN, so nothing is exposed here.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!media.servesLocalFiles()) {
            return;
        }
        registry.addResourceHandler("/media/**")
                .addResourceLocations(media.root().toUri().toString())
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }

    /** Adds ETag + 304 handling so browsers and CDNs can revalidate cheaply. */
    @Bean
    FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/*");
        registration.setName("etagFilter");
        return registration;
    }
}
