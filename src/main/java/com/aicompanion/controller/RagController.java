package com.aicompanion.controller;

import com.aicompanion.common.response.Result;
import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.config.WebMvcConfig;
import com.aicompanion.model.entity.KnowledgeFile;
import com.aicompanion.service.KnowledgeFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * RAG 知识库控制器
 *
 * 写侧：文件上传 → Tika 读取 → 切分 → 向量化 → Redis VectorStore
 * 读侧：基于 QuestionAnswerAdvisor 自动检索 + DeepSeek 生成回答
 */
@Slf4j
@Tag(name = "RAG 知识库", description = "知识库文件管理与问答接口")
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final KnowledgeFileService knowledgeFileService;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;
    private final WebMvcConfig webMvcConfig;
    private final ChatClient.Builder chatClientBuilder;

    /**
     * 上传文件到知识库
     */
    @Operation(summary = "上传知识库文件", description = "支持 PDF/Word/TXT/HTML 等格式，最大 50MB")
    @PostMapping("/upload")
    public Result<KnowledgeFile> upload(@RequestParam("file") MultipartFile file,
                                         HttpServletRequest request) {
        Long userId = SecurityUtil.getCurrentUserId(request);

        if (file.isEmpty()) {
            return Result.fail(400, "文件不能为空");
        }

        try {
            String uploadBasePath = webMvcConfig.getUploadAbsolutePath();
            KnowledgeFile record = knowledgeFileService.uploadFile(file, uploadBasePath, userId);
            return Result.success("文件「" + record.getOriginalFilename() + "」已导入知识库", record);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.fail("文件处理失败: " + e.getMessage());
        }
    }

    /**
     * 获取知识库文件列表
     */
    @Operation(summary = "知识库文件列表")
    @GetMapping("/files")
    public Result<List<KnowledgeFile>> listFiles() {
        return Result.success(knowledgeFileService.listFiles());
    }

    /**
     * 删除知识库文件
     */
    @Operation(summary = "删除知识库文件")
    @DeleteMapping("/files/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        String uploadBasePath = webMvcConfig.getUploadAbsolutePath();
        boolean deleted = knowledgeFileService.deleteFile(id, uploadBasePath);
        if (deleted) {
            return Result.success();
        }
        return Result.fail("文件不存在");
    }

    /**
     * 基于知识库的问答接口
     *
     * 使用独立的 ChatClient 实例，叠加 QuestionAnswerAdvisor，
     * 不影响普通 AI 对话（/ai/chat/*）。
     */
    @Operation(summary = "知识库问答", description = "基于知识库检索的 RAG 问答")
    @PostMapping("/ask")
    public Result<String> ask(@RequestBody AskRequest request,
                               HttpServletRequest httpRequest) {
        Long userId = SecurityUtil.getCurrentUserId(httpRequest);

        // 使用 ChatClient.Builder 构建，叠加 RAG Advisor
        String reply = chatClientBuilder
                .defaultSystem("""
                        你是一个基于知识库回答问题的AI助手。
                        请根据提供的上下文信息回答问题，如果上下文中没有相关内容，请如实告知用户你不了解，不要编造答案。
                        """)
                .defaultAdvisors(questionAnswerAdvisor)
                .build()
                .prompt()
                .user(request.getQuestion())
                .call()
                .content();

        return Result.success(reply);
    }

    // 请求体 DTO
    @lombok.Data
    public static class AskRequest {
        private String question;
    }
}
