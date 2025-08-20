package bjs.zangbu.global.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

@EnableWebMvc
@ComponentScan(basePackages = {
    "bjs.zangbu.exception",
    "bjs.zangbu.controller",
    "org.springdoc",
    "bjs.zangbu.global.controller",
    "bjs.zangbu.addressChange.controller",
    "bjs.zangbu.building.controller",
    "bjs.zangbu.chat.controller",
        "bjs.zangbu.chat.config",
    "bjs.zangbu.codef.controller",
    "bjs.zangbu.deal.controller",
    "bjs.zangbu.documentReport.controller",
    "bjs.zangbu.fcm.controller",
    "bjs.zangbu.map.controller",
    "bjs.zangbu.member.controller",
    "bjs.zangbu.notification.controller",
    "bjs.zangbu.payment.controller",
    "bjs.zangbu.publicdata.controller",
    "bjs.zangbu.review.controller",
    "bjs.zangbu.scheduler.controller",
    "bjs.zangbu.security.account.controller"
})
public class ServletConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/resources/**") // url이 /resources/로 시작하는 모든 경로
        .addResourceLocations("/resources/"); // webapp/resources/경로로 매핑
    /* ▼ 새로 추가: 계약서 PDF 두 개를 서빙 */
    registry.addResourceHandler("/contracts/**")
        .addResourceLocations("classpath:/contracts/"); // src/main/resources/contracts/

    // WebJars (필요 시)
    registry.addResourceHandler("/webjars/**")
        .addResourceLocations("classpath:/META-INF/resources/webjars/");

    registry.addResourceHandler("/swagger-ui/**")
        .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/");

    registry.addResourceHandler("/swagger-ui.html")
        .addResourceLocations("classpath:/META-INF/resources/");

    registry.addResourceHandler("/swagger-ui/index.html")
        .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
  }

  //	Servlet 3.0 파일 업로드 사용시
  @Bean
  public MultipartResolver multipartResolver() {
    StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
    return resolver;
  }

  @Bean
  public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
    return new HandlerMappingIntrospector();
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    for (HttpMessageConverter<?> c : converters) {
      if (c instanceof StringHttpMessageConverter) {
        ((StringHttpMessageConverter) c).setDefaultCharset(StandardCharsets.UTF_8);
      }
    }
  }

}
