package com.aicompanion.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台考核记录 VO
 */
@Data
public class AdminExamRecordVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;

    private String skillName;

    private Integer score;

    private String status;

    private String duration;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
