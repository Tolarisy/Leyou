package com.leyou.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * @anthor Tolaris
 * @date 2020/4/23 - 23:52
 */
@Configuration
public class LeyouCorsConfiguration {

    @Bean
    public CorsFilter corsFilter() {
        //初始化cors配置对象
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //允许跨域的域名
        corsConfiguration.addAllowedOrigin("http://manage.leyou.com:8080");
        corsConfiguration.addAllowedOrigin("http://www.leyou.com:8080");
        //允许携带cookie
        corsConfiguration.setAllowCredentials(true);
        //代表所有的请求方法
        corsConfiguration.addAllowedMethod("*");
        //允许携带任何的头信息
        corsConfiguration.addAllowedHeader("*");

        //初始化cors配置源对象
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsFilter(source);
    }
}
