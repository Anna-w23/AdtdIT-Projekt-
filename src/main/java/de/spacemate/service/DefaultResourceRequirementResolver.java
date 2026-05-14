package de.spacemate.service;

import de.spacemate.model.Appointment;
import de.spacemate.model.AppointmentType;
import de.spacemate.model.ResourceCategory;
import de.spacemate.model.ResourceRequirement;

import java.util.ArrayList;
import java.util.List;

public class DefaultResourceRequirementResolver implements ResourceRequirementResolver {

    private static final ResourceRequirement ROOM_AUTO =
            new ResourceRequirement(ResourceCategory.ROOM, null, true);
    private static final ResourceRequirement TRAINING_ROOM_MANUAL =
            new ResourceRequirement(ResourceCategory.ROOM, "SHUTTLE_ROOM", false);
    private static final ResourceRequirement VR_HEADSET_MANUAL =
            new ResourceRequirement(ResourceCategory.EQUIPMENT, "VR_HEADSET", false);
    private static final ResourceRequirement TRANSLATION_HEADPHONE_MANUAL =
            new ResourceRequirement(ResourceCategory.EQUIPMENT, "TRANSLATION_HEADPHONE", false);

    @Override
    public List<ResourceRequirement> getRequirements(AppointmentType type) {
        return switch (type) {
            case INITIAL_MEDICAL, EYE_SPECIALIST, CARDIOLOGIST, NEUROLOGIST,
                 ORTHOPEDIST, PSYCHOLOGIST_CONSULTATION,
                 FINAL_MEDICAL -> List.of(ROOM_AUTO);
            case SPACE_TRAINING -> List.of(TRAINING_ROOM_MANUAL, VR_HEADSET_MANUAL);
        };
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
