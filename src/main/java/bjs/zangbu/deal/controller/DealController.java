package bjs.zangbu.deal.controller;


import bjs.zangbu.deal.dto.request.DealRequest;
import bjs.zangbu.deal.dto.request.DealRequest.IntentRequest;
import bjs.zangbu.deal.dto.request.DealRequest.Status;
import bjs.zangbu.deal.dto.response.DealResponse;
import bjs.zangbu.deal.dto.response.DealResponse.Download;
import bjs.zangbu.deal.dto.response.DealResponse.Notice;
import bjs.zangbu.deal.dto.response.DealResponse.NoticeBefore;
import bjs.zangbu.deal.dto.response.DealWaitingListResponse.WaitingList;
import bjs.zangbu.deal.service.ContractService;
import bjs.zangbu.deal.service.DealService;
import bjs.zangbu.deal.vo.DocumentType;
import bjs.zangbu.documentReport.dto.response.DocumentReportResponse.DocumentReportElement;
import bjs.zangbu.documentReport.service.DocumentReportService;
import bjs.zangbu.member.service.MemberService;
import bjs.zangbu.security.account.vo.CustomUser;
import com.github.pagehelper.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import springfox.documentation.annotations.ApiIgnore;


@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/deal")
@Api(value = "Deal API", description = "거래 관련 기능 API")
public class DealController {

  private final DealService dealService;
  private final MemberService memberService;
  private final ContractService contractService;
  private final DocumentReportService documentReportService;

  /* -------------------------------------------------
   * 1. 거래 안내/대기 목록
   * ------------------------------------------------- */

