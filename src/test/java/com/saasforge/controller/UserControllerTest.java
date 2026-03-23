package com.saasforge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasforge.dto.PageResponse;
import com.saasforge.dto.UpdateUserRequest;
import com.saasforge.dto.UserResponse;
import com.saasforge.exception.GlobalExceptionHandler;
import com.saasforge.exception.ResourceNotFoundException;
import com.saasforge.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UserResponse sampleUser(Long id, String name, String email) {
        return new UserResponse(id, name, email, "ACTIVE", 1L, "ADMIN");
    }

    private PageResponse<UserResponse> samplePage(UserResponse... users) {
        return new PageResponse<>(new PageImpl<>(List.of(users), PageRequest.of(0, 10), users.length));
    }


    @Test
    void getAllUsers_returns200WithPagedContent() throws Exception {
        when(userService.getAllUsers(anyInt(), anyInt(), anyString())).thenReturn(samplePage(
                sampleUser(1L, "Alice", "alice@test.com"),
                sampleUser(2L, "Bob", "bob@test.com")
        ));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].email").value("alice@test.com"))
                .andExpect(jsonPath("$.content[1].email").value("bob@test.com"));
    }

    @Test
    void getAllUsers_returnsEmptyContent() throws Exception {
        when(userService.getAllUsers(anyInt(), anyInt(), anyString())).thenReturn(samplePage());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAllUsers_returnsPageMetadata() throws Exception {
        when(userService.getAllUsers(anyInt(), anyInt(), anyString())).thenReturn(samplePage(
                sampleUser(1L, "Alice", "alice@test.com")
        ));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getAllUsers_forwardsPageParams() throws Exception {
        when(userService.getAllUsers(eq(2), eq(5), eq("name"))).thenReturn(samplePage());

        mockMvc.perform(get("/api/users")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sortBy", "name"))
                .andExpect(status().isOk());

        verify(userService).getAllUsers(2, 5, "name");
    }


    @Test
    void getUserById_found_returns200() throws Exception {
        when(userService.getUserById(1L)).thenReturn(sampleUser(1L, "Alice", "alice@test.com"));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roleName").value("ADMIN"))
                .andExpect(jsonPath("$.tenantId").value(1));
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found with id: 99"));
    }


    @Test
    void updateUser_validRequest_returns200() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Updated Name");
        request.setStatus("INACTIVE");

        when(userService.updateUser(eq(1L), any())).thenReturn(
                sampleUser(1L, "Updated Name", "alice@test.com")
        );

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void updateUser_blankName_returns400() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("");
        request.setStatus("ACTIVE");

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_blankStatus_returns400() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Alice");
        request.setStatus("");

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_nullName_returns400() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setStatus("ACTIVE");

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_notFound_returns404() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Alice");
        request.setStatus("ACTIVE");

        when(userService.updateUser(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(put("/api/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found with id: 99"));
    }


    @Test
    void deleteUser_success_returns204() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    void deleteUser_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 99"))
                .when(userService).deleteUser(99L);

        mockMvc.perform(delete("/api/users/99"))
                .andExpect(status().isNotFound());
    }
}
