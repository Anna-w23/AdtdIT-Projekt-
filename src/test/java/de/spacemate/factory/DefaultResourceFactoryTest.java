package de.spacemate.factory;

import de.spacemate.model.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DefaultResourceFactoryTest {

    private final ResourceFactory factory = new DefaultResourceFactory();

    @Test
    void createsRoomForRoomCategory() {
        Resource resource = factory.create(UUID.randomUUID(), "Office 1", ResourceCategory.ROOM, "MEDICAL_ROOM");
        assertInstanceOf(Room.class, resource);
    }

    @Test
    void createsEquipmentForEquipmentCategory() {
        Resource resource = factory.create(UUID.randomUUID(), "VR Headset #1", ResourceCategory.EQUIPMENT, "VR_HEADSET");
        assertInstanceOf(Equipment.class, resource);
    }

    @Test
    void assignsIdNameAndTag() {
        UUID id = UUID.randomUUID();
        Resource resource = factory.create(id, "Shuttle Room A", ResourceCategory.ROOM, "SHUTTLE_ROOM");
        assertEquals(id, resource.getId());
        assertEquals("Shuttle Room A", resource.getName());
        assertEquals(ResourceCategory.ROOM, resource.getCategory());
        assertEquals("SHUTTLE_ROOM", resource.getTag());
    }

    @Test
    void allowsNullTag() {
        Resource resource = factory.create(UUID.randomUUID(), "Generic Room", ResourceCategory.ROOM, null);
        assertNull(resource.getTag());
    }
}
