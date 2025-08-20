package bjs.zangbu.deal.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberRequest {
    private String memberId;
    private String userName;
    private String identity;
    private String birthDate;
    private String phoneNo;
    private String telecom;
}
