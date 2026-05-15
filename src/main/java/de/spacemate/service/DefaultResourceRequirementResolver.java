package de.spacemate.service;

import de.spacemate.model.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DefaultResourceRequirementResolver implements ResourceRequirementResolver {

    private static final ResourceRequirement ROOM_AUTO =
            new ResourceRequirement(ResourceCategory.ROOM, null, true);
    private static final ResourceRequirement TRAINING_ROOM_MANUAL =
            new ResourceRequirement(ResourceCategory.ROOM, "SHUTTLE_ROOM", false);
    private static final ResourceRequirement VR_HEADSET_MANUAL =
            new ResourceRequirement(ResourceCategory.EQUIPMENT, "VR_HEADSET", false);
    private static final ResourceRequirement TRANSLATION_HEADPHONE_MANUAL =
            new ResourceRequirement(ResourceCategory.EQUIPMENT, "TRANSLATION_HEADPHONE", false);

    private static final Map<AppointmentType.ResourceProfile, List<ResourceRequirement>> PROFILE_REQUIREMENTS;

    static {
        PROFILE_REQUIREMENTS = new EnumMap<>(AppointmentType.ResourceProfile.class);
        PROFILE_REQUIREMENTS.put(AppointmentType.ResourceProfile.MEDICAL, List.of(ROOM_AUTO));
        PROFILE_REQUIREMENTS.put(AppointmentType.ResourceProfile.TRAINING, List.of(TRAINING_ROOM_MANUAL, VR_HEADSET_MANUAL));
    }

    @Override
    public List<ResourceRequirement> getRequirements(AppointmentType type) {
        return PROFILE_REQUIREMENTS.getOrDefault(type.getResourceProfile(), List.of());
    }

    @Override
    public List<ResourceRequirement> getRequirements(Appointment appointment) {
        List<ResourceRequirement> reqs = new ArrayList<>(getRequirements(appointment.getType()));
        if (!"EN".equals(appointment.getCustomer().getPreferredLanguage())) {
            reqs.add(TRANSLATION_HEADPHONE_MANUAL);
        }
        return reqs;
    }
}
