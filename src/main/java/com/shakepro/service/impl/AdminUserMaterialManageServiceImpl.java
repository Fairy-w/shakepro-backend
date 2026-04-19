package com.shakepro.service.impl;

import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.dto.response.UserMaterialResponse;
import com.shakepro.repository.UserRepository;
import com.shakepro.service.AdminUserMaterialManageService;
import com.shakepro.service.UserMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserMaterialManageServiceImpl implements AdminUserMaterialManageService {

    private final UserRepository userRepository;
    private final UserMaterialService userMaterialService;

    @Override
    public List<UserMaterialResponse> list(Long userId, String keyword, String categoryId) {
        ensureUserExists(userId);
        return userMaterialService.list(userId, keyword, categoryId);
    }

    private void ensureUserExists(Long userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }
}
