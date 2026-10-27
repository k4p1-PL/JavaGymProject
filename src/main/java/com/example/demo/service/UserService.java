package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.User;
import com.example.demo.model.enums.Role;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll(Role role) {
        List<User> users = role == null
                ? userRepository.findAllByOrderByLastNameAscFirstNameAsc()
                : userRepository.findAllByRoleOrderByLastNameAscFirstNameAsc(role);
        return users.stream().map(DtoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return DtoMapper.toResponse(getEntity(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Użytkownik z adresem " + email + " już istnieje");
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(req.password()))
                .firstName(req.firstName().trim())
                .lastName(req.lastName().trim())
                .phoneNumber(req.phoneNumber())
                .dateOfBirth(req.dateOfBirth())
                .role(req.role())
                .active(true)
                .build();
        return DtoMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest req, Long currentUserId) {
        User user = getEntity(id);
        if (id.equals(currentUserId) && (!req.active() || req.role() != Role.ADMIN)) {
            throw new BusinessException("Nie możesz zablokować swojego konta ani odebrać sobie roli administratora");
        }
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setPhoneNumber(req.phoneNumber());
        user.setDateOfBirth(req.dateOfBirth());
        user.setRole(req.role());
        user.setActive(req.active());
        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.newPassword()));
        }
        return DtoMapper.toResponse(user);
    }

    /**
     * "Usunięcie" użytkownika to dezaktywacja konta – jego karnety, płatności i wejścia
     * muszą zostać w historii.
     */
    @Transactional
    public void deactivate(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessException("Nie możesz dezaktywować własnego konta");
        }
        getEntity(id).setActive(false);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, ProfileUpdateRequest req) {
        User user = getEntity(userId);
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setPhoneNumber(req.phoneNumber());
        user.setDateOfBirth(req.dateOfBirth());
        return DtoMapper.toResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = getEntity(userId);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Obecne hasło jest niepoprawne");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
    }

    User getEntity(Long id) {
        return userRepository.findById(id).orElseThrow(() -> NotFoundException.of("Użytkownik", id));
    }
}
