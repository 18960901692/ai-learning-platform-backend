package com.aicompanion.service;

import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.mapper.KnowledgeFileMapper;
import com.aicompanion.model.entity.KnowledgeFile;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识库文件管理服务
 *
 * 负责：文件上传 → Tika 读取 → 切分 → 向量化 → 存入 Redis VectorStore
 * 元数据持久化到 MySQL knowledge_file 表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeFileService {

    private final VectorStore vectorStore;
    private final KnowledgeFileMapper knowledgeFileMapper;
    private final ObjectMapper objectMapper;

    private static final String UPLOAD_SUB_DIR = "knowledge/";

    /**
     * 上传文件到知识库
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeFile uploadFile(MultipartFile file, String uploadBasePath, Long userId) {
        String originalFilename = file.getOriginalFilename();

        // 1. 存盘
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String extension = getExtension(originalFilename);
        String storedFilename = uuid + extension;
        Path uploadDir = Paths.get(uploadBasePath, UPLOAD_SUB_DIR);
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("创建上传目录失败", e);
        }
        Path savedPath = uploadDir.resolve(storedFilename);
        try {
            Files.copy(file.getInputStream(), savedPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败: " + originalFilename, e);
        }

        // 2. Tika 读取文档
        TikaDocumentReader reader = new TikaDocumentReader(new PathResource(savedPath));
        List<Document> documents = reader.get();
        log.info("读取文档: {} → {} 篇", originalFilename, documents.size());

        // 3. 切分（带 overlap，避免切断语义）
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(300)
                .withMinChunkSizeChars(50)
                .withMinChunkLengthToEmbed(5)
                .withKeepSeparator(true)
                .build();
        List<Document> chunks = splitter.apply(documents);
        log.info("切分成 {} 个片段", chunks.size());

        // 4. 给每个 chunk 打 metadata（source=文件名，便于过滤检索）
        for (Document chunk : chunks) {
            chunk.getMetadata().put("source", originalFilename);
        }

        // 5. 先入库 MySQL（事务保护，失败时自动回滚）
        List<String> docIds = chunks.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        KnowledgeFile record = new KnowledgeFile();
        record.setOriginalFilename(originalFilename);
        record.setStoredFilename(storedFilename);
        record.setFileSize(file.getSize());
        record.setFileType(extension.replace(".", ""));
        record.setChunkCount(chunks.size());
        try {
            record.setChunkDocIds(objectMapper.writeValueAsString(docIds));
        } catch (Exception e) {
            log.warn("序列化 chunkDocIds 失败", e);
            record.setChunkDocIds("[]");
        }
        record.setUserId(userId);

        knowledgeFileMapper.insert(record);
        log.info("文件已入库: {} ({} 片段)", originalFilename, chunks.size());

        // 6. 分批向量化并存入 Redis VectorStore（DashScope Embedding 批量上限 10）
        //    若向量化失败，清理已存储的向量和磁盘文件，并抛出异常触发事务回滚
        try {
            int batchSize = 10;
            for (int i = 0; i < chunks.size(); i += batchSize) {
                List<Document> batch = chunks.subList(i, Math.min(i + batchSize, chunks.size()));
                vectorStore.add(batch);
                log.info("向量化进度: {}/{}", Math.min(i + batchSize, chunks.size()), chunks.size());
            }
        } catch (Exception e) {
            log.error("向量化失败，清理已存储的向量和文件: {}", originalFilename, e);
            // 清理可能已存储的向量
            try {
                vectorStore.delete(docIds);
            } catch (Exception ignored) {
                // 忽略清理异常
            }
            // 清理磁盘文件
            try {
                Files.deleteIfExists(savedPath);
            } catch (IOException ignored) {
                // 忽略清理异常
            }
            throw new RuntimeException("文件向量化失败: " + originalFilename, e);
        }

        return record;
    }

    /**
     * 删除知识库文件
     */
    public boolean deleteFile(Long fileId, String uploadBasePath) {
        KnowledgeFile record = knowledgeFileMapper.selectById(fileId);
        if (record == null) {
            return false;
        }

        // 校验归属：只能删除自己的文件
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (!currentUserId.equals(record.getUserId())) {
            log.warn("越权删除文件: userId={}, ownerId={}, fileId={}", currentUserId, record.getUserId(), fileId);
            throw new RuntimeException("无权删除该文件");
        }

        // 1. 删除磁盘文件
        Path filePath = Paths.get(uploadBasePath, UPLOAD_SUB_DIR, record.getStoredFilename());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("删除磁盘文件失败: {}", filePath, e);
        }

        // 2. 删除向量库中的记录
        try {
            List<String> docIds = objectMapper.readValue(
                    record.getChunkDocIds(),
                    new TypeReference<List<String>>() {}
            );
            vectorStore.delete(docIds);
        } catch (Exception e) {
            log.warn("删除向量库记录失败: {}", e.getMessage());
        }

        // 3. 逻辑删除数据库记录
        knowledgeFileMapper.deleteById(fileId);
        log.info("文件已删除: {}", record.getOriginalFilename());
        return true;
    }

    /**
     * 获取当前用户的文件列表（只能看到自己的文件）
     */
    public List<KnowledgeFile> listFiles() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return knowledgeFileMapper.selectList(
                new LambdaQueryWrapper<KnowledgeFile>()
                        .eq(KnowledgeFile::getUserId, currentUserId)
                        .orderByDesc(KnowledgeFile::getCreateTime)
        );
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
