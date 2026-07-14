package com.mctrack.common.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MCTrackAPITest {
    @Test
    void backoffIsBoundedWithoutOverflow() {
        assertEquals(0, MCTrackAPI.calculateBackoffMs(0));
        assertEquals(1_000, MCTrackAPI.calculateBackoffMs(1));
        assertEquals(2_000, MCTrackAPI.calculateBackoffMs(2));
        assertEquals(32_000, MCTrackAPI.calculateBackoffMs(6));
        assertEquals(60_000, MCTrackAPI.calculateBackoffMs(7));
        assertEquals(60_000, MCTrackAPI.calculateBackoffMs(64));
        assertEquals(60_000, MCTrackAPI.calculateBackoffMs(Integer.MAX_VALUE));
    }
}
