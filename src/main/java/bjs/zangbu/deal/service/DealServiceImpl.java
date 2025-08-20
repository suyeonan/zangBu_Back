package bjs.zangbu.deal.service;

import bjs.zangbu.building.mapper.BuildingMapper;
import bjs.zangbu.building.vo.Building;
import bjs.zangbu.chat.service.ChatService;
import bjs.zangbu.deal.dto.join.DealWithChatRoom;
import bjs.zangbu.deal.dto.request.DealRequest.Status;
import bjs.zangbu.deal.dto.response.DealResponse.CreateResult;
import bjs.zangbu.deal.dto.response.DealResponse.Notice;
import bjs.zangbu.deal.dto.response.DealResponse.NoticeBefore;
import bjs.zangbu.deal.dto.response.DealWaitingListResponse.WaitingList;
import bjs.zangbu.deal.mapper.DealMapper;
import bjs.zangbu.deal.vo.DealEnum;
import bjs.zangbu.imageList.service.ImageListService;
import bjs.zangbu.notification.service.NotificationService;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 거래 관련 비즈니스 로직 구현체
 *
 * <p>거래 생성/삭제, 대기 목록 조회, 상태 전환( {@link DealEnum} )을 담당</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class DealServiceImpl implements DealService {

  private final DealMapper dealMapper;
  private final BuildingMapper buildingMapper;
  private final ImageListService imageListService;
  private final NotificationService notificationService;
  private final ChatService chatService;

  /**
   * 거래 전 안내 조회
   *
   * @param dealId 매물 식별 ID
   * @return 거래 전 안내 DTO
   */
  @Override
  public Notice getNotice(Long dealId) {
    // buildingId 조회
    Long buildingId = dealMapper.getBuildingIdByDealId(dealId);

    // chatRoomId 조회
    String chatRoomId = dealMapper.getRoomIdByDealId(dealId);

    // Building 조회
    Building buildVO = buildingMapper.getBuildingById(buildingId);

    return Notice.toDto(dealId, chatRoomId, buildVO);
  }

  /**
   * (구매/판매 구분 없이) 내가 관여한 대기 중 거래 목록 조회
   *
   * @param memberId 회원 식별 ID
   * @param nickname 나의 닉네임
   * @return 대기 목록 DTO
   */
  @Override
  public WaitingList getAllWaitingList(String memberId, String nickname) {
    List<DealWithChatRoom> deals = dealMapper.getAllWaitingList(memberId);
    return buildWaitingList(deals, nickname);

  }

  /**
   * 내가 구매자로 참여 중인 대기 거래 목록 조회
   *
   * @param memberId 회원 식별 ID
   * @param nickname 나의 닉네임(구매자)
   * @return 대기 목록 DTO
   */
  @Override
  public WaitingList getPurchaseWaitingList(String memberId, String nickname) {
    List<DealWithChatRoom> deals = dealMapper.getPurchaseWaitingList(memberId, nickname);
    return buildWaitingList(deals, nickname);

  }

  /**
   * 내가 판매자로 참여 중인 대기 거래 목록 조회
   *
   * @param memberId 회원 식별 ID
   * @param nickname 나의 닉네임(판매자)
   * @return 대기 목록 DTO
   */
  @Override
  public WaitingList getOnSaleWaitingList(String memberId, String nickname) {
    List<DealWithChatRoom> deals = dealMapper.getOnSaleWaitingList(memberId, nickname);
    return buildWaitingList(deals, nickname);
  }

  /**
   * 대기 목록을 페이지 정보와 대표 이미지 포함 형태로 구성
   *
   * @param deals    조회된 거래+채팅방 조인 결과
   * @param nickname 나의 닉네임
   * @return 대기 목록 DTO
   */
  private WaitingList buildWaitingList(List<DealWithChatRoom> deals, String nickname) {
    PageInfo<DealWithChatRoom> pageInfo = new PageInfo<>(deals);

    // image map 생성
    Map<Long, String> imageMap = deals.stream()
        .map(DealWithChatRoom::getBuildingId)
        .distinct()
        .collect(Collectors.toMap(
            Function.identity(),
            imageListService::representativeImage
        ));

    return WaitingList.toDto(pageInfo, nickname, imageMap);
  }

  /**
   * 거래 삭제
   *
   * @param dealId 거래 식별 ID
   * @return 삭제 성공 여부
   */
  @Override
  @Transactional
  public boolean deleteDealById(Long dealId) {
    return dealMapper.deleteDealById(dealId) == 1;
  }

  /**
   * 거래 상태 전환
   *
   * <p>현재 상태에서 {@link bjs.zangbu.deal.vo.DealEnum}의 다음 유효 상태로 전환</p>
   *
   * @param status 거래 ID와 목표 상태를 담은 요청 DTO ({@link bjs.zangbu.deal.dto.request.DealRequest.Status})
   * @return 전환 및 업데이트 성공 여부
   */
  @Override
  @Transactional
  public boolean patchStatus(Status status) {
    // 현재(DB) 상태
    String from = dealMapper.getStatusByDealId(status.getDealId());
    // 바꿀(요청) 상태
    String to = status.getStatus();
    log.info("DealServiceImpl - patchStatus: " + from + " -> " + to);

    // 상태 FLOW 체크: from -> to 가 유효한지
    if (!checkStatus(from, to)) {
      log.warn("Invalid transition {} -> {} (dealId={})", from, to, status.getDealId());
      return false;
    }

    // 상태 업데이트
    int updated = dealMapper.patchStatus(status);
    if (updated != 1) {
      return false;
    }
    String roomId = status.getChatRoomId();
    //= dealMapper.getRoomIdByDealId(status.getDealId());

    try {
      switch (to) {
        case "BEFORE_CONSUMER":
          chatService.publishSystemMessage(roomId, "판매자가 거래를 활성화했습니다.");
          break;
        case "MIDDLE_DEAL":
          chatService.publishSystemMessage(roomId, "구매자가 거래를 수락했습니다. 거래가 시작되었습니다.");
          break;
        case "CLOSE_DEAL":
          chatService.publishSystemMessage(roomId, "거래가 완료되었습니다.");
          break;
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    // 성공적으로 업데이트된 뒤 알림
    if ("CLOSE_DEAL".equals(to)) {
      notificationService.detectTradeHappenedNow(status.getDealId());
    }
    return true;
  }

  /**
   * 거래 생성
   *
   * @param chatRoomId 채팅방 식별 ID
   * @return 생성된 거래 식별 ID
   */
  @Override
  @Transactional
  public Long createDeal(String chatRoomId) {
    CreateResult result = new CreateResult();
    dealMapper.createDeal(chatRoomId, result);
    return result.getDealId();
  }

  @Override
  public NoticeBefore getNoticeBefore(Long buildingId) {
    // Building 조회
    Building buildVO = buildingMapper.getBuildingById(buildingId);

    return NoticeBefore.toDto(buildVO);
  }


  /**
   * 유효한 상태 전환 정의
   *
   * <pre>
   * BEFORE_TRANSACTION → BEFORE_OWNER → BEFORE_CONSUMER → MIDDLE_DEAL → CLOSE_DEAL
   * </pre>
   */
  private static final Map<DealEnum, DealEnum> validTransitions = Map.of(
      DealEnum.BEFORE_TRANSACTION, DealEnum.BEFORE_OWNER,
      DealEnum.BEFORE_OWNER, DealEnum.BEFORE_CONSUMER,
      DealEnum.BEFORE_CONSUMER, DealEnum.MIDDLE_DEAL,
      DealEnum.MIDDLE_DEAL, DealEnum.CLOSE_DEAL
  );

  /**
   * 상태 전환 유효성 검사
   *
   * @param from 현재 상태 (DB 저장 문자열)
   * @param to   목표 상태 (요청 문자열)
   * @return 유효 전환이면 true
   */
  private boolean checkStatus(String from, String to) {
    try {
      DealEnum fromEnum = DealEnum.valueOf(from);
      DealEnum toEnum = DealEnum.valueOf(to);
      // FLOW 가 맞는 지 체크
      return validTransitions.getOrDefault(fromEnum, null) == toEnum;
    } catch (IllegalArgumentException | NullPointerException e) {
      return false;
    }
  }


}
