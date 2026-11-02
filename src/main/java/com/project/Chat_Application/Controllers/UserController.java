package com.project.Chat_Application.Controllers;

import com.project.Chat_Application.Dto.Request.UsersRequestDto;
import com.project.Chat_Application.Dto.Response.PagedResponseDto;
import com.project.Chat_Application.Dto.Response.UsersResponseDto;
import com.project.Chat_Application.Security.CustomUserDetails;
import com.project.Chat_Application.Service.Interfaces.UserInterface;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    final private UserInterface userInterface;

    public UserController(UserInterface userInterface) {
        this.userInterface = userInterface;
    }

    @PostMapping
    public ResponseEntity<UsersResponseDto> createUser(@Valid @RequestBody UsersRequestDto dto){
        return new ResponseEntity<>(userInterface.createUser(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<PagedResponseDto<UsersResponseDto>> getAllUsers(Pageable pageable){
        return ResponseEntity.ok(userInterface.getAllUsers(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsersResponseDto> getByUserId(@PathVariable Long id){
        return ResponseEntity.ok(userInterface.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable Long id, @Valid @RequestBody UsersRequestDto dto,@AuthenticationPrincipal CustomUserDetails currUser){
        Long currUserId = currUser.getId();
        boolean responseDto = userInterface.updateUser(id, dto,currUserId);
        if(responseDto){
            return ResponseEntity.ok("Updated Successfully!");
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currUser){
        Long currUserId = currUser.getId();
        boolean response = userInterface.deleteUser(id,currUserId);
        if(response){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}