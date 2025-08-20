package bjs.zangbu.addressChange.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

/**
 * 행안부 도로명주소 Open API 호출 클라이언트
 *
 * - 요청 방식: POST (application/x-www-form-urlencoded)
 * - 응답 형식: JSON (resultType=json)
 * - 본 예시는 최상위 1건의 "roadAddrPart1" 만 추출해 반환합니다.
 *
 * 참고: "roadAddr" vs "roadAddrPart1/2"
 *   - roadAddr        : 전체 도로명 주소(동·호 포함 가능)
 *   - roadAddrPart1   : 기본 도로명 주소(동·호 제외한 '도로명, 건물번호'까지의 본문)
 *   - roadAddrPart2   : 상세주소(동·호 등 상세)
 */
@Component
@RequiredArgsConstructor
public class JusoClient {
    private final RestTemplate restTemplate;
    // JSON 파싱용 ObjectMapper (스레드세이프)
    private static final ObjectMapper OM = new ObjectMapper();
    // API URL (JSON/폼 전송)
    @Value("${juso.endpoint:https://business.juso.go.kr/addrlink/addrLinkApi.do}")
    private String endpoint;
    @Value("${juso.serviceKey}")
    private String confmKey;

    /**
     * 검색어(keyword)로 후보를 조회하고, 최상위 1건의 roadAddrPart1을 반환합니다.
     *
     * @param keyword 도로명주소 검색 키워드(개행 제거/공백 정리된 값 권장)
     * @return roadAddrPart1 (예: "경기도 고양시 일산서구 강선로 30") 또는 null(없음/오류)
     */
    public String searchBestRoadAddr1(String keyword) {
        if (confmKey == null || confmKey.isBlank()) {
            throw new IllegalStateException("juso.serviceKey 미설정");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_FORM_URLENCODED, StandardCharsets.UTF_8));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("confmKey", confmKey);
        form.add("currentPage", "1");
        form.add("countPerPage", "10");
        form.add("keyword", keyword);
        form.add("resultType", "json");
        form.add("hstryYn", "Y");
        form.add("firstSort", "road");
        form.add("addInfoYn", "Y");

        //요청 전송
        ResponseEntity<String> rsp =
                restTemplate.postForEntity(endpoint, new HttpEntity<>(form, headers), String.class);
        // http 상태/바디 검증
        if (!rsp.getStatusCode().is2xxSuccessful() || rsp.getBody() == null) return null;

        //json 파싱 및 roadAddrPart1 추출
        try {
            JsonNode root = OM.readTree(rsp.getBody());
            // results.common.errorCode / errorMessage 확인 가능 (운영 시 로깅 권장)
            JsonNode juso = root.path("results").path("juso");
            if (juso.isArray() && juso.size() > 0) {
                // 최상위 1건에서 "roadAddrPart1" 추출
                return juso.get(0).path("roadAddrPart1").asText(null);
//                return juso.get(0).path("zipNo").asText(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String searchZipNo(String keyword) {
        if (confmKey == null || confmKey.isBlank()) {
            throw new IllegalStateException("juso.serviceKey 미설정");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_FORM_URLENCODED, StandardCharsets.UTF_8));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("confmKey", confmKey);
        form.add("currentPage", "1");
        form.add("countPerPage", "10");
        form.add("keyword", keyword);
        form.add("resultType", "json");
        form.add("hstryYn", "Y");
        form.add("firstSort", "road");
        form.add("addInfoYn", "Y");

        ResponseEntity<String> rsp =
                restTemplate.postForEntity(endpoint, new HttpEntity<>(form, headers), String.class);
        if (!rsp.getStatusCode().is2xxSuccessful() || rsp.getBody() == null) return null;

        try {
            JsonNode root = OM.readTree(rsp.getBody());
            JsonNode juso = root.path("results").path("juso");
            if (juso.isArray() && juso.size() > 0) {
                return juso.get(0).path("zipNo").asText(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
