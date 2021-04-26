package ninja.skyrocketing.fuyao.musicroom.configuration;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;


/**
 * @author skyrocketing Hong
 * @create 2020-06-22 7:00
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().authorizeRequests().
                antMatchers(HttpMethod.POST, "/house/add").permitAll().
                antMatchers(HttpMethod.POST, "/house/enter").permitAll().
                antMatchers(HttpMethod.POST, "/house/get").permitAll().
                antMatchers(HttpMethod.POST, "/house/search").permitAll().
                antMatchers(HttpMethod.POST, "/server/**").permitAll().
                antMatchers(HttpMethod.GET, "/server/**").permitAll().
                antMatchers("/img/**").permitAll().
                antMatchers("/css/**").permitAll().
                antMatchers("/js/**").permitAll().
                antMatchers("/index.html").permitAll().
                antMatchers("/favicon.ico").permitAll().
                antMatchers(HttpMethod.GET, "/").permitAll().
                anyRequest().authenticated().and().httpBasic().and().cors();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Arrays.asList(
                "Accept", "Origin", "Content-Type", "Depth", "User-Agent", "If-Modified-Since,",
                "Cache-Control", "Authorization", "X-Req", "X-File-Size", "X-Requested-With", "X-File-Name", "Accesstoken"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}