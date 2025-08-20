package bjs.zangbu.addressChange.service;

import bjs.zangbu.addressChange.dto.abstractResponse.ResidentDataResponse;
import bjs.zangbu.addressChange.dto.request.ResRegisterCertRequest;
import bjs.zangbu.addressChange.dto.response.ResRegisterCertResponse;
import bjs.zangbu.addressChange.mapper.AddressChangeMapper;
import bjs.zangbu.addressChange.util.AddrUtil;
import bjs.zangbu.addressChange.util.JusoClient;
import bjs.zangbu.addressChange.vo.AddressChange;
import bjs.zangbu.codef.converter.CodefConverter;
import bjs.zangbu.codef.service.CodefTwoFactorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class AddressChangeServiceImpl implements AddressChangeService {
    private final AddressChangeMapper addressChangeMapper;
    private final CodefTwoFactorService codefTwoFactorService;
    private final JusoClient jusoClient;

    private static final LocalDate CUTOFF = LocalDate.of(2009, 12, 10);                // 정책 기준일
    private static final DateTimeFormatter D8 = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * memberId 기준으로 CODEF 초본을 조회 → 전입 이력을 저장하고 DTO 리스트 반환
     */
    @Override
    @Transactional
    public List<ResRegisterCertResponse> generateAddressChange(String memberId) throws Exception {
        long start = System.currentTimeMillis();
        log.info("[START] generateAddressChange memberId={}", memberId);
        //db 조회
        ResRegisterCertRequest dto = addressChangeMapper.getRegisterCertRequest(memberId);
        if (dto == null) {
            log.warn("[DB] ResRegisterCertRequest not found. memberId={}", memberId);
            return List.of();
        }

        //1,2차 응답 후 응답
        String rawResponse = codefTwoFactorService.residentRegistrationCertificate(dto);

        // 2) DTO로 파싱
        ResidentDataResponse data =
                CodefConverter.parseDataToDto(rawResponse, ResidentDataResponse.class);

        if (data == null || data.addrChanges == null || data.addrChanges.isEmpty()) {
            log.info("No address changes in response. memberId={}", memberId);
            return List.of();
        }
        // 여기까진 동일
        log.info("[PARSE] addrChanges.total={}", data.addrChanges.size());


        // 3) '전입'만 선별 → 전입일 확정 → 메모 행 제외 → 전입일 오름차순
        var selected = data.addrChanges.stream()
                .filter(a -> "전입".equals(a.resChangeReason))
                .map(a -> new Temp(a, resolveMoveIn(a))) // 전입일: resMoveInDate 우선, 없으면 resChangeDate
                .filter(t -> t.moveIn != null && !AddrUtil.isMemoLine(t.src.resUserAddr))
                .sorted(Comparator.comparing(t -> t.moveIn))
                .toList();
        log.info("[FILTER] selected(after join/memo/moveIn)={}", selected.size());

        List<ResRegisterCertResponse> result = new ArrayList<>();

        // 4) 각 전입 건 처리
        for (Temp t : selected) {
            try {
                String original = t.src.resUserAddr == null ? "" : t.src.resUserAddr;

                // 4-1) 검색용 전처리: 개행 제거/공백 정리(동·호는 보존)
                String q1 = AddrUtil.normalizeForSearchKeepDongHo(original);

                // 4-2) 동·호 추출 → res_number 저장용
                String resNumber = AddrUtil.extractDongHo(original).orElse(null);

                // 4-3) 지번→도로명 변환 (정책 기준일 이전만)
                String saveAddr = q1; // 변환 실패 시 전처리된 원문을 저장
                if (t.moveIn.toLocalDate().isBefore(CUTOFF)) {
                    // 1차: 동·호 포함 키워드로 조회 (roadAddrPart1만 사용)
                    String roadPart1 = jusoClient.searchBestRoadAddr1(q1);

                    // 2차: 결과 없으면 동·호 제거 후 재시도
                    if (roadPart1 == null || roadPart1.isBlank()) {
                        String q2 = AddrUtil.removeDongHo(q1);
                        roadPart1 = jusoClient.searchBestRoadAddr1(q2);
                    }

                    if (roadPart1 != null && !roadPart1.isBlank()) {
                        saveAddr = roadPart1; // 동·호는 res_number로 분리 보관
                    }
                }

                // 4-4) VO 생성 (테이블 DDL에 맞춘 필드)
                AddressChange vo = new AddressChange(
                        null,                 // addressChangeId (AUTO_INCREMENT)
                        resNumber,            // res_number (동·호)
                        saveAddr,             // res_user_addr (도로명 또는 전처리 원문) -> 우편번호로 바꿈
                        t.moveIn,             // res_move_in_date (DATETIME)
                        memberId              // member_id (VARCHAR(36))
                );

                // 4-5) 저장
                addressChangeMapper.insert(vo); // PK 세팅됨(useGeneratedKeys)
                // 4-6) VO → DTO 변환 후 결과에 추가
                result.add(new ResRegisterCertResponse(
                        vo.getAddressChangeId(),
                        vo.getResNumber(),
                        vo.getResUserAddr(),
                        vo.getResMoveInDate(),
                        vo.getMemberId()
                ));
            } catch (Exception e) {
                // 개별 레코드 실패는 로깅하고 다음 건 진행(정책에 따라 전체 롤백도 가능)
                log.warn("AddressChange save failed. memberId={}, reason={}", memberId, e.toString());
            }
        }
        log.info("[END] generateAddressChange memberId={} resultSize={} elapsedMs={}",
                memberId, result.size(), (System.currentTimeMillis() - start));

        return result; // 컨트롤러에서 그대로 반환하거나 요약 DTO로 감싸 반환
    }
    /** 전입일 확정: resMoveInDate 우선, 없으면 resChangeDate 사용 */
    private static LocalDateTime resolveMoveIn(ResidentDataResponse.AddrChange a) {
        String d = (a.resMoveInDate != null && !a.resMoveInDate.isBlank())
                ? a.resMoveInDate
                : (a.resChangeDate != null ? a.resChangeDate : "");
        if (d.isBlank()) return null;
        return LocalDate.parse(d, D8).atStartOfDay(); // 00:00:00으로 정규화
    }

    /** 내부 작업용 튜플 */
    private record Temp(ResidentDataResponse.AddrChange src, LocalDateTime moveIn) {}


    // 테스트 메소드 todo: 지우기
    @Override
    @Transactional(readOnly = true)
    public List<ResRegisterCertResponse> generateAddressChangeFromRaw(String memberId, String rawJson) throws Exception {
        // 1) CODEF 응답의 data 노드만 DTO 매핑
        ResidentDataResponse data = CodefConverter.parseDataToDto(rawJson, ResidentDataResponse.class);
        if (data == null || data.addrChanges == null || data.addrChanges.isEmpty()) {
            log.info("No address changes in response. memberId={}", memberId);
            return List.of();
        }

        // 2) '전입'만 선별 → 전입일 확정 → 메모 라인 제외 → 전입일 오름차순
        var selected = data.addrChanges.stream()
                .filter(a -> "전입".equals(a.resChangeReason))
                .map(a -> new Temp(a, resolveMoveIn(a)))
                .filter(t -> t.moveIn != null && !AddrUtil.isMemoLine(t.src.resUserAddr))
                .sorted(Comparator.comparing(t -> t.moveIn))
                .toList(); // JDK 11이면 Collectors.toList() 사용

        List<ResRegisterCertResponse> result = new ArrayList<>();

        // 3) 저장 없이 변환만 수행
        for (Temp t : selected) {
            try {
                String original = t.src.resUserAddr == null ? "" : t.src.resUserAddr;

                // 검색용 전처리
                String q1 = AddrUtil.normalizeForSearchKeepDongHo(original);
                // 동·호 추출 → resNumber
                String resNumber = AddrUtil.extractDongHo(original).orElse(null);

                // 정책 기준일 이전이면 지번→도로명 변환 시도
                String saveAddr = q1;
                if (t.moveIn.toLocalDate().isBefore(CUTOFF)) {
                    String roadPart1 = jusoClient.searchBestRoadAddr1(q1);
                    if (roadPart1 == null || roadPart1.isBlank()) {
                        String q2 = AddrUtil.removeDongHo(q1);
                        roadPart1 = jusoClient.searchBestRoadAddr1(q2);
                    }
                    if (roadPart1 != null && !roadPart1.isBlank()) {
                        saveAddr = roadPart1; // 동·호는 resNumber에 분리 보관
                    }
                }

                // ★ DB 저장 없이 DTO만 생성해서 반환 리스트에 추가
                result.add(new ResRegisterCertResponse(
                        null,          // addressChangeId: 저장 안 하므로 null
                        resNumber,     // 동/호 (예: "1606동 902호" or "1606-902")
                        saveAddr,      // 도로명 주소(변환 성공 시) 또는 전처리 원문
                        t.moveIn,      // 전입일(LocalDateTime)
                        memberId
                ));
            } catch (Exception e) {
                log.warn("AddressChange preview failed. memberId={}, addr='{}'", memberId, t.src.resUserAddr, e);
            }
        }
        return result;
    }

    @Override
    public boolean hasLivedAtComplex(String memberId, long complexId) {
        return addressChangeMapper.countLivedByMemberAndComplexZone(memberId, complexId) > 0;
    }

}
