package bjs.zangbu.deal.dto.request;

import bjs.zangbu.deal.dto.join.DealDocumentInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BuildingRegisterRequest {
    //todo : dealdocumentinfo와 같은 형식이면 둘중 하나 날리는게 나을듯
    /** dealId → mapper 에서 채운다 */
    private Long buildingId;

    // --- CODEF 파라미터 ---
    private String userName;       // 사용자 이름
    private String identity;    // RSA 암호화된 주민번호(뒷자리)
    private String birthDate;      // YYMMDD
    private String phoneNo;        // 휴대전화
    private String address;        // 도로명 주소
    private String dong;           // 동
    private String ho;             // 호
    private String telecom;       // 통신사
    private String zipCode;        // 우편번호 (zonecode)

    /** mapper→DTO 변환 헬퍼 */
    public static BuildingRegisterRequest from(DealDocumentInfo info){
        return BuildingRegisterRequest.builder()
                .buildingId(info.getBuildingId())
                .userName(info.getName())
                .identity(info.getIdentity())
                .birthDate(info.getBirth())
                .phoneNo(info.getPhone())
                .address(info.getAddress())
                .zipCode(info.getZonecode())
                .telecom(info.getTelecom())
                .dong(info.getDong())
                .ho(info.getHo())
                .build();
    }
    public static BuildingRegisterRequest addTwoRequest(AddressRequest address, MemberRequest member){
        return BuildingRegisterRequest.builder()
                .buildingId(1L)
                .userName(member.getUserName())
                .identity(member.getIdentity())
                .birthDate(member.getBirthDate())
                .phoneNo(member.getPhoneNo())
                .address(address.getAddress())
                .zipCode(address.getZipcode())
                .telecom(member.getTelecom())
                .dong(address.getDong())
                .ho(address.getHo())
                .build();
    }
}
