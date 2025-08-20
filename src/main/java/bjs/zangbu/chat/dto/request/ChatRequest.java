package bjs.zangbu.chat.dto.request;

import bjs.zangbu.chat.vo.ChatMessage;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatRequest {

  //   /chat/room/{roomId}?lastMessageId={lastMessageId}&limit={limit}
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @ApiModel(description = "채팅방 메시지 목록 요청 DTO")
  public static class ChatMessageListRequest { //roomId에 해당하는 채팅방의 메시지들 마지막부터 limit개 불러오기

    @ApiModelProperty(value = "마지막으로 받은 메시지 ID")
    private Long lastMessageId;  // 마지막으로 받은 메시지 ID
    @ApiModelProperty(value = "한 번에 불러올 메시지 수")
    private int limit;      // 한 번에 불러올 개수
    //컨트롤러 작성해보고 dto작성 없이 @RequestParam으로 할지 생각해보기
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @ApiModel(description = "메시지 전송 요청 DTO")
  public static class SendMessageRequest {

    @ApiModelProperty(value = "전송할 메시지 내용", example = "안녕", required = true)
    @JsonProperty("message")
    private String message;
    @ApiModelProperty(value = "채팅방 ID", example = "b10011-1111-2222-3333-444455556675", required = true)
    @JsonProperty("chatRoomId")
    private String chatRoomId;
    @ApiModelProperty(value = "보낸 사용자 ID", required = true)
    @JsonProperty("senderId")
    private String senderId;

    public ChatMessage toEntity(LocalDateTime createdAt) {
      return ChatMessage.builder()
              .chatRoomId(chatRoomId)
              .senderId(senderId)
              .message(message)
              .createdAt(createdAt)
              .build();
    }
  }

  //  /chat/list?page={page}&size={size}&type={type}
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @ApiModel(description = "채팅방 목록 요청 DTO")
  public class ChatRoomListRequest {


    @ApiModelProperty(value = "페이지 번호 (0부터 시작)", example = "0")
    private int page;       //페이지

    @ApiModelProperty(value = "한 페이지에 보여줄 채팅방 개수", example = "10")
    private int size;       //한 페이지에 보여줄 채팅방 개수

    @ApiModelProperty(
            value = "필터 타입",
            allowableValues = "ALL,BUY,SELL"
    )
    private String type; // "전체" or "구매" or "판매"
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  // POST  /chat/room 채팅방 생성
  @ApiModel(description = "채팅방 생성 요청 DTO")
  public static class CreateRoomRequest {
    @ApiModelProperty(value = "매물 ID", example = "123", required = true)
    private Long buildingId;
  }
}
