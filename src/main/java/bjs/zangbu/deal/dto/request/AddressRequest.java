package bjs.zangbu.deal.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressRequest {
    private String address;
    private String dong;
    private String ho;
    private String zipcode;
    private String sido;
    private String roadName;
    private String sigungu;
}
