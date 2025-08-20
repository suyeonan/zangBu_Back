package bjs.zangbu.review.mapper;

import bjs.zangbu.review.dto.response.ReviewDetailResponse;
import bjs.zangbu.review.dto.response.ReviewListResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper {
    List<ReviewListResponse> selectByBuilding(@Param("buildingId") long buildingId);

    Long countByBuilding(@Param("buildingId") Long buildingId);

    // 최신 리뷰의 별점 가져오기
    Integer selectLatestReviewRank(@Param("buildingId") Long buildingId);

    // 리뷰 상세보기 기능
    ReviewDetailResponse selectById(@Param("reviewId") Long reviewId);

    // 리뷰 작성 기능
    int insertReview(ReviewInsertParam param);

    // 리뷰 삭제 기능
    int deleteReview(@Param("reviewId") Long reviewId);
}
