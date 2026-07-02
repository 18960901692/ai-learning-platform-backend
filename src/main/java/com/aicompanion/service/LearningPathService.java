package com.aicompanion.service;

import com.aicompanion.model.vo.LearningPathVO;

import java.util.List;

/**
 * 学习路径推荐服务接口
 */
public interface LearningPathService {

    /**
     * 获取用户推荐学习路径（纯规则）
     */
    List<LearningPathVO> getRecommendations(Long userId);

    /**
     * 获取用户推荐学习路径（规则 + AI增强）
     */
    List<LearningPathVO> getRecommendationsWithAi(Long userId);
}
