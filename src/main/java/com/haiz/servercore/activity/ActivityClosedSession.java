package com.haiz.servercore.activity;

import java.util.UUID;

public record ActivityClosedSession(UUID uuid, String name, long joinTime, long quitTime, long durationSeconds) {
}
