package bjs.zangbu.review.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewCreateRequest {
    private Long buildingId;
    private String floor; // 층수 정보 (저층/중층/고층)
    private Integer rank; // 별점 (1~5)
    private String content; // 리뷰 본문
}
