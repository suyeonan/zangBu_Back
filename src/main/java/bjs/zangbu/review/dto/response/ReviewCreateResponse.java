package bjs.zangbu.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewCreateResponse {
    private Long reviewId;
    private Long buildingId;
    private String reviewerNickname;
    private String floor;
    private Integer rank;
    private String content;
    private String createdAt; // "YYYY-MM-DD HH:mm:ss"
}