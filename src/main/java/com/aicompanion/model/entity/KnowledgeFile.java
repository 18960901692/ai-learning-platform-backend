package com.aicompanion.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文件实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_file")
public class KnowledgeFile extends BaseEntity {

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 存储文件名（UUID + 扩展名）
     */
    private String storedFilename;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（扩展名）
     */
    private String fileType;

    /**
     * 切分片段数
     */
    private Integer chunkCount;

    /**
     * 向量库中的文档ID列表（JSON 数组字符串）
     */
    private String chunkDocIds;

    /**
     * 上传用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
}
