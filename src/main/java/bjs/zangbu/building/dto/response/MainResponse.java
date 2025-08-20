package bjs.zangbu.building.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 메인 페이지 응답 DTO를 포함하는 클래스
 */
public class MainResponse {

    /**
     * 메인 페이지에 필요한 주요 정보 응답 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainPageResponse {
        /** 사용자 닉네임 */
        private String nickName;
        /** 리뷰가 많은 매물 Top 목록 */
        private List<BuildingInfo> topReviewed;
        /** 찜이 많은 매물 Top 목록 */
        private List<BuildingInfo> topLiked;
        /** 신규 등록된 매물 목록 */
        private List<BuildingInfo> newRooms;

        /**
         * DTO 생성 메서드
         * @param nickName 사용자 닉네임
         * @param topReviewed 리뷰 많은 매물 리스트
         * @param topLiked 찜 많은 매물 리스트
         * @param newRooms 신규 매물 리스트
         * @return MainPageResponse DTO 객체
         */
        public static MainPageResponse toDto(String nickName, List<BuildingInfo> topReviewed, List<BuildingInfo> topLiked, List<BuildingInfo> newRooms) {
            return new MainPageResponse(
                    nickName,
                    topReviewed,
                    topLiked,
                    newRooms);
        }
    }

    /**
     * 매물 정보 DTO (메인 페이지에서 사용)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuildingInfo {
        /** 매물 고유 ID */
        private Long buildingId;
        /** 매물 가격 */
        private Integer price;

        private Integer deposit;
        /** 매물 이름 */
        private String buildingName;
        /** 매물 이미지 URL */
        private String imageUrl;
        /** 사용자가 찜했는지 여부 */
        private Boolean isBookmarked;
        /** 평균 별점 */
        private float rank;
    }
}
