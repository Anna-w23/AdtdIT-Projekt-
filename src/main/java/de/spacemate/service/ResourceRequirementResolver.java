package de.spacemate.service;

import de.spacemate.model.Appointment;
import de.spacemate.model.AppointmentType;
import de.spacemate.model.ResourceRequirement;

import java.util.List;

public interface ResourceRequirementResolver {

    List<ResourceRequirement> getRequirements(AppointmentType type);

    default List<ResourceRequirement> getRequirements(Appointment appointment) {
        return getRequirements(appointment.getType());
    }
}
