package bjs.zangbu.deal.service;

import bjs.zangbu.codef.converter.CodefConverter;
import bjs.zangbu.codef.service.CodefService;
import bjs.zangbu.codef.service.CodefTwoFactorService;
import bjs.zangbu.deal.dto.join.DealDocumentInfo;
import bjs.zangbu.deal.dto.join.DealWithSaleType;
import bjs.zangbu.deal.dto.request.AddressRequest;
import bjs.zangbu.deal.dto.request.BuildingRegisterRequest;
import bjs.zangbu.deal.dto.request.EstateRegistrationRequest;
import bjs.zangbu.deal.dto.request.MemberRequest;
import bjs.zangbu.deal.dto.response.BuildingRegisterResponse;
import bjs.zangbu.deal.dto.response.DealResponse;
import bjs.zangbu.deal.dto.response.EstateRegistrationResponse;
import bjs.zangbu.deal.mapper.DealMapper;
import bjs.zangbu.deal.util.PdfUtil;
import bjs.zangbu.deal.vo.DocumentType;
import bjs.zangbu.documentReport.service.DocumentToMongoService;
import bjs.zangbu.ncp.service.BinaryUploaderService;
import bjs.zangbu.notification.vo.SaleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static bjs.zangbu.ncp.auth.Holder.HeaderCreationHolder.BUCKET_NAME;

@Log4j2
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {
    private final DealMapper dealMapper;
    private final CodefService codefService;
    private final CodefTwoFactorService codefTwoFactorService;
    private final BinaryUploaderService binaryUploaderService;
    private final DocumentToMongoService documentToMongoService;


    @Override
    @Transactional(readOnly = true)
    /** dealId → "/contracts/xxx.pdf" 상대 경로 반환 */
    public String getContractPdf(Long dealId) {
        DealWithSaleType deal = dealMapper.findWithType(dealId);

        if(deal == null) {
            throw new IllegalArgumentException("해당 거래가 존재하지 않습니다 dealId: "+ dealId);
        }

        String path;
        SaleType type = deal.getSaleType(); // 매매 전월세 타입 분류

        if(type == SaleType.TRADING){ // 매매
            path = "/contracts/sale_contract.pdf";
        } else if (type == SaleType.MONTHLY || type == SaleType.CHARTER) {
            // 전월세
            path = "/contracts/lease_contract.pdf";
        } else{
            throw new IllegalStateException("거래 타입 오류, saleType : " + type);
        }
        return path;
    }

    // 등기부등본 발급 api
    @Override
    public DealResponse.Download getEstateRegisterPdf(String memberId,Long buildingId)
            throws Exception {

        final DocumentType docType = DocumentType.ESTATE;

        //DB에서 데이터 가져와서 request 생성
        //EstateRegistrationRequest request = dealMapper.getEstateRegistrationRequest(buildingId);
        AddressRequest addr = dealMapper.getAddressRequest(buildingId);
        MemberRequest member = dealMapper.getMemberRequest(memberId);
        EstateRegistrationRequest request = EstateRegistrationRequest.addTwoRequest(addr, member);
        // codef에서 응답 가져오기
        String rawResponse = codefService.realEstateRegistrationLeader(request);
        // pdf dto 파싱로직
        EstateRegistrationResponse dto = CodefConverter.parseDataToDto(
                rawResponse, EstateRegistrationResponse.class);

        // PDF 바이트 추출
        byte[] pdfBytes = PdfUtil.decodePdfBytes(dto.getResOriGinalData());
        if (pdfBytes == null || pdfBytes.length == 0) {
            log.warn("PDF bytes empty: buildingId={}, type={}", buildingId, docType);
            // 필요시: 예외/복구 로직
        }
        // (추가)mongodb에 나머지 json 파싱해서 저장 -> 분석리포트를 위함
        documentToMongoService.saveJson(memberId, buildingId, docType, dto);
        /* 6) ncp 업로드 */
        String key = "estate-Register/" + "/" + memberId + "/" + buildingId + ".pdf";
        String url = binaryUploaderService.putPdfObject(BUCKET_NAME,key,pdfBytes);

        documentToMongoService.updatePdfMeta(memberId, buildingId,
                docType, url, pdfBytes.length, null);

        return new DealResponse.Download(url);
    }
    // 건축물대장 발급 api
    @Override
    public DealResponse.Download getBuildingRegisterPdf(String memberId, Long buildingId) throws Exception {

        final DocumentType docType = DocumentType.BUILDING_REGISTER;
        // 1) DB 조회
//        DealDocumentInfo deal = dealMapper.getDocumentInfo(buildingId);
        // request json 형식에 맞게 파싱
//        BuildingRegisterRequest request = BuildingRegisterRequest.from(deal);
        AddressRequest addr = dealMapper.getAddressRequest(buildingId);
        MemberRequest member = dealMapper.getMemberRequest(memberId);
        BuildingRegisterRequest request = BuildingRegisterRequest.addTwoRequest(addr, member);
        System.out.println("request = " + request);
        // 1차·2차가 섞여 있을 수 있는 응답(rawResponse)
        String rawResponse = codefTwoFactorService.generalBuildingLeader(request);
        System.out.println("rawResponse = " + rawResponse);
        // dto로 파싱
        BuildingRegisterResponse dto =
                CodefConverter.parseDataToDto(rawResponse, BuildingRegisterResponse.class);

        // PDF 바이트 추출
        byte[] pdfBytes = PdfUtil.decodePdfBytes(dto.getResOriGinalData());
        if (pdfBytes == null || pdfBytes.length == 0) {
            log.warn("PDF bytes empty: buildingId={}, type={}", buildingId, docType);
            // 필요시: 예외/복구 로직
        }
        // (추가)mongodb에 나머지 json 파싱해서 저장 -> 분석리포트를 위함
        documentToMongoService.saveJson(memberId, buildingId, docType, dto);
        /* ncp 업로드*/
        String key  = "building-register/" + "/" + memberId + "/" + buildingId + ".pdf";
        String url = binaryUploaderService.putPdfObject(BUCKET_NAME,key,pdfBytes);

        documentToMongoService.updatePdfMeta(memberId, buildingId,
                docType, url, pdfBytes.length, null);

        return new DealResponse.Download(url);
    }
}
