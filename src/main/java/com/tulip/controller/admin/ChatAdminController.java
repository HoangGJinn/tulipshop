package com.tulip.controller.admin;

import com.tulip.dto.ChatRoomDTO;
import com.tulip.repository.UserRepository;
import com.tulip.service.ChatService;
import com.tulip.service.impl.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dashboard nhân viên để quản lý chat
 * 
 * Phân luồng ưu tiên:
 * - Thời gian chờ
 * - Khách VIP (đã từng mua hàng)
 * - Đơn hàng đang pending
 */
@Slf4j
@Controller
@RequestMapping("/admin/chat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ChatAdminController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    /**
     * Dashboard nhân viên
     */
    @GetMapping
    public String dashboard(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        var staff = userRepository.findById(userDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Lấy danh sách phòng đang chờ
        List<ChatRoomDTO> waitingRooms = chatService.getWaitingChatRooms();
        
        // Lấy danh sách phòng đã được gán cho nhân viên này
        List<ChatRoomDTO> assignedRooms = chatService.getStaffChatRooms(staff);
        
        model.addAttribute("waitingRooms", waitingRooms);
        model.addAttribute("assignedRooms", assignedRooms);
        model.addAttribute("pageTitle", "Quản lý Chat");
        model.addAttribute("currentPage", "chat");
        
        return "admin/chat/dashboard";
    }

    /**
     * API: Nhận phòng chat
     */
    @PostMapping("/rooms/{roomId}/assign")
    @ResponseBody
    public ChatRoomDTO assignRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var staff = userRepository.findById(userDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        var room = chatService.assignChatRoom(roomId, staff);
        return chatService.getChatRoomDTO(room.getId(), staff);
    }

    /**
     * API: Đóng phòng chat
     */
    @PostMapping("/rooms/{roomId}/close")
    @ResponseBody
    public ChatRoomDTO closeRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var user = userRepository.findById(userDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        var room = chatService.closeChatRoom(roomId, user);
        return chatService.getChatRoomDTO(room.getId(), user);
    }
}

