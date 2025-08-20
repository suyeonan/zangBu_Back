package bjs.zangbu.chat.controller;

import bjs.zangbu.chat.dto.request.ChatRequest;
import bjs.zangbu.chat.dto.response.ChatResponse;
import bjs.zangbu.chat.service.ChatService;
import bjs.zangbu.chat.vo.ChatMessage;
import bjs.zangbu.chat.vo.ChatRoom;
import bjs.zangbu.fcm.mapper.FcmMapper;
import bjs.zangbu.fcm.service.FcmService;
import bjs.zangbu.member.mapper.MemberMapper;
import bjs.zangbu.member.service.MemberService;
import bjs.zangbu.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Log4j2
@Controller
@RequiredArgsConstructor
public class ChatStompController {  //WebSocket 메시지 수신 컨트롤러

    private final ChatService chatService;
    private final MemberService memberService;
    private final NotificationService notificationService;
    //서버에서 클라이언트로 STOMP 메시지를 보낼 때 사용하는 도구
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send/{roomId}")
    public void handleSendMessage(
            //@MessageMapping 경로의 {roomId} 부분
            //@DestinationVariable은 @PathVariable의 WebSocket(STOMP) 버전
            @DestinationVariable String roomId,
            //클라이언트가 보낸 채팅 메시지(JSON 형식) -> @Payload로 역직렬화됨.
            //@Payload는 메시지 본문 추출
            @Payload ChatRequest.SendMessageRequest request
    ) {

        try {
            log.info("🚀 메시지 수신 시작");
            log.info("roomId: {}", roomId);
            log.info("request: {}", request);
            log.info("message: {}", request.getMessage());
            log.info("chatRoomId: {}", request.getChatRoomId());
            log.info("senderId: {}", request.getSenderId());

            String senderId = request.getSenderId();

            log.info("💾 메시지 저장 시작");
            ChatResponse.SendMessageResponse response = chatService.sendMessage(senderId, roomId, request);
            log.info("💾 메시지 저장 완료: {}", response);

            log.info("📢 브로드캐스트 시작");
            String destination = "/topic/chat." + roomId;
            log.info("📢 브로드캐스트 목적지: {}", destination);

            messagingTemplate.convertAndSend(destination, response);
            log.info("✅ 브로드캐스트 완료");

        } catch (Exception e) {
            log.error("❌ 메시지 처리 중 에러: ", e);
            throw e;
        }
    }
        /*
        String senderId = request.getSenderId();
        log.info("senderId(member_id): " + senderId);

        if (senderId == null || senderId.isEmpty()) {
            throw new IllegalArgumentException("senderId가 없습니다.");
        }
        // 받은 메시지 DB에 저장 -> 클라이언트에게 보낼 메시지 형태인 응답 DTO(SendMessageResponse)로 가공
        ChatResponse.SendMessageResponse response = chatService.sendMessage(senderId, roomId, request);

        //알림 메소드 추후에 추가
        //chatRoomId : @DestinationVariable String roomId
        //message : request.getMessage()

        //receiverId : (chatRoomId를 구독하고 있는 사용자 id)
        ChatRoom chatRoom = chatService.getChatRoomDetail(roomId);
        String receiverId;
        if(senderId.equals(chatRoom.getConsumerId())) { //보낸 사람이 구매자일 경우
            //chatRoom의 판매자 닉네임으로 판매자 id 구함
            receiverId = chatService.getUserIdByNickname(chatRoom.getSellerNickname());
        } else {  //보낸 사람이 판매자일 경우
            //chatRoom의 구매자 id 구함
            receiverId = chatRoom.getConsumerId();
        }

        notificationService.sendChatNotification(receiverId, roomId, request.getMessage());


        // 채팅방 구독자(/topic/chat.room.{roomId}를 구독하고 있는 모든 클라이언트)에게 메시지 브로드캐스트(response를 실시간 전송)
        messagingTemplate.convertAndSend("/topic/chat." + roomId, response);

         */
    }

}

