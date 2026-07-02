package com.aicompanion.service;

public interface CheckInService {

    void checkIn(Long userId);

    int getConsecutiveDays(Long userId);
}