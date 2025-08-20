package bjs.zangbu.deal.service;

import bjs.zangbu.deal.dto.request.DealRequest.Status;
import bjs.zangbu.deal.dto.response.DealResponse.Notice;
import bjs.zangbu.deal.dto.response.DealResponse.NoticeBefore;
import bjs.zangbu.deal.dto.response.DealWaitingListResponse.WaitingList;
import bjs.zangbu.deal.vo.DealEnum;

/**
 * 거래 관련 서비스 인터페이스
 *
 * <p>거래 생성/삭제, 대기 목록 조회, 상태 전환({@link DealEnum}) 등의 기능을 정의</p>
 */
public interface DealService {

  /**
   * 거래 전 안내 조회
   *
   * @param dealId 매물 식별 ID
   * @return 거래 전 안내 정보 DTO
   */
  Notice getNotice(Long dealId);

  /**
   * 내가 참여 중인 전체 대기 거래 목록 조회
   *
   * @param memberId 회원 식별 ID
   * @param nickname 회원 닉네임
   * @return 대기 거래 목록 DTO
   */
  WaitingList getAllWaitingList(String memberId, String nickname);

  /**
   * 내가 구매자로 참여 중인 대기 거래 목록 조회
   *
   * @param memberId 회원 식별 ID
   * @param nickname 회원 닉네임(구매자)
   * @return 대기 거래 목록 DTO
   */
  WaitingList getPurchaseWaitingList(String memberId, String nickname);

  /**
   * 내가 판매자로 참여 중인 대기 거래 목록 조회
   *
   * @param memberId 회원 식별 ID
   * @param nickname 회원 닉네임(판매자)
   * @return 대기 거래 목록 DTO
   */
  WaitingList getOnSaleWaitingList(String memberId, String nickname);

  /**
   * 거래 삭제
   *
   * @param dealId 거래 식별 ID
   * @return 삭제 성공 여부
   */
  boolean deleteDealById(Long dealId);

  /**
   * 거래 상태 전환
   *
   * <p>현재 상태에서 {@link bjs.zangbu.deal.vo.DealEnum}의 다음 유효 상태로 전환</p>
   *
   * @param status 거래 ID와 목표 상태를 담은 요청 DTO ({@link bjs.zangbu.deal.dto.request.DealRequest.Status})
   * @return 전환 및 업데이트 성공 여부
   */
  boolean patchStatus(Status status);

  /**
   * 거래 생성
   *
   * @param chatRoomId 채팅방 식별 ID
   * @return 생성된 거래 식별 ID
   */
  Long createDeal(String chatRoomId);

  /**
   * 거래 전 안내 조회(chatRoomId X)
   *
   * @param buildingId 매물 식별 ID
   * @return 거래 전 안내 정보 DTO
   */
  NoticeBefore getNoticeBefore(Long buildingId);
}
