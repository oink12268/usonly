package com.evho.usonly.domain.chat.service;

import com.evho.usonly.domain.chat.dto.ChatMessage;
import com.evho.usonly.domain.chat.entity.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import com.evho.usonly.domain.couple.entity.Couple;
import com.evho.usonly.domain.couple.repository.CoupleRepository;
import com.evho.usonly.domain.member.dto.MemberCacheDto;
import com.evho.usonly.domain.member.service.MemberService;
import com.evho.usonly.global.exception.CustomException;
import com.evho.usonly.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final MemberService memberService;
    private final CoupleRepository coupleRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public Chat save(ChatMessage chatDto) {
        Couple couple = resolveCouple(chatDto.getWriterUid());

        Chat chat = Chat.builder()
                .message(chatDto.getMessage())
                .writerUid(chatDto.getWriterUid())
                .sendTime(chatDto.getSendTime())
                .replyToId(chatDto.getReplyToId())
                .replyToMessage(chatDto.getReplyToMessage())
                .replyToUid(chatDto.getReplyToUid())
                .couple(couple)
                .build();

        Chat saved = chatRepository.save(chat);
        evictAiSearchCache(couple.getId());
        return saved;
    }

    // writerUid로 발신자의 커플을 찾아 채팅에 태깅. 커플이 없으면 저장 자체를 막음
    // (예전엔 null로 조용히 저장해서 양쪽 파트너 모두에게 영구적으로 안 보이는 메시지가 됐음).
    // MemberService의 캐시된 조회를 써서 ChatMessageSender와 동일 요청 내 중복 DB 조회를 피하고,
    // Couple은 getReferenceById로 프록시만 얻어 FK 세팅용으로만 쓰고 추가 SELECT를 발생시키지 않음.
    private Couple resolveCouple(String writerUid) {
        MemberCacheDto member = memberService.findByProviderId(writerUid);
        if (member == null || member.getCoupleId() == null) {
            throw new CustomException(ErrorCode.COUPLE_REQUIRED);
        }
        return coupleRepository.getReferenceById(member.getCoupleId());
    }

    // 이 커플의 AI 검색 캐시만 지움 (allEntries=true로 다른 커플 캐시까지 같이 날리던 문제 수정)
    private void evictAiSearchCache(Long coupleId) {
        Set<String> keys = redisTemplate.keys("aiSearch::" + coupleId + "_*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
