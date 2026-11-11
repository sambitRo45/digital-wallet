package com.digitalwallet.service.impl;

import com.digitalwallet.dto.UpdateProfileRequest;
import com.digitalwallet.dto.UserProfileResponse;
import com.digitalwallet.entity.User;
import com.digitalwallet.repository.UserRepository;
import com.digitalwallet.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String email) {

        User user = findUser(email);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {

        User user = findUser(email);
        user.setFullName(request.fullName().trim());

        User savedUser = userRepository.save(user);
        return toResponse(savedUser);
    }

    private User findUser(String email) {

        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User Not Found"
                ));
    }

    private UserProfileResponse toResponse(User user) {

        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus(),
                user.getCreatedAt()
        );
    }
}
