package bjs.zangbu.map.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AptDetailResponse {
    private String saleType;      // 매매 종류 (building.sale_type)
    private float size;           // 면적 (building.size)
    private String dong;          // 상세 주소 (complex_list.dong)
}
