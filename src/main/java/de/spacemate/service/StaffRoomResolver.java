package de.spacemate.service;

import java.util.Optional;
import java.util.UUID;

public interface StaffRoomResolver {

    Optional<UUID> getDefaultRoom(UUID staffId);

    void registerMapping(UUID staffId, UUID roomId);
}
