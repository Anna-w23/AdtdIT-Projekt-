package de.spacemate.ui;

import de.spacemate.factory.ResourceFactory;
import de.spacemate.factory.StaffFactory;
import de.spacemate.model.*;
import de.spacemate.repository.ResourceAvailabilityRepository;
import de.spacemate.repository.ResourceRepository;
import de.spacemate.repository.StaffAvailabilityRepository;
import de.spacemate.repository.StaffRepository;
import de.spacemate.service.StaffRoomResolver;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class DataSeeder {

    private final StaffRepository staffRepository;
    private final StaffAvailabilityRepository staffAvailabilityRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceAvailabilityRepository resourceAvailabilityRepository;
    private final StaffRoomResolver staffRoomResolver;
    private final StaffFactory staffFactory;
    private final ResourceFactory resourceFactory;

    private final Random rng = new Random(42);

    public DataSeeder(StaffRepository staffRepository,
                      StaffAvailabilityRepository staffAvailabilityRepository,
                      ResourceRepository resourceRepository,
                      ResourceAvailabilityRepository resourceAvailabilityRepository,
                      StaffRoomResolver staffRoomResolver,
                      StaffFactory staffFactory,
                      ResourceFactory resourceFactory) {
        this.staffRepository = staffRepository;
        this.staffAvailabilityRepository = staffAvailabilityRepository;
        this.resourceRepository = resourceRepository;
        this.resourceAvailabilityRepository = resourceAvailabilityRepository;
        this.staffRoomResolver = staffRoomResolver;
        this.staffFactory = staffFactory;
        this.resourceFactory = resourceFactory;
    }

    public void seed() {
        seedStaff();
        seedResources();
    }

    // -------------------------------------------------------------------------
    // Staff + continuous availability schedules
    // -------------------------------------------------------------------------

    private void seedStaff() {
        seedDayStaff("doc-1",    "Dr. Elena Richter",  StaffRole.CHIEF_PHYSICIAN,    0);
        seedDayStaff("doc-2",    "Dr. Hans Gruber",    StaffRole.RESIDENT_PHYSICIAN,  1);
        seedDayStaff("eye-1",    "Dr. Amina Osei",     StaffRole.EYE_SPECIALIST,      0);
        seedDayStaff("cardio-1", "Dr. Luca Ferretti",  StaffRole.CARDIOLOGIST,       -1);
        seedDayStaff("neuro-1",  "Dr. Priya Nair",     StaffRole.NEUROLOGIST,         1);
        seedDayStaff("ortho-1",  "Dr. Bjorn Larsson",  StaffRole.ORTHOPEDIST,         0);
        seedDayStaff("psych-1",  "Dr. Markus Berger",  StaffRole.PSYCHOLOGIST,       -1);
        seedDayStaff("trainer-1","Tom Brandt",          StaffRole.SPACE_TRAINER,       0);
        seedDayStaff("trainer-2","Yara Saleh",          StaffRole.SPACE_TRAINER,      -1);
        seedEveningStaff("trainer-3","Kai Nakamura",    StaffRole.SPACE_TRAINER);
        seedLateNightStaff("trainer-4","Lucia Alvarez", StaffRole.SPACE_TRAINER);

        seedEveningStaff("doc-3",  "Dr. Mei Lin",     StaffRole.RESIDENT_PHYSICIAN);
        seedEveningStaff("psych-2","Dr. Nadia Russo",  StaffRole.PSYCHOLOGIST);

        // Early-morning specialists (6:00 – 9:00)
        seedEarlyMorningStaff("eye-2",   "Dr. Kenji Takahashi", StaffRole.EYE_SPECIALIST);
        seedEarlyMorningStaff("cardio-2","Dr. Sofia Reyes",     StaffRole.CARDIOLOGIST);

        // Late-evening specialists (18:00 – 21:30)
        seedEveningStaff("neuro-2", "Dr. Olga Smirnova", StaffRole.NEUROLOGIST);
        seedEveningStaff("ortho-2", "Dr. James Okonkwo", StaffRole.ORTHOPEDIST);

        // Night physicians: one early-morning, one late-night
        seedEarlyNightStaff("night-1", "Dr. Fatima Al-Rashid", StaffRole.NIGHT_PHYSICIAN);
        seedLateNightStaff("night-2", "Dr. Ivan Petrov",       StaffRole.NIGHT_PHYSICIAN);
    }

    private void seedDayStaff(String id, String name, StaffRole role, int stagger) {
        UUID staffId = UUID.nameUUIDFromBytes(id.getBytes());
        Staff staff = staffFactory.create(staffId, name, role);
        staffRepository.save(staff);

        int morningStart = 8 + stagger;
        int morningEnd = 12 + stagger;
        int afternoonStart = 13 + stagger;
        int afternoonEnd = 17 + stagger;

        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        for (int week = 0; week < 4; week++) {
            for (int day = 0; day < 5; day++) {
                LocalDate date = monday.plusWeeks(week).plusDays(day);
                if (rng.nextDouble() < 0.10) continue;

                staffAvailabilityRepository.save(new StaffAvailability(
                        UUID.randomUUID(), staff,
                        date.atTime(morningStart, 0),
                        date.atTime(morningEnd, 0)));

                staffAvailabilityRepository.save(new StaffAvailability(
                        UUID.randomUUID(), staff,
                        date.atTime(afternoonStart, 0),
                        date.atTime(afternoonEnd, 0)));
            }
        }
    }

    private void seedEveningStaff(String id, String name, StaffRole role) {
        UUID staffId = UUID.nameUUIDFromBytes(id.getBytes());
        Staff staff = staffFactory.create(staffId, name, role);
        staffRepository.save(staff);

        LocalDate today = LocalDate.now();
        for (int dayOffset = 0; dayOffset < 28; dayOffset++) {
            LocalDate date = today.plusDays(dayOffset);
            if (rng.nextDouble() < 0.15) continue;

            staffAvailabilityRepository.save(new StaffAvailability(
                    UUID.randomUUID(), staff,
                    date.atTime(17, 0),
                    date.atTime(21, 30)));
        }
    }

    private void seedEarlyMorningStaff(String id, String name, StaffRole role) {
        UUID staffId = UUID.nameUUIDFromBytes(id.getBytes());
        Staff staff = staffFactory.create(staffId, name, role);
        staffRepository.save(staff);

        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        for (int week = 0; week < 4; week++) {
            for (int day = 0; day < 5; day++) {
                LocalDate date = monday.plusWeeks(week).plusDays(day);
                if (rng.nextDouble() < 0.15) continue;

                staffAvailabilityRepository.save(new StaffAvailability(
                        UUID.randomUUID(), staff,
                        date.atTime(6, 0),
                        date.atTime(9, 0)));
            }
        }
    }

    private void seedEarlyNightStaff(String id, String name, StaffRole role) {
        UUID staffId = UUID.nameUUIDFromBytes(id.getBytes());
        Staff staff = staffFactory.create(staffId, name, role);
        staffRepository.save(staff);

        LocalDate today = LocalDate.now();
        for (int week = 0; week < 4; week++) {
            List<Integer> nightDays = pickRandomDays(3 + rng.nextInt(2), 7);
            for (int day : nightDays) {
                LocalDate date = today.plusWeeks(week).plusDays(day);

                staffAvailabilityRepository.save(new StaffAvailability(
                        UUID.randomUUID(), staff,
                        date.atTime(5, 0),
                        date.atTime(9, 0)));
            }
        }
    }

    private void seedLateNightStaff(String id, String name, StaffRole role) {
        UUID staffId = UUID.nameUUIDFromBytes(id.getBytes());
        Staff staff = staffFactory.create(staffId, name, role);
        staffRepository.save(staff);

        LocalDate today = LocalDate.now();
        for (int week = 0; week < 4; week++) {
            List<Integer> nightDays = pickRandomDays(3 + rng.nextInt(2), 7);
            for (int day : nightDays) {
                LocalDate date = today.plusWeeks(week).plusDays(day);

                staffAvailabilityRepository.save(new StaffAvailability(
                        UUID.randomUUID(), staff,
                        date.atTime(20, 0),
                        date.atTime(23, 59)));

                staffAvailabilityRepository.save(new StaffAvailability(
                        UUID.randomUUID(), staff,
                        date.plusDays(1).atTime(0, 0),
                        date.plusDays(1).atTime(2, 0)));
            }
        }
    }

    private List<Integer> pickRandomDays(int count, int total) {
        List<Integer> days = new ArrayList<>();
        for (int i = 0; i < total; i++) days.add(i);
        Collections.shuffle(days, rng);
        return days.subList(0, Math.min(count, total));
    }

    // -------------------------------------------------------------------------
    // Resource seeding
    // -------------------------------------------------------------------------

    private void seedResources() {
        LocalDate today = LocalDate.now();

        seedMedicalRoom("room-richter", "M2.01", "doc-1");
        seedMedicalRoom("room-gruber", "M2.02", "doc-2");
        seedMedicalRoom("room-osei", "M2.03", "eye-1");
        seedMedicalRoom("room-ferretti", "M2.04", "cardio-1");
        seedMedicalRoom("room-nair", "M2.05", "neuro-1");
        seedMedicalRoom("room-larsson", "M2.06", "ortho-1");
        seedMedicalRoom("room-berger", "M2.07", "psych-1");
        seedMedicalRoom("room-takahashi", "M2.08", "eye-2");
        seedMedicalRoom("room-reyes", "M2.09", "cardio-2");
        seedMedicalRoom("room-smirnova", "M2.10", "neuro-2");
        seedMedicalRoom("room-okonkwo", "M2.11", "ortho-2");

        seedBusinessHoursRoom("room-shuttle-a", "S1.01", "SHUTTLE_ROOM", today);
        seedBusinessHoursRoom("room-shuttle-b", "S1.02", "SHUTTLE_ROOM", today);
        seedBusinessHoursRoom("room-shuttle-c", "S1.03", "SHUTTLE_ROOM", today);

        seedBusinessHoursEquipment("vr-1", "VR Headset #1", "VR_HEADSET", today);
        seedBusinessHoursEquipment("vr-2", "VR Headset #2", "VR_HEADSET", today);
        seedBusinessHoursEquipment("vr-3", "VR Headset #3", "VR_HEADSET", today);

        seedBusinessHoursEquipment("headphone-1", "Headphone #1", "TRANSLATION_HEADPHONE", today);
        seedBusinessHoursEquipment("headphone-2", "Headphone #2", "TRANSLATION_HEADPHONE", today);
        seedBusinessHoursEquipment("headphone-3", "Headphone #3", "TRANSLATION_HEADPHONE", today);
        seedBusinessHoursEquipment("headphone-4", "Headphone #4", "TRANSLATION_HEADPHONE", today);
    }

    private void seedMedicalRoom(String id, String name, String staffId) {
        UUID roomId = UUID.nameUUIDFromBytes(id.getBytes());
        UUID staffUuid = UUID.nameUUIDFromBytes(staffId.getBytes());

        Resource room = resourceFactory.create(roomId, name, ResourceCategory.ROOM, "MEDICAL_ROOM");
        resourceRepository.save(room);
        staffRoomResolver.registerMapping(staffUuid, roomId);

        seedBusinessHoursAvailability(room, LocalDate.now());
    }

    private void seedBusinessHoursRoom(String id, String name, String tag, LocalDate startDate) {
        UUID resourceId = UUID.nameUUIDFromBytes(id.getBytes());
        Resource room = resourceFactory.create(resourceId, name, ResourceCategory.ROOM, tag);
        resourceRepository.save(room);
        seedBusinessHoursAvailability(room, startDate);
    }

    private void seedBusinessHoursEquipment(String id, String name, String tag, LocalDate startDate) {
        UUID resourceId = UUID.nameUUIDFromBytes(id.getBytes());
        Resource equipment = resourceFactory.create(resourceId, name, ResourceCategory.EQUIPMENT, tag);
        resourceRepository.save(equipment);
        seedBusinessHoursAvailability(equipment, startDate);
    }

    private void seedBusinessHoursAvailability(Resource resource, LocalDate startDate) {
        for (int d = 0; d < 28; d++) {
            LocalDate day = startDate.plusDays(d);
            LocalDateTime start = day.atTime(0, 0);
            LocalDateTime end = day.atTime(23, 59);
            resourceAvailabilityRepository.save(new ResourceAvailability(
                    UUID.randomUUID(), resource, start, end));
        }
    }
}
