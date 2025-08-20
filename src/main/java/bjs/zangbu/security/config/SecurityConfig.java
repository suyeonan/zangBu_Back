package bjs.zangbu.security.config;

import bjs.zangbu.security.filter.AuthenticationErrorFilter;
import bjs.zangbu.security.filter.JwtAuthenticationFilter;
import bjs.zangbu.security.filter.JwtUsernamePasswordAuthenticationFilter;
import bjs.zangbu.security.handler.LoginFailureHandler;
import bjs.zangbu.security.handler.LoginSuccessHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@Log4j2
@MapperScan(basePackages = { "bjs.zangbu.security.account.mapper" })
@ComponentScan(basePackages = { "bjs.zangbu.security" })
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final AuthenticationErrorFilter authenticationErrorFilter;
  private final LoginSuccessHandler loginSuccessHandler;
  private final LoginFailureHandler loginFailureHandler;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * AuthenticationManager 빈으로 등록 (Spring 6 표준 방식)
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
      throws Exception {
    return authConfig.getAuthenticationManager();
  }

  /**
   * SecurityFilterChain 구성
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
      AuthenticationManager authenticationManager) throws Exception {
    // 로그인 필터 생성
    JwtUsernamePasswordAuthenticationFilter jwtUsernamePasswordAuthenticationFilter = new JwtUsernamePasswordAuthenticationFilter(
        authenticationManager, loginSuccessHandler,
        loginFailureHandler);

    http
        .addFilterAt(jwtUsernamePasswordAuthenticationFilter,
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, JwtUsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(authenticationErrorFilter, JwtAuthenticationFilter.class)

        // CORS 설정
        .cors(corsCustomizer -> corsCustomizer.configurationSource(request -> {
          CorsConfiguration config = new CorsConfiguration();
          config.setAllowedOrigins(List.of(
              "https://zangbu.site",
              "https://www.zangbu.site",
              "https://api.zangbu.site",
              "http://localhost:3000",
              "http://localhost:5173",
              "http://localhost:8080",
              "http://localhost:61613",
              "http://localhost:6379"));
          config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
          config.setAllowedHeaders(List.of("*"));
          config.setAllowCredentials(true);
          config.setMaxAge(3600L);
          return config;
        }))

        .csrf(csrf -> csrf.disable())
        .httpBasic(httpBasic -> httpBasic.disable())
        .formLogin(formLogin -> formLogin.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // 권한 설정
        .authorizeHttpRequests(auth -> auth
            // 기본 및 테스트 URL
            .requestMatchers(new AntPathRequestMatcher("/")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/index.html")).permitAll()

            // Swagger 관련 경로 허용 (Springfox 기준)
            .requestMatchers(new AntPathRequestMatcher("/swagger-ui.html/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/v2/api-docs")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/swagger-resources/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/webjars/**")).permitAll()

            // 정적 리소스 허용 (필요시)
            .requestMatchers(new AntPathRequestMatcher("/favicon.ico")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/static/**")).permitAll()

            // map 엔드포인트 허용
            .requestMatchers(new AntPathRequestMatcher("/map/**")).permitAll()

            // auth 엔트 포인트
            // .requestMatchers(new AntPathRequestMatcher("/auth/**")).permitAll()
            // 보안 API 경로 설정
            .requestMatchers(new AntPathRequestMatcher("/security/all")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/security/admin")).hasRole("ADMIN")
            .requestMatchers(new AntPathRequestMatcher("/security/member"))
            .hasAnyRole("ADMIN", "MEMBER")
            .requestMatchers(new AntPathRequestMatcher("/auth/**")).permitAll()

            // codef 엔드포인트 허용
            .requestMatchers(new AntPathRequestMatcher("/codef/**")).permitAll()

            // 보안 API 경로 설정
            .requestMatchers(new AntPathRequestMatcher("/security/all")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/security/admin")).hasRole("ADMIN")
            .requestMatchers(new AntPathRequestMatcher("/security/member"))
            .hasAnyRole("ADMIN", "MEMBER")

            // 테스트용: /chat/** 전체 허용
            .requestMatchers(new AntPathRequestMatcher("/chat/**")).permitAll()

            // 테스트용: /deal/**허용
            .requestMatchers(new AntPathRequestMatcher("/deal/**")).permitAll()

            // 멤버십 엔드포인트는 인증된 사용자만 접근 가능
            .requestMatchers(new AntPathRequestMatcher("/membership/**")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/address-changes/**")).authenticated()

            .requestMatchers(new AntPathRequestMatcher("/auth/signup")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/auth/login")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/auth/reissue")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/auth/email")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/auth/check/email")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/auth/check/nickname")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/auth/verify")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/codef/secure")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/codef/captcha")).permitAll()

            .requestMatchers(new AntPathRequestMatcher("/auth/logout")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/building/{buildingId}")).permitAll()

            // 그 외 요청은 인증 필요
            .anyRequest().authenticated());

    return http.build();
  }
}