package de.spacemate.service;

import java.util.*;

public class DefaultStaffRoomResolver implements StaffRoomResolver {

    private final Map<UUID, UUID> staffToRoom = new HashMap<>();

    @Override
    public Optional<UUID> getDefaultRoom(UUID staffId) {
        return Optional.ofNullable(staffToRoom.get(staffId));
    }

    @Override
    public void registerMapping(UUID staffId, UUID roomId) {
        staffToRoom.put(staffId, roomId);
    }
}
