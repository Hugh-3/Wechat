package com.linliquan.service;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import com.linliquan.repository.UserRepository;
import com.linliquan.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Result<Map<String, Object>> login(String phone) {
        String phoneHash = sha256(phone);

        User user = userRepository.findByPhoneHash(phoneHash).orElse(null);

        if (user == null) {
            user = register(phone);
        }

        String accessToken = JwtUtil.generateToken(user.getId(), phoneHash);

        Map<String, Object> result = new HashMap<>();
        result.put("user", buildUserInfo(user));
        result.put("accessToken", accessToken);

        return Result.success(result);
    }

    @Transactional
    public User register(String phone) {
        User user = new User();
        user.setPhoneHash(sha256(phone));
        user.setPhoneEncrypted(encryptPhone(phone));
        user.setNickname("业主" + phone.substring(7));
        user.setVerificationStatus(VerificationStatus.UNAUTH);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Transactional
    public Result<Void> applyVerification(Long userId, String idCard, String houseNumber, String certificateUrl) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        if (user.getVerificationStatus() == VerificationStatus.VERIFIED) {
            return Result.fail(ResultCode.CONFLICT);
        }

        user.setIdCardEncrypted(encryptIdCard(idCard));
        user.setHouseNumberEncrypted(encryptHouseNumber(houseNumber));
        user.setCertificateUrl(certificateUrl);
        user.setVerificationStatus(VerificationStatus.PENDING);
        user.setVerificationApplyTime(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        return Result.success(null);
    }

    public Result<Map<String, Object>> getUserStatus(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        Map<String, Object> status = new HashMap<>();
        status.put("verificationStatus", user.getVerificationStatus().getCode());
        status.put("description", user.getVerificationStatus().getDescription());
        status.put("canWrite", user.getVerificationStatus().canWrite());
        status.put("canRead", user.getVerificationStatus().canRead());

        return Result.success(status);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public User getUserByToken(String token) {
        try {
            Long userId = JwtUtil.getUserIdFromToken(token);
            return userRepository.findById(userId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("nickname", user.getNickname());
        info.put("avatarUrl", user.getAvatarUrl());
        info.put("verificationStatus", user.getVerificationStatus().getCode());
        info.put("verificationDesc", user.getVerificationStatus().getDescription());
        return info;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String encryptPhone(String phone) {
        return phone;
    }

    private String encryptIdCard(String idCard) {
        return idCard;
    }

    private String encryptHouseNumber(String houseNumber) {
        return houseNumber;
    }
}
