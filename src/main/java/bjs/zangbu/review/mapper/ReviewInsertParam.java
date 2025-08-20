package bjs.zangbu.review.mapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewInsertParam {
    private Long reviewId; // 자동 생성된 PK
    private Long buildingId;
    private String memberId;
    private String reviewerNickname;
    private Integer rank;
    private String content;
    private String floor;
}