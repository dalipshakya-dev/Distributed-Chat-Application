package com.project.Chat_Application.Service.Impl;

import com.project.Chat_Application.Dto.DtoMapper.DtoMapper;
import com.project.Chat_Application.Dto.Response.ConversationParticipantResponseDto;
import com.project.Chat_Application.Dto.Response.ConversationSummaryDto;
import com.project.Chat_Application.Dto.Response.UnReadCountResponseDto;
import com.project.Chat_Application.Dto.Response.UsersResponseDto;
import com.project.Chat_Application.Entities.Conversation;
import com.project.Chat_Application.Entities.ConversationParticipant;
import com.project.Chat_Application.Entities.User;
import com.project.Chat_Application.Exceptions.ResourceNotFoundException;
import com.project.Chat_Application.Repositories.ConversationParticipantRepository;
import com.project.Chat_Application.Repositories.ConversationRepository;
import com.project.Chat_Application.Repositories.MessageRepository;
import com.project.Chat_Application.Repositories.UsersRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationParticipantServiceTest {

    @Mock
    private DtoMapper dtoMapper;
    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private ConversationParticipantService conversationParticipantService;


    @Test
    void isParticipant_returnsTrue_whenParticipantExists(){
        Long userId = 1L;
        Long conversationId = 100L;
        ConversationParticipant participant = new ConversationParticipant();

        when(conversationParticipantRepository.findByUserIdAndConversationId(userId,conversationId)).thenReturn(Optional.of(participant));

        boolean result = conversationParticipantService.isParticipant(userId,conversationId);

        assertTrue(result);

    }

    @Test
    void isParticipant_returnsFalse_whenParticipantDoesNotExist(){
        Long userId = 1L;
        Long conversationId = 100L;

        when(conversationParticipantRepository.findByUserIdAndConversationId(userId,conversationId)).thenReturn(Optional.empty());

        boolean result = conversationParticipantService.isParticipant(userId,conversationId);

        assertFalse(result);
    }

    @Test
    void getUnreadCount_returnsCountFromRedis_whenCacheHit(){
        Long conversationId = 1L;
        Long userId = 10L;
        String key = "unread:" + userId + ":" + conversationId;
        ValueOperations<String,String>valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get(key)).thenReturn("5");

        UnReadCountResponseDto result = conversationParticipantService.getUnreadCount(conversationId,userId);
        assertEquals(5L,result.getUnreadCount());
        assertEquals(conversationId,result.getConversationId());

    }


    @Test
    void getUnreadCount_returnsFullCountFromMySQL_whenCacheMissAndNeverRead(){
        Long conversationId = 1L;
        Long userId = 10L;
        String key = "unread:" + userId + ":" + conversationId;
        ValueOperations<String,String>valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get(key)).thenReturn(null);
        ConversationParticipant conversationParticipant = new ConversationParticipant();
        conversationParticipant.setLastReadAt(null);
        when(messageRepository.countByConversationId(conversationId)).thenReturn(5L);
        when(conversationParticipantRepository.findByUserIdAndConversationId(userId,conversationId)).thenReturn(Optional.of(conversationParticipant));

        UnReadCountResponseDto result = conversationParticipantService.getUnreadCount(conversationId,userId);
        assertEquals(5L,result.getUnreadCount());
        assertEquals(conversationId,result.getConversationId());

    }

    @Test
    void getUnreadCount_returnsCountAfterLastRead_whenCacheMissAndPreviouslyRead(){
        Long conversationId = 1L;
        Long userId = 10L;
        String key = "unread:" + userId + ":" + conversationId;
        ValueOperations<String,String>valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get(key)).thenReturn(null);
        ConversationParticipant conversationParticipant = new ConversationParticipant();
        conversationParticipant.setLastReadAt(LocalDateTime.now());
        when(messageRepository.countByConversationIdAndCreatedAtAfter(conversationId,conversationParticipant.getLastReadAt())).thenReturn(5L);
        when(conversationParticipantRepository.findByUserIdAndConversationId(userId,conversationId)).thenReturn(Optional.of(conversationParticipant));

        UnReadCountResponseDto result = conversationParticipantService.getUnreadCount(conversationId,userId);
        assertEquals(5L,result.getUnreadCount());
        assertEquals(conversationId,result.getConversationId());
    }



    @Test
    void markAsRead_throwsAccessDenied_whenNotParticipant(){
        Long userId = 1L;
        Long conversationId = 100L;
        when(conversationParticipantRepository.findByUserIdAndConversationId(userId,conversationId)).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class,()->conversationParticipantService.markAsRead(conversationId,userId));
    }


    @Test
    void markAsRead_updatesLastReadAndClearsRedisCache_whenAuthorized(){
        Long userId = 1L;
        Long conversationId = 100L;
        String key = "unread:" + userId + ":" + conversationId;
        ConversationParticipant participant = new ConversationParticipant();
        when(conversationParticipantRepository.findByUserIdAndConversationId(userId,conversationId)).thenReturn(Optional.of(participant));
        participant.setLastReadAt(LocalDateTime.now()); //existing

        conversationParticipantService.markAsRead(conversationId,userId);
        verify(conversationParticipantRepository).save(participant);
        verify(stringRedisTemplate).delete(key);

    }

    @Test
    void getByUserId_throwsAccessDenied_whenUserIdMismatch(){
        Long userId = 1L;
        Long currUserId = 2L;
        assertThrows(AccessDeniedException.class,()->conversationParticipantService.getByUserId(userId,currUserId));
    }

    @Test
    void getByUserIdAndConversationId_throwsAccessDenied_whenUserIdMismatch(){
        Long userId = 1L;
        Long currUserId = 2L;
        Long conversationId = 3L;
        assertThrows(AccessDeniedException.class,()->conversationParticipantService.getByUserIdAndConversationId(userId,conversationId,currUserId));
    }


    @Test
    void getByConversationId_throwsAccessDenied_whenNotParticipant(){
        Long conversationId = 1L;
        Long currUserId = 2L;

        when(conversationParticipantRepository.findByUserIdAndConversationId(currUserId,conversationId)).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class,()->conversationParticipantService.getByConversationId(conversationId,currUserId));
    }

    @Test
    void getByUserId_returnsParticipantList_whenUserIdMatches(){
        Long userId = 1L;
        ConversationParticipant participant = new ConversationParticipant();
        when(conversationParticipantRepository.findByUserId(userId)).thenReturn(List.of(participant));

        when(dtoMapper.convertUserToDto(any())).thenReturn(new UsersResponseDto());
        when(dtoMapper.convertConversationSummaryToDto(any())).thenReturn(new ConversationSummaryDto());

        List<ConversationParticipantResponseDto>result = conversationParticipantService.getByUserId(userId,userId);

        assertEquals(1,result.size());
    }

    @Test
    void getByConversationId_returnsParticipantList_whenAuthorized(){
        Long userId = 1L;
        Long conversationId = 2L;
        ConversationParticipant participant = new ConversationParticipant();
        when(conversationParticipantRepository.findByUserIdAndConversationId(userId,conversationId)).thenReturn(Optional.of(participant));

        when(dtoMapper.convertUserToDto(any())).thenReturn(new UsersResponseDto());
        when(dtoMapper.convertConversationSummaryToDto(any())).thenReturn(new ConversationSummaryDto());
        when(conversationParticipantRepository.findByConversationId(conversationId)).thenReturn(List.of(participant));

        List<ConversationParticipantResponseDto>result = conversationParticipantService.getByConversationId(conversationId,userId);

        assertEquals(1,result.size());
    }

    @Test
    void getByUserIdAndConversationId_returnsDto_whenAuthorized(){
        Long userId = 2L;
        Long currUserId = 2L;
        Long conversationId = 3L;

        ConversationParticipant participant = new ConversationParticipant();
        when(conversationParticipantRepository.findByUserIdAndConversationId(userId,conversationId)).thenReturn(Optional.of(participant));
        when(dtoMapper.convertUserToDto(any())).thenReturn(new UsersResponseDto());
        when(dtoMapper.convertConversationSummaryToDto(any())).thenReturn(new ConversationSummaryDto());

        ConversationParticipantResponseDto result = conversationParticipantService.getByUserIdAndConversationId(userId,conversationId,currUserId);

        assertNotNull(result);
    }

    @Test
    void leaveConversation_throwsResourceNotFound_whenParticipantMissing(){
        Long participantId = 1L;
        Long userId = 2L;

        when(conversationParticipantRepository.findById(participantId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->conversationParticipantService.leaveConversation(participantId,userId));

    }

    @Test
    void leaveConversation_throwsAccessDenied_whenNotOwnParticipation(){
        Long participantId = 1L;
        Long userId = 2L;
        ConversationParticipant participant = new ConversationParticipant();
        User user = new User();
        user.setId(4L);
        participant.setUser(user);
        when(conversationParticipantRepository.findById(participantId)).thenReturn(Optional.of(participant));

        assertThrows(AccessDeniedException.class,()->conversationParticipantService.leaveConversation(participantId,userId));
    }

    @Test
    void leaveConversation_deletesAndReturnsTrue_whenAuthorized(){
        Long participantId = 1L;
        Long currUserId = 2L;
        ConversationParticipant participant = new ConversationParticipant();
        User user = new User();
        user.setId(2L);
        participant.setUser(user);
        when(conversationParticipantRepository.findById(participantId)).thenReturn(Optional.of(participant));

        boolean result = conversationParticipantService.leaveConversation(participantId,currUserId);
        assertTrue(result);

    }


    @Test
    void createParticipant_throwsResourceNotFound_whenConversationMissing(){
        Long conversationId = 2L;
        Long userId = 3L;
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->conversationParticipantService.createParticipant(userId,conversationId));

    }

    @Test
    void createParticipant_throwsResourceNotFound_whenUserMissing(){
        Long conversationId = 2L;
        Long userId = 3L;
        Conversation conversation = new Conversation();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(usersRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->conversationParticipantService.createParticipant(userId,conversationId));
    }

    @Test
    void createParticipant_returnsDto_whenSuccessful(){
        Long conversationId = 2L;
        Long userId = 3L;
        Conversation conversation = new Conversation();
        User user = new User();
        ConversationParticipant savedParticipant = new ConversationParticipant();
        savedParticipant.setId(1L);
        savedParticipant.setJoinedAt(LocalDateTime.now());
        savedParticipant.setUser(user);
        savedParticipant.setConversation(conversation);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
        when(dtoMapper.convertUserToDto(any())).thenReturn(new UsersResponseDto());
        when(dtoMapper.convertConversationSummaryToDto(any())).thenReturn(new ConversationSummaryDto());
        when(conversationParticipantRepository.save(any())).thenReturn(savedParticipant);

        ConversationParticipantResponseDto result = conversationParticipantService.createParticipant(userId,conversationId);
        assertNotNull(result);

    }


    @Test
    void getParticipantUserIds_returnsUserIds_forConversation(){
        Long conversationId = 2L;
        ConversationParticipant conversationParticipant = new ConversationParticipant();
        User user = new User();
        user.setId(1L);
        conversationParticipant.setUser(user);

        when(conversationParticipantRepository.findByConversationId(conversationId)).thenReturn(List.of(conversationParticipant));
        List<Long> result = conversationParticipantService.getParticipantUserIds(conversationId);
        assertEquals(1,result.size());
        assertEquals(1L, result.get(0));

    }

    @Test
    void getMySQLUnreadCount_returnsZero_whenNoParticipantRecord(){
        Long userId = 1L;
        Long conversationId = 2L;

        when(conversationParticipantRepository.findByUserIdAndConversationId(userId,conversationId)).thenReturn(Optional.empty());
        long result = conversationParticipantService.getMySQLUnreadCount(conversationId,userId);
        assertEquals(0,result);
    }

    @Test
    void getMySQLUnreadCount_returnsFullCount_whenNeverRead(){
        Long userId = 1L;
        Long conversationId = 2L;
        ConversationParticipant participant = new ConversationParticipant();
        participant.setLastReadAt(null);
        when(conversationParticipantRepository.findByUserIdAndConversationId(userId,conversationId)).thenReturn(Optional.of(participant));
        when(messageRepository.countByConversationId(conversationId)).thenReturn(5L);
        long result = conversationParticipantService.getMySQLUnreadCount(conversationId,userId);
        assertEquals(5L,result);
    }

    @Test
    void getMySQLUnreadCount_returnsCountAfterLastRead_whenPreviouslyRead(){
        Long userId = 1L;
        Long conversationId = 2L;
        ConversationParticipant participant = new ConversationParticipant();
        participant.setLastReadAt(LocalDateTime.now());
        when(conversationParticipantRepository.findByUserIdAndConversationId(userId,conversationId)).thenReturn(Optional.of(participant));
        when(messageRepository.countByConversationIdAndCreatedAtAfter(conversationId,participant.getLastReadAt())).thenReturn(5L);
        long result = conversationParticipantService.getMySQLUnreadCount(conversationId,userId);
        assertEquals(5L,result);
    }
}
