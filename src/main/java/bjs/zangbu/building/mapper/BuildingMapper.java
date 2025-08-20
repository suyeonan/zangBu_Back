package bjs.zangbu.building.mapper;

import bjs.zangbu.building.dto.request.BuildingRequest;
import bjs.zangbu.building.dto.request.BuildingRequest.UpdateBuilding;
import bjs.zangbu.building.dto.response.BuildingResponse.FilteredResponse.Filtered;
import bjs.zangbu.building.dto.response.MainResponse.BuildingInfo;
import bjs.zangbu.building.vo.Building;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Building 관련 DB 매핑 인터페이스 (MyBatis Mapper) 매물(건물) 조회, 필터링, 찜 수 관리 등을 담당합니다.
 */
@Mapper
public interface BuildingMapper {

    String selectBuildingNameById(Long buildingId);

    /**
     * 특정 매물의 찜 수를 1 감소시킵니다.
     *
     * @param buildingId 찜 수를 감소시킬 매물 ID
     */
    void decrementBookmarkCount(Long buildingId);

    /**
     * 특정 매물의 찜 수를 1 증가시킵니다.
     *
     * @param buildingId 찜 수를 증가시킬 매물 ID
     */
    void incrementBookmarkCount(Long buildingId);

    /**
     * 매물이 존재하는지 여부를 확인합니다.
     *
     * @param buildingId 존재 여부를 확인할 매물 ID
     * @return 존재하면 true, 아니면 false
     */
    boolean isBuildingExists(Long buildingId);

    /**
     * 특정 매물의 상세 정보를 조회합니다.
     *
     * @param buildingId 조회할 매물 ID
     * @return 조회된 매물 정보 객체
     */
    Building getBuildingById(Long buildingId);

    /**
     * 특정 매물의 현재 가격을 조회합니다.
     *
     * @param buildingId 가격을 조회할 매물 ID
     * @return 매물의 현재 가격 (Integer)
     */
    Integer selectCurrentPrice(Long buildingId);

    /**
     * 새로운 매물을 등록하고, 생성된 매물 ID를 반환합니다.
     *
     * @param building 등록할 매물 객체
     * @return 생성된 매물 ID (Long)
     */
    int createBuilding(Building building);

    Long selectLastInsertId();

    /**
     * 특정 매물을 삭제합니다.
     *
     * @param buildingId 삭제할 매물 ID
     */
    void deleteBuilding(Long buildingId);

    /**
     * 필터 조건에 맞는 매물 목록을 조회합니다. (평점 포함)
     *
     * @param buildingName 매물명 필터 (부분 일치, null 가능)
     * @param saleType     매물 거래 유형 필터 (ex: "매매", "전세", null 가능)
     * @param startPrice   가격 범위 시작 (최소 가격, null 가능)
     * @param endPrice     가격 범위 끝 (최대 가격, null 가능)
     * @param propertyType 매물 유형 필터 (ex: "아파트", "오피스텔", null 가능)
     * @return 필터링된 매물 리스트 (Filtered 객체 리스트)
     */
    List<Filtered> selectFilteredBuildings(
            @Param("buildingName") String buildingName,
            @Param("saleType") String saleType,
            @Param("startPrice") Long startPrice,
            @Param("endPrice") Long endPrice,
            @Param("propertyType") String propertyType);

    /**
     * 필터 조건에 맞는 매물 총 개수를 조회합니다.
     *
     * @param buildingName 매물명 필터 (부분 일치, null 가능)
     * @param saleType     매물 거래 유형 필터 (null 가능)
     * @param startPrice   가격 범위 시작 (null 가능)
     * @param endPrice     가격 범위 끝 (null 가능)
     * @param propertyType 매물 유형 필터 (null 가능)
     * @return 필터 조건에 부합하는 매물 총 개수
     */
    long countFilteredBuildings(
            @Param("buildingName") String buildingName,
            @Param("saleType") String saleType,
            @Param("startPrice") Long startPrice,
            @Param("endPrice") Long endPrice,
            @Param("propertyType") String propertyType);

    /**
     * 리뷰가 많은 매물 Top 3를 조회합니다.
     *
     * @param memberId 조회 기준 회원 ID (찜 여부 표시용)
     * @return 리뷰 많은 매물 Top 3 리스트 (BuildingInfo 객체 리스트)
     */
    List<BuildingInfo> selectTopReviewedBuildings(@Param("memberId") String memberId);

    /**
     * 찜(좋아요) 수가 많은 매물 Top 3를 조회합니다.
     *
     * @param memberId 조회 기준 회원 ID (찜 여부 표시용)
     * @return 찜 많은 매물 Top 3 리스트 (BuildingInfo 객체 리스트)
     */
    List<BuildingInfo> selectTopLikedBuildings(@Param("memberId") String memberId);

    /**
     * 최근에 등록된 신규 매물 Top 3를 조회합니다.
     *
     * @param memberId 조회 기준 회원 ID (찜 여부 표시용)
     * @return 신규 등록 매물 Top 3 리스트 (BuildingInfo 객체 리스트)
     */
    List<BuildingInfo> selectNewRooms(@Param("memberId") String memberId);

    Integer getDeposit(Long buildingId);

    void updateBuilding(@Param("building") UpdateBuilding building,
            @Param("memberId") String memberId);
}
