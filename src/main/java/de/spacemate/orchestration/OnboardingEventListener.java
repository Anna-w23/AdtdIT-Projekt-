package de.spacemate.orchestration;

/**
 * Observer interface. Any class that wants to react to onboarding state changes
 * implements this — most importantly the JavaFX UI controller.
 */
public interface OnboardingEventListener {
    void onEvent(OnboardingEvent event);
}
