package bjs.zangbu.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewDetailResponse {
    private Long reviewId; // 리뷰 고유 ID
    private Long buildingId;
    private String reviewerNickname;
    private Integer rank;
    private String content;
    private String createdAt;
}
