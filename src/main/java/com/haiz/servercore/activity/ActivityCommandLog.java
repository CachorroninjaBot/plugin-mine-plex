package com.haiz.servercore.activity;

import java.util.UUID;

public record ActivityCommandLog(UUID uuid, String name, String command, long timestamp) {
}
