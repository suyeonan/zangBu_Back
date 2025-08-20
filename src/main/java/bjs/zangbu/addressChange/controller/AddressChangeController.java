package bjs.zangbu.addressChange.controller;


import bjs.zangbu.addressChange.dto.response.ResRegisterCertResponse;
import bjs.zangbu.addressChange.mapper.AddressChangeMapper;
import bjs.zangbu.addressChange.service.AddressChangeService;

import java.util.List;

import bjs.zangbu.security.account.vo.CustomUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/address-changes", produces = MediaType.APPLICATION_JSON_VALUE)
//@Api(tags = "addressChange API", description = "초본API를 활용한 주소 내역 API")
public class AddressChangeController {

    private final AddressChangeService addressChangeService;
    private final AddressChangeMapper addressChangeMapper;

    /**
     * 초본을 조회하고 전입 이력을 저장한 뒤, 저장된 항목들을 반환한다. 호출 예) POST
     * /api/address-changes/import?memberId=USER-UUID
     */
    @PostMapping("/import")
    public ResponseEntity<?> importFromResidentAbstract(
            @AuthenticationPrincipal CustomUser customUser) {

        String memberId = customUser.getMember().getMemberId();

        // 간단 유효성 검증
        if (memberId == null || memberId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (addressChangeMapper.existsByMemberId(memberId) > 0) {
            return ResponseEntity.ok("true");
        }
        try {
            List<ResRegisterCertResponse> items =
                    addressChangeService.generateAddressChange(memberId);

            // 비즈니스 정책에 따라, 결과가 비어도 200 OK로 리스트 반환
            return ResponseEntity.ok("true");

        } catch (Exception e) {
            log.error("importFromResidentAbstract failed. memberId={}, err={}",
                    memberId, e.toString(), e);
            // 운영 정책에 맞게 상태코드/메시지 결정(여기서는 500)
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/find/{buildingId}")
    public ResponseEntity<?> findAddressChange(
            @AuthenticationPrincipal CustomUser customUser, @PathVariable long buildingId) {
        String memberId = customUser.getMember().getMemberId();
        boolean lived = addressChangeService.hasLivedAtBuilding(memberId, buildingId);
        return ResponseEntity.ok(lived);
    }
}
