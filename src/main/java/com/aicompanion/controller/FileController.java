package com.aicompanion.controller;

import com.aicompanion.common.response.Result;
import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Slf4j
@Tag(name = "文件管理", description = "文件上传相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class FileController {

    private final UserService userService;

    // 头像存储目录
    private static final String AVATAR_UPLOAD_DIR = "uploads/avatars/";

    // 允许的文件类型
    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    // 最大文件大小 2MB
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    /**
     * 上传头像
     */
    @Operation(summary = "上传头像", description = "上传用户头像，支持 jpg/png/gif/webp，最大 2MB")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                       HttpServletRequest request) {
        Long userId = SecurityUtil.getCurrentUserId(request);

        // 1. 校验文件
        if (file.isEmpty()) {
            return Result.fail(400, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.fail(400, "文件大小不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedType(contentType)) {
            return Result.fail(400, "只支持 jpg/png/gif/webp 格式");
        }

        // 2. 确保目录存在
        File uploadDir = new File(AVATAR_UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 3. 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;

        // 4. 保存文件
        File destFile = new File(uploadDir, fileName);
        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            log.error("头像上传失败: userId={}", userId, e);
            return Result.fail(500, "文件保存失败");
        }

        // 5. 更新数据库
        String avatarUrl = "/api/uploads/avatars/" + fileName;
        userService.updateAvatar(userId, avatarUrl);

        log.info("头像上传成功: userId={}, url={}", userId, avatarUrl);
        return Result.success("头像上传成功", avatarUrl);
    }

    private boolean isAllowedType(String contentType) {
        for (String allowed : ALLOWED_TYPES) {
            if (allowed.equals(contentType)) {
                return true;
            }
        }
        return false;
    }
}
