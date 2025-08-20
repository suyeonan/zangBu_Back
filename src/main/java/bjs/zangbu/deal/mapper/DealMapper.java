package bjs.zangbu.deal.mapper;

import bjs.zangbu.deal.dto.join.DealDocumentInfo;
import bjs.zangbu.deal.dto.join.DealWithChatRoom;
import bjs.zangbu.deal.dto.join.DealWithSaleType;
import bjs.zangbu.deal.dto.request.AddressRequest;
import bjs.zangbu.deal.dto.request.DealRequest.Status;
import bjs.zangbu.deal.dto.request.EstateRegistrationRequest;
import bjs.zangbu.deal.dto.request.MemberRequest;
import bjs.zangbu.deal.dto.response.DealResponse.CreateResult;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 거래 관련 MyBatis Mapper
 *
 * <p>거래 생성/삭제, 상태 변경, 목록 조회 및 계약·등기·건축물대장 조회를 위한 데이터를 조회</p>
 */
@Mapper
public interface DealMapper {

  /**
   * 내가 참여한 모든 대기 거래 목록 조회
   *
   * @param memberId 회원 식별 ID
   * @return 대기 거래 목록
   */
  List<DealWithChatRoom> getAllWaitingList(@Param("memberId") String memberId);

  /**
   * 거래 삭제
   *
   * @param dealId 거래 식별 ID
   * @return 삭제된 행 수
   */
  int deleteDealById(@Param("dealId") Long dealId);

  /**
   * 거래 상태 변경
   *
   * @param status 거래 ID와 목표 상태를 담은 요청 DTO ({@link bjs.zangbu.deal.dto.request.DealRequest.Status})
   * @return 변경된 행 수
   */
  int patchStatus(Status status);

  /**
   * 거래 상태 조회
   *
   * @param dealId 거래 식별 ID
   * @return 현재 거래 상태 (문자열, {@link bjs.zangbu.deal.vo.DealEnum} 값)
   */
  String getStatusByDealId(@Param("dealId") Long dealId);

  /**
   * 내가 구매자로 참여 중인 대기 거래 목록 조회
   *
   * @param memberId 회원 식별 ID
   * @param nickname 회원 닉네임(구매자)
   * @return 대기 거래 목록
   */
  List<DealWithChatRoom> getPurchaseWaitingList(@Param("memberId") String memberId,
      @Param("nickname") String nickname);

  /**
   * 내가 판매자로 참여 중인 대기 거래 목록 조회
   *
   * @param memberId 회원 식별 ID
   * @param nickname 회원 닉네임(판매자)
   * @return 대기 거래 목록
   */
  List<DealWithChatRoom> getOnSaleWaitingList(@Param("memberId") String memberId,
      @Param("nickname") String nickname);

  /**
   * 등기부등본 조회에 필요한 데이터 조회
   *
   * @param buildingId 거래 식별 ID
   * @return 등기부등본 요청 DTO
   */
  EstateRegistrationRequest getEstateRegistrationRequest(@Param("buildingId") Long buildingId);

  /**
   * 건축물대장 조회에 필요한 데이터 조회
   *
   * @param buildingId 거래 식별 ID
   * @return 건축물대장 정보 DTO
   */
  DealDocumentInfo getDocumentInfo(@Param("buildingId") Long buildingId);
  AddressRequest getAddressRequest(@Param("buildingId") Long buildingId);
  MemberRequest getMemberRequest(@Param("memberId") String memberId);
  /**
   * 표준계약서 XML 코드 조회
   *
   * @param dealId 거래 식별 ID
   * @return 매물의 판매 유형과 계약서 정보 DTO
   */
  DealWithSaleType findWithType(@Param("dealId") Long dealId);

  /**
   * 오늘 거래된 매물들의 building_id 목록 조회
   *
   * @return building_id 목록
   */
  List<Long> selectTodayTradedBuildingIds();

  /**
   * 거래 신규 생성
   *
   * @param chatRoomId 채팅방 식별 ID
   * @param result     생성된 거래 ID를 담을 DTO
   * @return 생성된 행 수
   */
  int createDeal(@Param("chatRoomId") String chatRoomId,
      @Param("result") CreateResult result);

  /**
   * 거래 ID로 buildingId 조회
   *
   * @param dealId 거래 식별 ID
   * @return buildingId
   */
  Long getBuildingIdByDealId(@Param("dealId") Long dealId);

  /**
   * 거래 ID로 complexId 조회
   *
   * @param dealId 거래 식별 ID
   * @return complexId
   */
  Long getComplexIdByDealId(@Param("dealId") Long dealId);

  String getRoomIdByDealId(Long dealId);
}
