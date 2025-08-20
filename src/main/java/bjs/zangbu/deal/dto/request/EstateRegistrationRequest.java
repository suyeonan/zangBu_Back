package bjs.zangbu.deal.dto.request;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class EstateRegistrationRequest {

  private Long buildingId;

  //    member table
  private String phone;
  private String birth;


  //    complex_list table
  private String sido;
  private String address;
  private String sigungu;
  private String dong;
  private String ho;
  private String roadName; // 도로명, todo : complexList 테이블에 추가해야함

  public static EstateRegistrationRequest addTwoRequest(AddressRequest addr, MemberRequest member) {
    return EstateRegistrationRequest.builder()
            .buildingId(1L)
            .phone(member.getPhoneNo())
            .birth(member.getMemberId())
            .sido(addr.getSido())
            .address(addr.getAddress())
            .sigungu(addr.getSigungu())
            .dong(addr.getDong())
            .ho(addr.getHo())
            .roadName(addr.getRoadName())
            .build();
  }

}
