package com.walkietalkie.pv;

import su.plo.voice.api.server.PlasmoVoiceServer;

import java.lang.reflect.Method;

/**
 * Registers a permission that defaults to "everyone may use it".
 * Done with reflection because the permission-manager accessor differs between Plasmo Voice versions.
 * If this fails, the log tells you, and you can grant the permission with LuckPerms or similar.
 */
final class PermissionCompat {
    private PermissionCompat() {}

    static void registerDefaultTrue(PlasmoVoiceServer voiceServer, String permission) {
        try {
            Object[] holders = { voiceServer, voiceServer.getMinecraftServer() };
            String[] getters = { "getPermissionManager", "getPermissionsManager", "getPermissions" };

            Class<?> defaultClass = Class.forName("su.plo.voice.api.server.permission.PermissionDefault");
            Object allow = null;
            for (Object c : defaultClass.getEnumConstants()) {
                if (c.toString().equals("TRUE")) allow = c;
            }
            if (allow == null) throw new IllegalStateException("PermissionDefault.TRUE not found");

            for (Object holder : holders) {
                for (String getter : getters) {
                    Object manager;
                    try {
                        manager = holder.getClass().getMethod(getter).invoke(holder);
                    } catch (ReflectiveOperationException e) {
                        continue;
                    }
                    for (Method m : manager.getClass().getMethods()) {
                        if (m.getName().equals("register") && m.getParameterCount() == 2
                                && m.getParameterTypes()[0] == String.class
                                && m.getParameterTypes()[1] == defaultClass) {
                            m.invoke(manager, permission, allow);
                            return;
                        }
                    }
                }
            }
            throw new IllegalStateException("permission manager not found");
        } catch (Throwable t) {
            System.err.println("[WalkieTalkie] Could not register default permission '" + permission
                    + "' (" + t + "). Grant it manually if players can't use the Walkie Talkie key.");
        }
    }
}
