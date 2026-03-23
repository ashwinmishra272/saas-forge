package com.saasforge.service;

import com.saasforge.dto.PageResponse;
import com.saasforge.dto.UpdateUserRequest;
import com.saasforge.dto.UserResponse;
import com.saasforge.entity.SystemRole;
import com.saasforge.entity.Tenant;
import com.saasforge.entity.User;
import com.saasforge.exception.ResourceNotFoundException;
import com.saasforge.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User buildUser(Long id, String name, String email) {
        Tenant tenant = new Tenant();
        tenant.setId(1L);

        SystemRole role = new SystemRole();
        role.setName("ADMIN");

        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setStatus("ACTIVE");
        user.setTenant(tenant);
        user.setRole(role);
        return user;
    }

    // ── getAllUsers ────────────────────────────────────────────────────────────

    @Test
    void getAllUsers_returnsPaginatedResponse() {
        Page<User> userPage = new PageImpl<>(List.of(
                buildUser(1L, "Alice", "alice@test.com"),
                buildUser(2L, "Bob", "bob@test.com")
        ));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        PageResponse<UserResponse> result = userService.getAllUsers(0, 10, "id");

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice@test.com");
        assertThat(result.getContent().get(1).getEmail()).isEqualTo("bob@test.com");
    }

    @Test
    void getAllUsers_returnsEmptyPage_whenNoUsers() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        PageResponse<UserResponse> result = userService.getAllUsers(0, 10, "id");

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getAllUsers_passesCorrectPageableToRepository() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        userService.getAllUsers(2, 5, "name");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().getOrderFor("name")).isNotNull();
    }

    @Test
    void getAllUsers_returnsPaginationMetadata() {
        List<User> users = List.of(buildUser(1L, "Alice", "alice@test.com"));
        Page<User> userPage = new PageImpl<>(users, org.springframework.data.domain.PageRequest.of(0, 5), 1);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        PageResponse<UserResponse> result = userService.getAllUsers(0, 5, "id");

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getPage()).isZero();
    }

    // ── getUserById ───────────────────────────────────────────────────────────

    @Test
    void getUserById_found_returnsUserResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, "Alice", "alice@test.com")));

        UserResponse response = userService.getUserById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getEmail()).isEqualTo("alice@test.com");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getRoleName()).isEqualTo("ADMIN");
        assertThat(response.getTenantId()).isEqualTo(1L);
    }

    @Test
    void getUserById_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── updateUser ────────────────────────────────────────────────────────────

    @Test
    void updateUser_success_updatesNameAndStatus() {
        User existing = buildUser(1L, "Old Name", "user@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("New Name");
        request.setStatus("INACTIVE");

        UserResponse response = userService.updateUser(1L, request);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void updateUser_doesNotModifyEmailOrTenantOrRole() {
        User existing = buildUser(1L, "Name", "original@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Name");
        request.setStatus("ACTIVE");

        UserResponse response = userService.updateUser(1L, request);

        assertThat(response.getEmail()).isEqualTo("original@test.com");
        assertThat(response.getTenantId()).isEqualTo(1L);
        assertThat(response.getRoleName()).isEqualTo("ADMIN");
    }

    @Test
    void updateUser_setsUpdatedAt() {
        User existing = buildUser(1L, "Name", "user@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Name");
        request.setStatus("ACTIVE");

        userService.updateUser(1L, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void updateUser_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Name");
        request.setStatus("ACTIVE");

        assertThatThrownBy(() -> userService.updateUser(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    void deleteUser_success_callsDelete() {
        User user = buildUser(1L, "Alice", "alice@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteUser_notFound_doesNotCallDelete() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).delete(any(User.class));
    }
}
