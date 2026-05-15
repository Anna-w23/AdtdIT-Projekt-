package de.spacemate.service;

import de.spacemate.model.CustomerResponse;

import java.util.UUID;

public interface QuestionnaireCommunicationService {
    CustomerResponse sendQuestionnaire(UUID customerId);
}