  /**
   * 거래 전 안내 페이지 이동
   *
   * @param dealId 거래 ID
   * @return 거래 안내 정보
   */
  @ApiOperation(
      value = "거래 안내 페이지 이동",
      notes = "거래 전 건물 ID를 기반으로 안내 정보를 반환합니다.",
      response = Notice.class
  )
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "안내 정보 반환 성공"),
      @ApiResponse(code = 400, message = "안내 정보 요청 실패")
  })
  @GetMapping("/notice/{dealId}")
  public ResponseEntity<?> moveNoticePage(
      @ApiParam(value = "건물 ID", example = "123")
      @PathVariable Long dealId) {
    try {
      Notice response = dealService.getNotice(dealId);
      return ResponseEntity.status(HttpStatus.OK).body(response);

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("안내 페이지 요청에 실패했습니다.");
    }
  }

  /**
   * 거래 전 안내 페이지 이동 (chatRoomId 없는 버전)
   *
   * @param buildingId 거래 ID
   * @return 거래 안내 정보
   */
  @ApiOperation(
      value = "거래 안내 페이지 이동",
      notes = "거래 전 건물 ID를 기반으로 안내 정보를 반환합니다.",
      response = Notice.class
  )
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "안내 정보 반환 성공"),
      @ApiResponse(code = 400, message = "안내 정보 요청 실패")
  })
  @GetMapping("/notice/before/{buildingId}")
  public ResponseEntity<?> moveNoticePageBefore(
      @ApiParam(value = "건물 ID", example = "123")
      @PathVariable Long buildingId) {
    try {
      NoticeBefore response = dealService.getNoticeBefore(buildingId);
      return ResponseEntity.status(HttpStatus.OK).body(response);

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("안내 페이지 요청에 실패했습니다.");
    }
  }

  /**
   * 사용자의 전체 거래 대기 매물 조회
   *
   * @param user 로그인 사용자 정보
   * @param page 요청 페이지 (1부터 시작)
   * @param size 페이지당 항목 수
   * @return 전체 거래 대기 매물 목록
   */
  @ApiOperation(
      value = "전체 거래 대기 매물 조회",
      notes = "사용자의 전체 거래 대기 매물을 조회합니다.",
      response = WaitingList.class
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "전체 대기 매물 조회 성공"),
      @ApiResponse(code = 400, message = "전체 대기 매물 조회 실패")

  })
  @GetMapping("/waitinglist")
  public ResponseEntity<?> getAllWaitingList(
      @ApiIgnore
      @AuthenticationPrincipal CustomUser user,
      @ApiParam(value = "페이지 번호", example = "1")
      @RequestParam(defaultValue = "1") int page,
      @ApiParam(value = "페이지당 항목 수", example = "10")
      @RequestParam(defaultValue = "10") int size) {
    try {
//      String email = user.getUsername();
      String memberId = user.getMember().getMemberId();
      String nickname = memberService.getNickname(memberId); // 닉네임 추출
      log.info("닉네임", nickname);

      // PageHelper 페이지네이션 시작
      PageHelper.startPage(page, size);
      // Response 생성
      // 내부적으로 LIMIT OFFSET 쿼리로 변환되어서 페이지네이션 됨
      WaitingList response = dealService.getAllWaitingList(memberId, nickname);

      return ResponseEntity.status(HttpStatus.OK).body(response);

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("전체 매물 리스트 불러오는데 실패했습니다.");

    }
  }

  /**
   * 사용자의 구매 진행 중인 매물 목록 조회
   *
   * @param user 로그인 사용자 정보
   * @param page 요청 페이지 (1부터 시작)
   * @param size 페이지당 항목 수
   * @return 구매 진행 중인 거래 대기 매물 목록
   */
  @ApiOperation(
      value = "구매 중인 대기 매물 조회",
      notes = "사용자의 구매 진행 중인 거래 대기 매물 목록을 조회합니다.",
      response = WaitingList.class
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "구매 대기 매물 조회 성공"),
      @ApiResponse(code = 400, message = "구매 대기 매물 조회 실패")
  })
  @GetMapping("/waitinglist/purchase")
  public ResponseEntity<?> getPurchaseWaitingList(
      @ApiIgnore
      @AuthenticationPrincipal CustomUser user,
      @ApiParam(value = "페이지 번호", example = "1")
      @RequestParam(defaultValue = "1") int page,
      @ApiParam(value = "페이지당 항목 수", example = "10")
      @RequestParam(defaultValue = "10") int size) {
    try {

      String memberId = user.getMember().getMemberId();
      String nickname = memberService.getNickname(memberId); // 닉네임 추출

      // PageHelper 페이지네이션 시작
      PageHelper.startPage(page, size);
      WaitingList response = dealService.getPurchaseWaitingList(memberId, nickname);
      return ResponseEntity.status(HttpStatus.OK).body(response);

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("구매 중인 리스트 불러오는데 실패했습니다.");

    }
  }

  /**
   * 사용자의 판매 중인 거래 대기 매물 목록 조회
   *
   * @param user 로그인 사용자 정보
   * @param page 요청 페이지 번호 (1부터 시작)
   * @param size 페이지당 항목 수
   * @return 판매 중인 거래 대기 매물 목록
   */
  @ApiOperation(
      value = "판매 중인 대기 매물 조회",
      notes = "사용자의 판매 진행 중인 거래 대기 매물 목록을 조회합니다.",
      response = WaitingList.class
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "판매 대기 매물 조회 성공"),
      @ApiResponse(code = 400, message = "판매 대기 매물 조회 실패")
  })
  @GetMapping("/waitinglist/onsale")
  public ResponseEntity<?> getOnSaleWaitingList(
      @ApiIgnore
      @AuthenticationPrincipal CustomUser user,
      @ApiParam(value = "페이지 번호", example = "1")
      @RequestParam(defaultValue = "1") int page,
      @ApiParam(value = "페이지당 항목 수", example = "10")
      @RequestParam(defaultValue = "10") int size) {
    try {
      String memberId = user.getMember().getMemberId();
      String nickname = memberService.getNickname(memberId); // 닉네임 추출

      // PageHelper 페이지네이션 시작
      PageHelper.startPage(page, size);
      WaitingList response = dealService.getOnSaleWaitingList(memberId, nickname);
      return ResponseEntity.status(HttpStatus.OK).body(response);

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("판매 중인 리스트 불러오는데 실패했습니다.");

    }
  }

  /**
   * 거래 취소 (삭제)
   *
   * @param dealId 거래 ID
   * @return 삭제 처리 결과 메시지
   */
  @ApiOperation(value = "거래 취소", notes = "거래 ID를 기반으로 해당 거래를 삭제(취소)합니다.", response = String.class)
  @ApiResponses({
      @ApiResponse(code = 204, message = "거래 취소 성공"),
      @ApiResponse(code = 400, message = "거래 취소 실패"),
      @ApiResponse(code = 404, message = "예기치 못한 서버 오류")
  })
  @DeleteMapping("/remove/{dealId}")
  public ResponseEntity<?> removeDeal(
      @ApiParam(value = "거래 ID", example = "123")
      @PathVariable Long dealId) {
    try {
      // TODO : 거래 취소하는 당사자가 판매자, 구매자 인지 check 로직 추가
      if (dealService.deleteDealById(dealId)) {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("거래 취소에 성공했습니다.");
      } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("거래 취소에 실패했습니다.");
      }
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("예기치 못한 문제가 발생했습니다.");
    }
  }

  /* -------------------------------------------------
   * 2. 구매자 전용 (documents / membership / report …)
   * ------------------------------------------------- */

  /**
   * 등기/건축 서류 다운로드
   *
   * @param buildingId 매물 ID
   * @param type       문서 종류 (예: BUILDING_REGISTER, ARCHITECTURE 등)
   * @return PDF 다운로드 링크
   */
  @ApiOperation(value = "등기/건축 서류 다운로드(자동 첫 발급 포함))",
      notes = "최신 URL이 없으면 즉시 발급하여 URL을 반환합니다.",
      response = Download.class)
  @GetMapping("/consumer/documents/{buildingId}/{type}/download")
  public ResponseEntity<?> downloadDocument(
      @ApiIgnore @AuthenticationPrincipal CustomUser user,
      @ApiParam(value = "매물 ID", example = "123")
      @PathVariable Long buildingId,
      @ApiParam(value = "문서 타입")
      @PathVariable DocumentType type) throws Exception {

    final String memberId = user.getMember().getMemberId();

    // 1) 최신 URL 조회 → 있으면 그대로 반환
    String existing = documentReportService.getLatestUrlOrNull(memberId, buildingId, type);
    if (existing != null && !existing.isBlank()) {
      return ResponseEntity.ok(new DealResponse.Download(existing));
    }

    // 2) 없으면 "첫 발급" 수행 (문서 타입에 따라 분기)
    DealResponse.Download issued = switch (type) {
      case ESTATE -> contractService.getEstateRegisterPdf(memberId, buildingId);
      case BUILDING_REGISTER -> contractService.getBuildingRegisterPdf(memberId, buildingId);
      default -> throw new IllegalArgumentException("지원하지 않는 문서 타입: " + type);
    };
    // 3) 업로드가 켜져 있어 URL이 내려왔는지 확인 후 저장·반환
    String newUrl = (issued == null) ? null : issued.getUrl();
    if (newUrl == null || newUrl.isBlank()) {
      // 업로드 비활성/실패 등인 경우
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("발급은 되었으나 URL이 비었습니다. 업로드 설정을 확인하세요.");
    }

    // 최신 URL upsert (중복 upsert 무해)
    documentReportService.saveLatestUrl(memberId, buildingId, type, newUrl);

    return ResponseEntity.ok(new DealResponse.Download(newUrl));
  }

  @ApiOperation(
      value = "등기/건축 서류 수동 갱신(재발급)",
      notes = "강제로 재발급을 수행하고, 업로드가 성공하면 최신 URL을 반환합니다.",
      response = Download.class
  )
  @PostMapping("/consumer/documents/{buildingId}/{type}/refresh")
  public ResponseEntity<?> refreshDocument(
      @ApiIgnore @AuthenticationPrincipal CustomUser user,
      @PathVariable Long buildingId,
      @PathVariable DocumentType type
  ) throws Exception {
    final String memberId = user.getMember().getMemberId();

    DealResponse.Download issued;
    switch (type) {
      case ESTATE:
        issued = contractService.getEstateRegisterPdf(memberId, buildingId);
        break;
      case BUILDING_REGISTER:
        issued = contractService.getBuildingRegisterPdf(memberId, buildingId);
        break;
      default:
        return ResponseEntity.badRequest()
            .body("지원하지 않는 문서 타입: " + type);
    }

    String newUrl = (issued == null) ? null : issued.getUrl();
    if (newUrl == null || newUrl.isBlank()) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("발급은 되었으나 URL이 비었습니다. 업로드 설정을 확인하세요.");
    }
    documentReportService.saveLatestUrl(memberId, buildingId, type, newUrl);
    return ResponseEntity.ok(new DealResponse.Download(newUrl));
  }

  /**
   * 분석 리포트 주문 → 결제 페이지 이동
   *
   * @param dto 결제 의도 요청 DTO
   * @return 상태 200 OK
   */
  @ApiOperation(value = "분석 리포트 결제 요청", notes = "분석 리포트 주문 후 결제 페이지로 이동합니다.", response = String.class)
  @PostMapping("/consumer/membership")
  public ResponseEntity<?> orderAnalysisReport(
      @ApiParam(value = "결제 의도 정보", required = true)
      @RequestBody IntentRequest dto) {
    return ResponseEntity.ok().build();
  }

  /**
   * 분석 리포트 다운로드
   *
   * @param reportId 리포트 ID
   * @return PDF 파일 또는 다운로드 링크
   */
  @ApiOperation(value = "분석 리포트 다운로드", notes = "리포트 ID를 통해 분석 리포트 PDF를 다운로드합니다.", response = Download.class)
  @GetMapping("/consumer/report/{reportId}/download")
  public ResponseEntity<?> downloadAnalysisReport(
      @ApiParam(value = "리포트 ID", example = "1001")
      @PathVariable Long reportId) {
    return ResponseEntity.ok().build();
  }

  /**
   * 표준 계약서 다운로드
   *
   * @param dealId 거래 ID
   * @return 계약서 다운로드 링크
   */
  @ApiOperation(value = "표준 계약서 다운로드", notes = "거래 ID를 기반으로 표준 계약서 PDF를 다운로드합니다.", response = Download.class)
  @GetMapping("/consumer/contract/{dealId}/download")
  public ResponseEntity<?> downloadContractReport(
      @ApiParam(value = "거래 ID", example = "1001")
      @PathVariable Long dealId) {    // 1. 상대 경로
    String relativePath = contractService.getContractPdf(dealId);

    //2. 절대 URL(Host·Port 포함) 생성
    String absolutePath = ServletUriComponentsBuilder
        .fromCurrentContextPath()
        .path(relativePath)
        .toUriString();

    return ResponseEntity.ok(new DealResponse.Download(absolutePath));
  }

  /**
   * 결제 완료 후 리포트 페이지 이동
   *
   * @param reportId 리포트 ID
   * @return 리포트 조회 페이지로 이동
   */
  @ApiOperation(value = "결제 완료 후 리포트 페이지 이동", notes = "리포트 결제가 완료된 후 해당 리포트 페이지로 이동합니다.", response = String.class)
  @PostMapping("/consumer/report/{reportId}")
  public ResponseEntity<?> moveReportPage(
      @ApiParam(value = "리포트 ID", example = "600")
      @PathVariable Long reportId) {
    return ResponseEntity.ok().build();
  }

  /* -------------------------------------------------
   * 3. 상태 변경
   * ------------------------------------------------- */

  /**
   * 거래 상태 변경
   *
   * @param dto 거래 상태 변경 요청 DTO
   * @return 변경 결과 메시지
   */
  @ApiOperation(value = "거래 상태 변경", notes = "거래 상태(예: WAITING → COMPLETED 등)를 변경합니다.", response = String.class)
  @ApiResponses({
      @ApiResponse(code = 200, message = "상태 변경 성공"),
      @ApiResponse(code = 400, message = "상태 변경 실패"),
      @ApiResponse(code = 404, message = "예기치 못한 오류 발생")
  })
  @PatchMapping("/status")
  public ResponseEntity<?> changeDealStatus(
      @ApiParam(value = "거래 상태 변경 요청 DTO", required = true)
      @RequestBody Status dto) {
    try {
      // 상태 patch
      if (dealService.patchStatus(dto)) {
        return ResponseEntity.status(HttpStatus.OK).body("상태변경에 성공했습니다.");
      } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("상태변경에 실패했습니다.");
      }
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("예기치 못한 문제가 발생했습니다.");
    }

  }

  /**
   * 분석 리포트 상세 조회
   *
   * @param reportId 리포트 ID
   * @return 리포트 상세 정보
   */
  @ApiOperation(value = "분석 리포트 상세 조회", notes = "리포트 ID를 기반으로 리포트 상세 정보를 반환합니다.", response = DocumentReportElement.class)
  @ApiResponses({
      @ApiResponse(code = 200, message = "리포트 조회 성공"),
      @ApiResponse(code = 400, message = "리포트 요청 실패"),
      @ApiResponse(code = 404, message = "리포트를 찾을 수 없음")
  })
  @GetMapping("/consumer/report/{reportId}")
  public ResponseEntity<?> getReport(
      @ApiParam(value = "리포트 ID", example = "600")
      @PathVariable Long reportId) {
    try {
      DocumentReportElement response = documentReportService.getDocumentReportByReportId(reportId);
      return ResponseEntity.status(HttpStatus.OK).body(response);
    } catch (NullPointerException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("리포트 요청에 실패했습니다.");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("리포트를 찾을 수 없습니다.");
    }
  }

  @PostMapping(value = "", consumes = "application/json", produces = "application/json;charset=UTF-8")
  public ResponseEntity<Long> createDeal(@RequestBody DealRequest.CreateDeal req) {
    Long dealId = dealService.createDeal(req.getChatRoomId());
    return ResponseEntity.status(201).body(dealId);
  }

}
