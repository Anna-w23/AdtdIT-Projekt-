package de.spacemate.ui;

import de.spacemate.app.SimulatedClock;
import de.spacemate.model.Customer;
import de.spacemate.orchestration.OnboardingOrchestrator;

import java.time.LocalDate;
import java.util.*;

public class CustomerSpawner {

    private static final String[][] NAME_POOL = {
            {"Maria", "Santos"}, {"James", "Wright"}, {"Yuki", "Tanaka"},
            {"Lena", "Hoffmann"}, {"Omar", "Khalil"}, {"Sofia", "Müller"},
            {"Chen", "Wei"}, {"Anna", "Bergström"}, {"Diego", "Reyes"},
            {"Petra", "Novak"}, {"Lucas", "Fontaine"}, {"Aisha", "Okafor"},
            {"Raj", "Malhotra"}, {"Elena", "Sokolova"}, {"Tom", "Nakamura"},
            {"Fatima", "Al-Rashid"}, {"Björn", "Eriksson"}, {"Chiara", "Rossi"},
            {"Kenji", "Watanabe"}, {"Amara", "Diallo"}, {"Viktor", "Petrov"},
            {"Ingrid", "Hansen"}, {"Marco", "Silva"}, {"Hana", "Kim"},
            {"Felix", "Weber"}, {"Nadia", "Khoury"}, {"Oscar", "Lindgren"},
            {"Priya", "Sharma"}, {"Leo", "Dubois"}, {"Mina", "Yamamoto"}
    };

    private static final String[] LANGUAGES = {"EN", "EN", "EN", "EN", "EN",
            "DE", "DE", "ES", "FR", "JA", "ZH", "AR", "RU", "SV", "IT"};

    private final OnboardingOrchestrator orchestrator;
    private final SimulatedClock clock;
    private final Random rng = new Random();
    private int nameIndex = 0;

    public CustomerSpawner(OnboardingOrchestrator orchestrator, SimulatedClock clock) {
        this.orchestrator = orchestrator;
        this.clock = clock;
    }

    public List<Customer> spawnDaily() {
        int count = 1 + rng.nextInt(3);
        List<Customer> spawned = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            spawned.add(spawnOne());
        }
        return spawned;
    }

    private Customer spawnOne() {
        String[] name = NAME_POOL[nameIndex % NAME_POOL.length];
        nameIndex++;

        String firstName = name[0];
        String lastName = name[1];
        String email = firstName.toLowerCase() + "." + lastName.toLowerCase().replace("ü", "ue")
                .replace("ö", "oe").replace("ä", "ae").replace(" ", "")
                .replace("-", "") + "@email.com";
        String language = LANGUAGES[rng.nextInt(LANGUAGES.length)];

        int weeksOut = 3 + rng.nextInt(6);
        LocalDate takeoff = clock.today().plusWeeks(weeksOut);

        Customer customer = orchestrator.registerCustomer(firstName, lastName, email, language);
        customer.setTakeoffDate(takeoff);
        customer.setNeedsAttention(true, "New registration – send questionnaire");
        orchestrator.saveCustomer(customer);

        return customer;
    }
}
