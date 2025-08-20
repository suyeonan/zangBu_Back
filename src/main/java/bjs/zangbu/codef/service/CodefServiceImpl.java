package bjs.zangbu.codef.service;

import bjs.zangbu.building.dto.request.BuildingRequest;
import bjs.zangbu.building.dto.request.BuildingRequest.SaleRegistrationRequest;
import bjs.zangbu.codef.converter.CodefConverter;
import bjs.zangbu.codef.dto.request.CodefRequest.AddressRequest;
import bjs.zangbu.codef.encryption.CodefEncryption;
import bjs.zangbu.codef.encryption.RSAEncryption;
import bjs.zangbu.codef.exception.CodefException;
import bjs.zangbu.codef.session.CodefAuthSession;
import bjs.zangbu.complexList.service.ComplexListService;
import bjs.zangbu.complexList.vo.ComplexList;
import bjs.zangbu.deal.dto.request.BuildingRegisterRequest;
import bjs.zangbu.deal.dto.request.EstateRegistrationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.codef.api.EasyCodef;
import io.codef.api.EasyCodefMessageConstant;
import io.codef.api.EasyCodefServiceType;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * CODEF API 연동을 위한 서비스 구현체. ① 단순 1‑Way 상품 (priceInformation 등) ② 2‑Way 인증 상품
 * (realEstateRegistrationIssuance 등) ③ 3‑Way(보안문자) 상품 (processSecureNo 등) 등 다양한
 * CODEF 상품 API 호출 및
 * 응답 처리를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodefServiceImpl implements CodefService {

  // CODEF 암호화 및 인증을 위한 유틸 클래스 (DI 주입)
  private final CodefEncryption codefEncryption;

  // CODEF SDK 객체 (실제 API 연동용)
  private EasyCodef codef;

  // 세션/인증 데이터를 임시 저장하는 Redis 연결 객체
  private final RedisTemplate<String, Object> redisTemplate;

  private final ComplexListService complexListService;

  // RSA 암호화
  private final RSAEncryption rsaEncryption;

  // 서비스가 생성될 때 CODEF 인스턴스를 초기화
  @PostConstruct
  public void init() {
    codef = codefEncryption.getCodefInstance();
  }

  // codef 결제 관련
  @Value("${codef.ePrepayNo}")
  private String ePrepayNo;
  @Value("${codef.ePrePayPass}")
  private String ePrepayPass;

  /**
   * 아파트 단지(건물) 실거래 시세정보를 조회합니다. 요청 DTO를 기반으로 파라미터 맵을 생성한 후, CODEF API에 요청하여 JSON
   * 응답을 반환합니다.
   *
   * @param buildingId 매물 상세 조회 요청 DTO
   * @return CODEF API로부터 받은 응답 JSON 문자열
   * @throws UnsupportedEncodingException 인코딩 지원되지 않을 때 발생하는 예외
   * @throws JsonProcessingException      JSON 처리 중 발생하는 예외
   * @throws InterruptedException         API 호출 지연 시 발생하는 예외
   */
  @Override
  public String getBuildingDetail(Long buildingId)
      throws UnsupportedEncodingException, JsonProcessingException, InterruptedException {
    // 단일 건물 정보 매핑을 위한 map 구성
    HashMap<String, Object> map = new HashMap<>();
    map.put("organization", "0011"); // CODEF 공공부동산기관 코드
    map.put("searchGbn", "1"); // 검색 구분 ("1": 면적별 시세정보)
    map.put("complexNo", complexListService.getComplexNoByBuildingId(buildingId)); // 단지번호(필수)

    System.out.println(map);
    // CODEF 시세조회 상품 URL
    String url = "/v1/kr/public/lt/real-estate-board/market-price-information";

    // CODEF DEMO API 호출
    String response = codef.requestProduct(url, EasyCodefServiceType.DEMO, map);

    // 응답 JSON 그대로 반환
    return response;
  }

  /**
   * 부동산 등기부 등본을 발급합니다. 등본 발급에 필요한 정보를 파라미터 맵으로 생성한 후, CODEF API를 호출하여 결과를 반환합니다.
   *
   * @param request 등기부 등본 발급 요청 DTO
   * @return CODEF API로부터 받은 응답 JSON 문자열
   * @throws UnsupportedEncodingException 인코딩 지원되지 않을 때 발생하는 예외
   * @throws JsonProcessingException      JSON 처리 중 발생하는 예외
   * @throws InterruptedException         API 호출 지연 시 발생하는 예외
   */
  @Override
  public String realEstateRegistrationLeader(EstateRegistrationRequest request)
      throws UnsupportedEncodingException, JsonProcessingException, InterruptedException {
    String password = request.getBirth();
    password = password.substring(2);
    // RSA 암호화
    String encryptedPassword;
    try {
      encryptedPassword = rsaEncryption.encrypt(password);
    } catch (Exception e) {
      throw new RuntimeException("RSA 암호화 실패", e);
    }
    System.out.println("encryptedPassword = " + encryptedPassword); // 지워야됌

    // 건물번호 로직 todo: 항상 주소 마지막에 띄어쓰기 번호 되어있어야 함 ex. 오부자로 14
    String address_tmp = request.getAddress();
    String[] parts = address_tmp.split(" ");
    String bN = parts[parts.length - 1];

    // 등기부 등본 발급 파라미터 생성 (실제 값은 request에서 추출)
    HashMap<String, Object> parameterMap = new HashMap<>();
    parameterMap.put("organization", "0002");
    parameterMap.put("phoneNo", request.getPhone());
    parameterMap.put("password", encryptedPassword);
    parameterMap.put("inquiryType", "3");
    parameterMap.put("realtyType", "1");
    parameterMap.put("addr_sido", request.getSido());
    parameterMap.put("address", request.getAddress());
    parameterMap.put("dong", request.getDong());
    parameterMap.put("ho", request.getHo());
    parameterMap.put("addr_buildingNumber", bN);
    parameterMap.put("jointMortgageJeonseYN", "1");
    parameterMap.put("tradingYN", "1");
    parameterMap.put("ePrepayNo", ePrepayNo);
    parameterMap.put("ePrepayPass", ePrepayPass);
    parameterMap.put("issueType", "0");
    parameterMap.put("originDataYN", "1");
    parameterMap.put("registerSummaryYN", "1");
    parameterMap.put("addr_sigungu", request.getSigungu());
    parameterMap.put("addr_roadName", request.getRoadName());

    String productUrl = "/v1/kr/public/ck/real-estate-register/status";

    // 2. 1차 인증 요청
    String firstResponse = codef.requestProduct(productUrl, EasyCodefServiceType.DEMO, parameterMap);

    return firstResponse;
  }

  /**
   * 부동산 등기부 실명 일치(소유자 인증) 검사를 수행합니다. 부동산 실명 일치 여부를 확인하는 CODEF API를 호출하여 결과를
   * 반환합니다.
   *
   * @param uniqueNo 부동산 번호
   * @param identity 주민등록번호 원문
   * @return CODEF API로부터 받은 응답 JSON 문자열
   * @throws UnsupportedEncodingException 인코딩 지원되지 않을 때 발생하는 예외
   * @throws JsonProcessingException      JSON 처리 중 발생하는 예외
   * @throws InterruptedException         API 호출 지연 시 발생하는 예외
   */
  @Override
  public String RealEstateRegistrationRegister(String uniqueNo, String identity)
      throws UnsupportedEncodingException, JsonProcessingException, InterruptedException {
    HashMap<String, Object> map = new HashMap<>();
    map.put("organization", "0002");
    map.put("uniqueNo", uniqueNo);
    map.put("identity", identity);

    String url = "/v1/kr/public/ck/real-estate-register/identity-matching";

    String response = codef.requestProduct(url, EasyCodefServiceType.DEMO, map);

    return response;
  }

  /**
   * 3차 인증(보안문자 입력)을 처리합니다. 프론트에서 받은 세션키로 Redis에서 세션 정보를 복구하고, 보안문자 값을 포함하여 CODEF
   * API에 최종 요청합니다. 요청
   * 완료 후 Redis 세션 정보는 삭제됩니다.
   *
   * @param sessionKey Redis에 저장된 세션 정보의 키
   * @param secureNo   사용자가 입력한 보안문자
   * @return CODEF API로부터 받은 최종 응답 JSON 문자열
   */
  @Override
  public String processSecureNo(String sessionKey, String secureNo) {
    // Redis에서 2차 인증까지의 세션 정보 조회
    CodefAuthSession session = (CodefAuthSession) redisTemplate.opsForValue().get(sessionKey);

    // 세션이 없으면 예외 발생 (전역 Advice에서 처리)
    if (session == null) {
      throw new CodefException.CodefServiceException(EasyCodefMessageConstant.INVALID_SESSION);
    }

    // 기존 파라미터(1~2차 인증까지 모든 값)에 3차 인증용 추가값 합치기
    HashMap<String, Object> lastRequest = new HashMap<>(session.getParameterMap());

    HashMap<String, Object> twoWayParams = new HashMap<>();
    twoWayParams.put("jobIndex", session.getJobIndex());
    twoWayParams.put("threadIndex", session.getThreadIndex());
    twoWayParams.put("jti", session.getJti());
    twoWayParams.put("twoWayTimestamp", session.getTwoWayTimestamp());

    lastRequest.put("twoWayInfo", twoWayParams);
    lastRequest.put("secureNo", secureNo); // 사용자가 입력한 보안문자 값 추가

    try {
      String result = codef.requestCertification(
          session.getProductUrl(), EasyCodefServiceType.DEMO, lastRequest);
      // 인증 처리 후 Redis 세션 데이터 삭제 (보안/메모리 관리)
      redisTemplate.delete(sessionKey);
      return result;
    } catch (Exception e) {
      // 서버 처리 오류 발생 시 커스텀 예외로 전환 (전역 Advice에서 JSON 응답 처리)
      throw new CodefException.CodefServiceException(
          EasyCodefMessageConstant.SERVER_PROCESSING_ERROR, e.getMessage());
    }
  }

  /**
   * 시/군/동 주소로 건물 목록을 조회합니다.
   *
   * @param request 주소 정보를 담고 있는 {@link AddressRequest} DTO
   * @return CODEF API로부터 받은 건물 목록 JSON 문자열
   * @throws UnsupportedEncodingException 인코딩 지원되지 않을 때 발생하는 예외
   * @throws JsonProcessingException      JSON 처리 중 발생하는 예외
   * @throws InterruptedException         API 호출 지연 시 발생하는 예외
   */

  @Override
  public String justListInquiry(AddressRequest request)
      throws UnsupportedEncodingException, JsonProcessingException, InterruptedException {
    String url = "/v1/kr/public/lt/real-estate-board/estate-list";
    HashMap<String, Object> map = new HashMap<>();
    map.put("organization", "0011");
    map.put("addrSido", request.getAddrSido());
    map.put("addrSigun", request.getAddrSigun());
    map.put("addrDong", request.getAddrDong());

    String response = codef.requestProduct(url, EasyCodefServiceType.DEMO, map);
    return response;
  }

  /**
   * 특정 매물 ID로 단지 시세 정보를 조회합니다.
   *
   * @param buildingId 단지 시세 정보를 조회할 매물의 ID
   * @return CODEF API로부터 받은 시세 정보 JSON 문자열
   * @throws UnsupportedEncodingException 인코딩 지원되지 않을 때 발생하는 예외
   * @throws JsonProcessingException      JSON 처리 중 발생하는 예외
   * @throws InterruptedException         API 호출 지연 시 발생하는 예외
   */
  @Override
  public String priceInformation(Long buildingId)
      throws UnsupportedEncodingException, JsonProcessingException, InterruptedException {
    HashMap<String, Object> map = new HashMap<>();
    map.put("organization", "0011");
    map.put("searchGbn", "1");
    map.put("complexNo", complexListService.getComplexNoByBuildingId(buildingId));
    String url = "/v1/kr/public/lt/real-estate-board/market-price-information";
    String response = codef.requestProduct(url, EasyCodefServiceType.DEMO, map);
    return response;
  }

  @Override
  public String realEstateRegistrationAddressSearch(SaleRegistrationRequest request)
      throws UnsupportedEncodingException, JsonProcessingException, InterruptedException {
    HashMap<String, Object> map = new HashMap<>();
    map.put("organization", "0002");
    map.put("searchGbn", "3");
    map.put("realtyType", "1");
    map.put("addrSido", request.getComplexList().getSido());
    map.put("addrSigungu", request.getComplexList().getSigungu());
    map.put("addrRoadName", request.getComplexList().getRoadName());
    // 1. 전체 주소를 가져옵니다. (예: "서울특별시 강남구 테헤란로 123")
    String fullAddress = request.getComplexList().getAddress();

    // 2. 주소에서 마지막 공백의 위치를 찾습니다.
    int lastSpaceIndex = fullAddress.lastIndexOf(" ");

    String buildingNumber = ""; // 기본값
    // 3. 공백이 발견되었을 경우, 그 위치 다음부터 끝까지 잘라내 건물번호를 추출합니다.
    if (lastSpaceIndex != -1) {
      buildingNumber = fullAddress.substring(lastSpaceIndex + 1); // "+ 1"은 공백 자체를 제외하기 위함
    }

    map.put("addrBuildingNumber", buildingNumber); // 추출한 건물번호를 맵에 추가
    map.put("electronicClosedYN", "0");
    map.put("dong", request.getComplexList().getDong());
    map.put("ho", request.getComplexList().getHo());
    System.out.println(map);
    String url = "/v1/kr/public/ck/real-estate/address";
    String response = codef.requestProduct(url, EasyCodefServiceType.DEMO, map);
    return response;
  }

  @Override
  public String getComplexDetailByBuildingId(Long buildingId)
      throws UnsupportedEncodingException, JsonProcessingException, InterruptedException {

    try {
      // 1. buildingId로 DB에서 단지 정보 조회
      log.info("DB에서 단지 정보 조회 시작 (buildingId: {})", buildingId);
      ComplexList complex = complexListService.getComplexListByBuildingId(buildingId);
      if (complex == null) {
        throw new RuntimeException("해당 buildingId에 대한 단지 정보가 DB에 없습니다.");
      }
      log.info("DB 조회 완료. 단지 번호: {}", complex.getComplexNo());

      // 2. 단지 상세 정보 조회 (estate-list)
      log.info("Codef 단지 상세 정보 API 호출 시작...");
      AddressRequest addressRequest = new AddressRequest();
      addressRequest.setAddrSido(complex.getSido());
      addressRequest.setAddrSigun(complex.getSigungu());
      addressRequest.setAddrDong(complex.getEupmyeondong());
      addressRequest.setBuildingName(complex.getComplexName());
      String estateListJson = justListInquiry(addressRequest);
      log.info("Codef 단지 상세 정보 API 응답 수신");
      List<Map<String, Object>> estateListData = CodefConverter.parseDataToDto(estateListJson, List.class);

      Map<String, Object> estateDetail = null;
      if (estateListData != null && !estateListData.isEmpty()) {
        String targetComplexNo = complex.getComplexNo();
        if (targetComplexNo != null && !targetComplexNo.isEmpty()) {
          estateDetail = estateListData.stream()
              .filter(infoMap -> targetComplexNo.equals(infoMap.get("commComplexNo")))
              .findFirst()
              .orElse(null);
        }
      }

      // 3. 시세 정보 조회 (market-price-information)
      log.info("Codef 시세 정보 API 호출 시작...");
      String marketPriceJson = priceInformation(buildingId);
      log.info("Codef 시세 정보 API 응답 수신");
      Map<String, Object> marketPriceData = CodefConverter.parseDataToDto(marketPriceJson, Map.class);

      // 4. 두 정보 병합
      log.info("API 응답 데이터 병합 중...");
      Map<String, Object> mergedData = new HashMap<>();
      if (estateDetail != null) {
        mergedData.put("estateDetail", estateDetail);
      }
      if (marketPriceData != null) {
        mergedData.put("marketPrice", marketPriceData);
      }

      // 5. 최종 응답 생성
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> result = new HashMap<>();
      if (mergedData.isEmpty()) {
        result.put("result", Map.of("code", "CF-00004", "message", "조회된 정보가 없습니다."));
      } else {
        result.put("result", Map.of("code", "CF-00000", "message", "성공"));
      }
      result.put("data", mergedData);

      log.info("최종 응답 생성 완료.");
      return mapper.writeValueAsString(result);

    } catch (Exception e) {
      log.error("getComplexDetailByBuildingId 처리 중 예외 발생", e);
      throw e; // 예외를 다시 던져서 전역 핸들러가 처리하도록 함
    }
  }
}