package bjs.zangbu.addressChange.mapper;

import bjs.zangbu.addressChange.dto.request.ResRegisterCertRequest;
import bjs.zangbu.addressChange.vo.AddressChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AddressChangeMapper {
    ResRegisterCertRequest getRegisterCertRequest(String memberId);

    /** 단건 insert. 성공 시 row.addressChangeId 에 PK가 생성됨. */
    int insert(AddressChange row);

    /** 중복 방지 체크: 동일 사용자 + 동일 주소 + 동일 전입일 존재 여부 */
    int existsByMemberAddrDate(@Param("memberId") String memberId,
            @Param("resUserAddr") String resUserAddr,
            @Param("resMoveInDate") LocalDateTime resMoveInDate);

    /**
     * 사용자의 주민등록초본에 기록된 모든 주소를 조회합니다.
     *
     * @param memberId 사용자 ID
     * @return 주소 목록
     */
    List<String> selectUserAddresses(String memberId);

    int existsByMemberId(String memberId);

    /** 멤버가 특정 단지의 zonecode에 거주한 적이 있는지 확인 */
    int countLivedByMemberAndComplexZone(
            @Param("memberId") String memberId,
            @Param("complexId") long complexId
    );
}
