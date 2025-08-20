package bjs.zangbu.addressChange.service;

import bjs.zangbu.addressChange.dto.response.ResRegisterCertResponse;

import java.util.List;

public interface AddressChangeService {
    List<ResRegisterCertResponse> generateAddressChange(String memberId) throws Exception;
    boolean hasLivedAtBuilding(String memberId, long buildingId);
    // 로직 테스트용 코드
    List<ResRegisterCertResponse> generateAddressChangeFromRaw(String memberId, String rawJson) throws Exception;
}
