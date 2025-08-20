package bjs.zangbu.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class ReviewListResult {
    private long total;
    private List<ReviewListResponse> list;
    private boolean hasNext;
    private Integer latestRank;
    private String buildingName;
}
