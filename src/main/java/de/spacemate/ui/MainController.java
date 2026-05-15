package de.spacemate.ui;

import de.spacemate.app.SimulatedClock;
import de.spacemate.app.SimulationConfig;
import de.spacemate.model.*;
import de.spacemate.orchestration.OnboardingEvent;
import de.spacemate.orchestration.OnboardingEventListener;
import de.spacemate.orchestration.OnboardingEventType;
import de.spacemate.orchestration.OnboardingOrchestrator;
import de.spacemate.repository.ResourceRepository;
import de.spacemate.repository.StaffRepository;
import de.spacemate.service.AppointmentTypeStaffResolver;
import de.spacemate.service.CustomerNotAvailableException;
import de.spacemate.service.SimulationAdvancer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Root UI controller.
 *
 * Layout:
 *   TOP    — toolbar: week nav + simulated date + Simulate Day button + Auto toggle + event log + Register
 *   LEFT   — VBox: customer list (top) + action panel for selected customer (bottom)
 *   CENTER — WeekCalendarView showing the selected customer's appointments
 *   BOTTOM — staff strip: one ToggleButton per staff member, toggle shows/hides their slots
 */
public class MainController implements OnboardingEventListener {

    private static final DateTimeFormatter WEEK_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DAY_FMT  = DateTimeFormatter.ofPattern("EEE dd MMM yyyy");

    private final OnboardingOrchestrator orchestrator;
    private final StaffRepository staffRepository;
    private final ResourceRepository resourceRepository;
    private final AppointmentTypeStaffResolver staffResolver;
    private final SimulatedClock clock;
    private final SimulationConfig simulationConfig;
    private final WeekCalendarView calendarView;
    private final DocumentListPopover documentListPopover;
    private final CustomerSpawner customerSpawner;

    private final BorderPane root = new BorderPane();
    private final ListView<Customer> customerList = new ListView<>();
    private final Label eventLog = new Label();
    private final Label weekLabel = new Label();
    private final Label simDayLabel = new Label();
    private final HBox statusBanner = new HBox();
    private final HBox actionBar = new HBox(6);
    private boolean autoSpawnEnabled = true;

    private final Timeline eventLogClearTimer = new Timeline(
            new KeyFrame(Duration.seconds(3), e -> eventLog.setText("")));

    private boolean suppressEventLogClear = false;

    /** Fires every 100ms to advance the simulated clock; 60 real seconds = 24 simulated hours. */
    private final Timeline autoTimeline = new Timeline(
            new KeyFrame(Duration.millis(100), e -> tickSimulation()));

    private final SimulationAdvancer simulationAdvancer;

    public MainController(OnboardingOrchestrator orchestrator,
                          StaffRepository staffRepository,
                          ResourceRepository resourceRepository,
                          AppointmentTypeStaffResolver staffResolver,
                          SimulationAdvancer simulationAdvancer,
                          SimulatedClock clock,
                          CustomerSpawner customerSpawner,
                          SimulationConfig simulationConfig) {
        this.orchestrator = orchestrator;
        this.staffRepository = staffRepository;
        this.resourceRepository = resourceRepository;
        this.staffResolver = staffResolver;
        this.simulationAdvancer = simulationAdvancer;
        this.clock = clock;
        this.simulationConfig = simulationConfig;
        this.customerSpawner = customerSpawner;
        this.calendarView = new WeekCalendarView(orchestrator, staffRepository, clock, this::refreshCalendar);
        this.documentListPopover = new DocumentListPopover(orchestrator);
        calendarView.setDragScheduleHandler(this::onDragComplete);
        calendarView.setCustomerClickHandler(this::selectCustomer);
        autoTimeline.setCycleCount(Timeline.INDEFINITE);
        orchestrator.addEventListener(this);
        buildLayout();
        if (autoSpawnEnabled) customerSpawner.spawnDaily();
        refreshCustomerList();
        updateSimDayLabel();
    }

    public BorderPane getRoot() { return root; }

    // -------------------------------------------------------------------------
    // Observer
    // -------------------------------------------------------------------------

    @Override
    public void onEvent(OnboardingEvent event) {
        Platform.runLater(() -> {
            if (event.getType() == OnboardingEventType.APPOINTMENT_SUGGESTED) {
                showPersistentEvent(event.getMessage());
            } else {
                showEvent(event.getMessage());
            }
            if (event.getType() == OnboardingEventType.APPOINTMENT_SENT) {
                orchestrator.collectPendingResponses();
            }
            refreshAfterAction(event.getCustomer().getId());
        });
    }

    // -------------------------------------------------------------------------
    // Layout
    // -------------------------------------------------------------------------

    private void buildLayout() {
        root.setTop(buildToolbar());
        root.setLeft(buildCustomerSidebar());

        // Status banner + action bar sit above the calendar view
        statusBanner.setVisible(false);
        statusBanner.setManaged(false);
        actionBar.setVisible(false);
        actionBar.setManaged(false);
        actionBar.setPadding(new Insets(4, 12, 4, 12));
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.setStyle("-fx-background-color: #F8F8F8; -fx-border-color: #DDD; -fx-border-width: 0 0 1 0;");
        VBox centerBox = new VBox(statusBanner, actionBar, calendarView);
        VBox.setVgrow(calendarView, Priority.ALWAYS);
        root.setCenter(centerBox);

        root.setBottom(buildStaffStrip());
    }

    private VBox buildToolbar() {
        // Week navigation
        Button prevWeek = new Button("← Week");
        prevWeek.setOnAction(e -> { calendarView.goToPreviousWeek(); updateWeekLabel(); });
        Button nextWeek = new Button("Week →");
        nextWeek.setOnAction(e -> { calendarView.goToNextWeek(); updateWeekLabel(); });
        updateWeekLabel();

        HBox nav = new HBox(6, prevWeek, weekLabel, nextWeek);
        nav.setAlignment(Pos.CENTER_LEFT);

        // Simulation controls
        simDayLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        simDayLabel.setStyle("-fx-text-fill: #1565C0;");

        Button simDayBtn = new Button("▶ Simulate Day");
        simDayBtn.setStyle("-fx-font-size: 11;");
        simDayBtn.setOnAction(e -> simulateNextDay());

        ToggleButton autoBtn = new ToggleButton("⏱ Auto (1 min/day)");
        autoBtn.setStyle("-fx-font-size: 11;");
        autoBtn.selectedProperty().addListener((obs, wasOn, isOn) -> {
            if (isOn) {
                autoTimeline.play();
                autoBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 11;");
            } else {
                autoTimeline.stop();
                autoBtn.setStyle("-fx-font-size: 11;");
            }
        });

        HBox simControls = new HBox(6, simDayLabel, simDayBtn, autoBtn);
        simControls.setAlignment(Pos.CENTER_LEFT);
        simControls.setPadding(new Insets(0, 12, 0, 12));
        simControls.setStyle("-fx-border-color: #BBDEFB; -fx-border-width: 0 0 0 1;");

        CheckBox showCompletedCb = new CheckBox("Show completed");
        showCompletedCb.setStyle("-fx-font-size: 11;");
        showCompletedCb.setOnAction(e -> calendarView.setShowCompleted(showCompletedCb.isSelected()));

        CheckBox autoSpawnCb = new CheckBox("Auto-spawn");
        autoSpawnCb.setSelected(true);
        autoSpawnCb.setStyle("-fx-font-size: 11;");
        autoSpawnCb.setOnAction(e -> autoSpawnEnabled = autoSpawnCb.isSelected());

        CheckBox instantResponseCb = new CheckBox("Instant response");
        instantResponseCb.setSelected(simulationConfig.isInstantResponse());
        instantResponseCb.setStyle("-fx-font-size: 11;");
        instantResponseCb.setOnAction(e -> simulationConfig.setInstantResponse(instantResponseCb.isSelected()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        eventLog.setStyle("-fx-text-fill: #555; -fx-font-style: italic;");
        eventLog.setMaxWidth(400);

        HBox toolbar = new HBox(10, nav, simControls, showCompletedCb, autoSpawnCb, instantResponseCb, spacer, eventLog);
        toolbar.setPadding(new Insets(8, 12, 8, 12));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: #DDD; -fx-border-width: 0 0 1 0;");

        return new VBox(toolbar);
    }

    private VBox buildCustomerSidebar() {
        Label header = new Label("Customers");
        header.setFont(Font.font("System", FontWeight.BOLD, 13));
        header.setPadding(new Insets(8, 8, 4, 8));
        header.setMaxWidth(Double.MAX_VALUE);
        header.setStyle("-fx-background-color: #F0F0F0; -fx-border-color: #DDD; -fx-border-width: 0 0 1 0;");

        customerList.setCellFactory(lv -> new CustomerCell());
        customerList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (!suppressEventLogClear && old != null && (sel == null || !sel.getId().equals(old.getId()))) {
                eventLog.setText("");
                eventLogClearTimer.stop();
            }
            if (sel != null) {
                calendarView.setSelectedCustomer(sel);
                rebuildStatusBanner(sel);
                rebuildActionBar(sel);
            } else {
                calendarView.setSelectedCustomer(null);
                statusBanner.setVisible(false);
                statusBanner.setManaged(false);
                actionBar.setVisible(false);
                actionBar.setManaged(false);
            }
        });

        VBox sidebar = new VBox(header, customerList);
        VBox.setVgrow(customerList, Priority.ALWAYS);
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(190);
        sidebar.setStyle("-fx-border-color: #DDD; -fx-border-width: 0 1 0 0;");
        return sidebar;
    }

    private HBox buildStaffStrip() {
        List<Map.Entry<String, List<StaffRole>>> roleGroups = List.of(
                Map.entry("Physicians",  List.of(StaffRole.CHIEF_PHYSICIAN, StaffRole.RESIDENT_PHYSICIAN, StaffRole.NIGHT_PHYSICIAN)),
                Map.entry("Specialists", List.of(StaffRole.EYE_SPECIALIST, StaffRole.CARDIOLOGIST,
                                                  StaffRole.NEUROLOGIST, StaffRole.ORTHOPEDIST, StaffRole.PSYCHOLOGIST)),
                Map.entry("Training",    List.of(StaffRole.SPACE_TRAINER)),
                Map.entry("Management",  List.of(StaffRole.PROCESS_MANAGER))
        );

        // One TitledPane per group; content is a FlowPane of fixed-width tiles so groups
        // use horizontal space naturally and only scroll vertically when many groups are open
        VBox panesBox = new VBox(2);
        panesBox.setPadding(new Insets(2, 0, 2, 0));

        ToggleGroup exclusiveGroup = new ToggleGroup();

        for (var groupEntry : roleGroups) {
            String groupName = groupEntry.getKey();
            List<StaffRole> roles = groupEntry.getValue();

            List<Staff> groupStaff = staffRepository.findAll().stream()
                    .filter(s -> roles.contains(s.getRole()))
                    .toList();
            if (groupStaff.isEmpty()) continue;

            FlowPane tiles = new FlowPane(6, 4);
            tiles.setPadding(new Insets(4, 6, 4, 6));

            for (Staff staff : groupStaff) {
                Label nameLbl = new Label(staff.getName());
                nameLbl.setFont(Font.font("System", FontWeight.BOLD, 11));
                nameLbl.setStyle("-fx-text-fill: #1A1A1A;");
                Label roleLbl = new Label(formatRole(staff.getRole()));
                roleLbl.setFont(Font.font(10));
                roleLbl.setStyle("-fx-text-fill: #555555;");

                VBox tileContent = new VBox(1, nameLbl, roleLbl);
                tileContent.setPadding(new Insets(4, 6, 4, 6));

                ToggleButton toggle = new ToggleButton();
                toggle.setGraphic(tileContent);
                toggle.setToggleGroup(exclusiveGroup);
                toggle.setPrefWidth(170);
                toggle.setMinWidth(150);
                toggle.setStyle("-fx-alignment: center-left; -fx-background-color: white; "
                        + "-fx-border-color: #CCCCCC; -fx-border-radius: 4; -fx-background-radius: 4;");
                toggle.selectedProperty().addListener((obs, wasOn, isOn) -> {
                    calendarView.setStaffVisible(staff.getId(), isOn);
                    toggle.setStyle("-fx-alignment: center-left; -fx-background-radius: 4; -fx-border-radius: 4; "
                            + (isOn
                            ? "-fx-background-color: #BBDEFB; -fx-border-color: #1565C0;"
                            : "-fx-background-color: white; -fx-border-color: #CCCCCC;"));
                });
                tiles.getChildren().add(toggle);
            }

            TitledPane pane = new TitledPane(groupName, tiles);
            pane.setCollapsible(true);
            pane.setExpanded(false);
            pane.setMaxWidth(Double.MAX_VALUE);
            panesBox.getChildren().add(pane);
        }

        // Resource groups
        List<Map.Entry<String, ResourceCategory>> resourceGroups = List.of(
                Map.entry("Rooms", ResourceCategory.ROOM),
                Map.entry("Equipment", ResourceCategory.EQUIPMENT)
        );

        for (var rGroup : resourceGroups) {
            String groupName = rGroup.getKey();
            ResourceCategory category = rGroup.getValue();
            List<Resource> resources = resourceRepository.findByCategory(category);
            if (resources.isEmpty()) continue;

            FlowPane tiles = new FlowPane(6, 4);
            tiles.setPadding(new Insets(4, 6, 4, 6));

            for (Resource resource : resources) {
                Label nameLbl = new Label(resource.getName());
                nameLbl.setFont(Font.font("System", FontWeight.BOLD, 11));
                nameLbl.setStyle("-fx-text-fill: #1A1A1A;");
                Label tagLbl = new Label(formatResourceTag(resource.getTag()));
                tagLbl.setFont(Font.font(10));
                tagLbl.setStyle("-fx-text-fill: #555555;");

                VBox tileContent = new VBox(1, nameLbl, tagLbl);
                tileContent.setPadding(new Insets(4, 6, 4, 6));

                ToggleButton toggle = new ToggleButton();
                toggle.setGraphic(tileContent);
                toggle.setToggleGroup(exclusiveGroup);
                toggle.setPrefWidth(170);
                toggle.setMinWidth(150);
                toggle.setStyle("-fx-alignment: center-left; -fx-background-color: white; "
                        + "-fx-border-color: #CCCCCC; -fx-border-radius: 4; -fx-background-radius: 4;");
                toggle.selectedProperty().addListener((obs, wasOn, isOn) -> {
                    calendarView.setResourceVisible(resource.getId(), isOn);
                    toggle.setStyle("-fx-alignment: center-left; -fx-background-radius: 4; -fx-border-radius: 4; "
                            + (isOn
                            ? "-fx-background-color: #E8EAF6; -fx-border-color: #5C6BC0;"
                            : "-fx-background-color: white; -fx-border-color: #CCCCCC;"));
                });
                tiles.getChildren().add(toggle);
            }

            TitledPane pane = new TitledPane(groupName + "  (" + resources.size() + ")", tiles);
            pane.setCollapsible(true);
            pane.setExpanded(false);
            pane.setMaxWidth(Double.MAX_VALUE);
            panesBox.getChildren().add(pane);
        }

        ScrollPane scrollPane = new ScrollPane(panesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        HBox strip = new HBox(scrollPane);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        strip.setPadding(new Insets(4, 12, 4, 12));
        strip.setAlignment(Pos.TOP_LEFT);
        strip.setPrefHeight(160);
        strip.setMaxHeight(160);
        strip.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #DDD; -fx-border-width: 1 0 0 0;");
        return strip;
    }

    // -------------------------------------------------------------------------
    // Action bar (compact button row above calendar)
    // -------------------------------------------------------------------------

    /**
     * Rebuilds the action bar for the given customer. Shows only the context-sensitive
     * action buttons — no duplicate status text (that lives in the banner).
     */
    private void rebuildActionBar(Customer customer) {
        actionBar.getChildren().clear();
        if (customer == null) {
            actionBar.setVisible(false);
            actionBar.setManaged(false);
            return;
        }
        buildActionButtons(customer);
        boolean hasActions = !actionBar.getChildren().isEmpty();
        actionBar.setVisible(hasActions);
        actionBar.setManaged(hasActions);
    }

    private void rebuildStatusBanner(Customer customer) {
        if (customer == null) {
            statusBanner.setVisible(false);
            statusBanner.setManaged(false);
            return;
        }

        OnboardingStage stage = customer.getCurrentStage();
        boolean needsAttention = customer.isNeedsAttention();

        // Line 1: customer name + current stage
        String stageName = formatStage(stage);
        String line1Text = customer.getFullName() + "  —  " + stageName;
        long daysLeft = daysUntilTakeoff(customer);
        if (daysLeft >= 0 && stage != OnboardingStage.APPROVED && stage != OnboardingStage.REJECTED) {
            line1Text += "  (" + daysLeft + "d to takeoff)";
        }

        Label line1 = new Label(line1Text);
        line1.setFont(Font.font("System", FontWeight.BOLD, 12));
        line1.setTextFill(Color.web("#1A1A1A"));
        line1.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(line1, Priority.ALWAYS);

        // Status badge
        String scoreColor = urgencyColor(customer);
        Label scoreBadge;
        if (isCustomerFailed(customer)) {
            scoreBadge = new Label("Failed");
            scoreColor = "#9E9E9E";
            Tooltip.install(scoreBadge, new Tooltip("Customer cannot complete onboarding"));
        } else if (isCustomerApproved(customer)) {
            scoreBadge = new Label("🚀 Ready");
            scoreColor = "#5C6BC0";
            Tooltip.install(scoreBadge, new Tooltip("Customer approved for flight"));
        } else {
            long days = daysUntilTakeoff(customer);
            if (days >= 0) {
                scoreBadge = new Label(days + "d");
                Tooltip.install(scoreBadge, new Tooltip(days + " days until takeoff"));
            } else {
                scoreBadge = new Label("—");
                Tooltip.install(scoreBadge, new Tooltip("No takeoff date set"));
            }
        }
        scoreBadge.setFont(Font.font("System", FontWeight.BOLD, 11));
        scoreBadge.setTextFill(Color.WHITE);
        scoreBadge.setAlignment(Pos.CENTER);
        scoreBadge.setPadding(new Insets(2, 6, 2, 6));
        scoreBadge.setStyle("-fx-background-color: " + scoreColor + "; -fx-background-radius: 8;");

        // Line 2: what needs to happen next (always computed dynamically)
        String nextAction = deriveNextStepMessage(customer);

        Label line2 = new Label(nextAction);
        line2.setFont(Font.font(11));
        if (isCustomerFailed(customer)) {
            line2.setTextFill(Color.web("#9E9E9E"));
        } else if (isCustomerApproved(customer)) {
            line2.setTextFill(Color.web("#5C6BC0"));
        } else {
            line2.setTextFill(needsAttention ? Color.web("#B71C1C") : Color.web(urgencyColor(customer)));
        }

        VBox lines = new VBox(1, line1, line2);
        lines.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lines, Priority.ALWAYS);

        // (docs button)
        Button docsBtn = new Button("Docs");
        docsBtn.setFont(Font.font("System", FontWeight.BOLD, 10));
        docsBtn.setMinSize(40, 24);
        docsBtn.setMaxSize(40, 24);
        docsBtn.setStyle("-fx-background-color: #455A64; -fx-text-fill: white; "
                + "-fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
        HBox.setMargin(docsBtn, new Insets(0, 16, 0, 0));
        Tooltip.install(docsBtn, new Tooltip("View documents"));
        docsBtn.setOnAction(e -> {
            if (documentListPopover.isShowing()) {
                documentListPopover.hide();
            } else {
                documentListPopover.show(customer.getId(), customer.getFullName(), docsBtn);
            }
        });

        HBox bannerContent = new HBox(8, scoreBadge, lines, docsBtn);
        bannerContent.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(bannerContent, Priority.ALWAYS);

        statusBanner.getChildren().setAll(bannerContent);
        statusBanner.setAlignment(Pos.CENTER_LEFT);
        statusBanner.setPadding(new Insets(6, 12, 6, 12));
        statusBanner.setPrefHeight(Region.USE_COMPUTED_SIZE);
        statusBanner.setMinHeight(Region.USE_COMPUTED_SIZE);
        statusBanner.setMaxHeight(Region.USE_COMPUTED_SIZE);
        statusBanner.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #DDD; -fx-border-width: 0 0 1 0;");
        statusBanner.setVisible(true);
        statusBanner.setManaged(true);
    }

    private String formatStage(OnboardingStage stage) {
        return stage.displayName();
    }

    private String urgencyColor(Customer customer) {
        long days = daysUntilTakeoff(customer);
        if (days < 0) return "#4CAF50";  // no takeoff date — assume fine
        int steps = remainingSteps(customer);
        if (steps == 0) return "#4CAF50"; // done — green
        long daysPerStep = days / steps;
        if (daysPerStep < 7) return "#B71C1C";   // red — critical
        if (daysPerStep < 14) return "#E65100";  // orange — tight
        return "#4CAF50";                         // green — on track
    }

    private String urgencyTint(Customer customer) {
        long days = daysUntilTakeoff(customer);
        if (days < 0) return "#F1F8E9";  // no takeoff date — light green
        int steps = remainingSteps(customer);
        if (steps == 0) return "#F1F8E9"; // done — light green
        long daysPerStep = days / steps;
        if (daysPerStep < 7) return "#FFEBEE";   // light red
        if (daysPerStep < 14) return "#FFF3E0";  // light orange
        return "#F1F8E9";                         // light green
    }

    private int remainingSteps(Customer customer) {
        return orchestrator.remainingSteps(customer.getId());
    }

    private int criticalitySort(Customer a, Customer b) {
        int groupA = sortGroup(a);
        int groupB = sortGroup(b);
        if (groupA != groupB) return Integer.compare(groupA, groupB);
        return Long.compare(daysUntilTakeoff(a), daysUntilTakeoff(b));
    }

    private int sortGroup(Customer c) {
        if (isCustomerFailed(c)) return 2;
        if (isCustomerApproved(c)) return 1;
        return 0;
    }

    private long daysUntilTakeoff(Customer customer) {
        if (customer.getTakeoffDate() == null) return -1;
        return ChronoUnit.DAYS.between(clock.today(), customer.getTakeoffDate());
    }

    private boolean isCustomerFailed(Customer customer) {
        return customer.getCurrentStage() == OnboardingStage.REJECTED
                || customer.getCurrentStage() == OnboardingStage.FAILED;
    }

    private boolean isCustomerApproved(Customer customer) {
        return customer.getCurrentStage() == OnboardingStage.APPROVED;
    }

    // OCP: UI-facing messages per stage — presentation concern, not extensible business logic
    private String deriveNextStepMessage(Customer customer) {
        String name = customer.getFullName();
        return switch (customer.getCurrentStage()) {
            case REGISTERED           -> "Send questionnaire to " + name;
            case QUESTIONNAIRE_SENT   -> "Waiting for questionnaire response — " + name;
            case QUESTIONNAIRE_COMPLETED -> "Schedule Initial Medical for " + name + " — toggle a doctor below and drag in calendar";
            case FIRST_MEDICAL_SCHEDULED -> "Awaiting Initial Medical result — " + name;
            case FIRST_MEDICAL_COMPLETED -> {
                List<AppointmentType> required = orchestrator.getRequiredNextAppointments(customer.getId());
                if (required.isEmpty()) {
                    yield "Schedule Space Training for " + name + " — toggle a trainer below and drag in calendar";
                }
                String types = required.stream().map(AppointmentType::displayName).reduce((a, b) -> a + ", " + b).orElse("");
                yield "Schedule " + types + " for " + name + " — toggle staff below, drag in calendar";
            }
            case SPECIALIST_SCHEDULED -> "Awaiting Specialist result — " + name;
            case SPECIALIST_COMPLETED -> {
                List<AppointmentType> remaining = orchestrator.getRequiredNextAppointments(customer.getId());
                if (remaining.isEmpty() || (remaining.size() == 1 && remaining.get(0) == AppointmentType.SPACE_TRAINING)) {
                    yield "All specialists cleared — schedule Space Training for " + name;
                }
                String types = remaining.stream().map(AppointmentType::displayName).reduce((a, b) -> a + ", " + b).orElse("");
                yield "Schedule remaining: " + types + " for " + name + " — toggle staff below, drag in calendar";
            }
            case SPACE_TRAINING_SCHEDULED -> "Awaiting Space Training result — " + name;
            case SPACE_TRAINING_COMPLETED -> "Schedule Final Medical for " + name + " — toggle a doctor below and drag in calendar";
            case FINAL_MEDICAL_SCHEDULED -> "Awaiting Final Medical result — " + name;
            case FINAL_MEDICAL_COMPLETED -> name + " — Final medical passed, approved for flight";
            case INDEMNITY_PENDING    -> "Waiting for indemnity signature — " + name;
            case INDEMNITY_SIGNED     -> {
                List<AppointmentType> next = orchestrator.getRequiredNextAppointments(customer.getId());
                if (next.isEmpty()) {
                    yield "Indemnity signed — " + name + " approved for flight";
                }
                String types = next.stream().map(AppointmentType::displayName).reduce((a, b) -> a + ", " + b).orElse("");
                yield "Indemnity signed — schedule " + types + " for " + name;
            }
            case APPOINTMENT_REFUSED  -> {
                AppointmentType refused = orchestrator.getLastRefusedAppointmentType(customer.getId());
                String refusedName = refused != null ? refused.displayName() : "appointment";
                yield name + " refused " + refusedName + " — offer indemnity agreement";
            }
            case APPROVED             -> name + " — Approved for flight";
            case REJECTED             -> name + " — Not approved for flight";
            case FAILED               -> name + " — Onboarding failed";
        };
    }

    // OCP: only 2 stages need action buttons; a pattern for 2 cases adds complexity without benefit
    private void buildActionButtons(Customer customer) {
        OnboardingStage stage = customer.getCurrentStage();
        UUID id = customer.getId();
        String uc = urgencyColor(customer);

        switch (stage) {
            case REGISTERED -> {
                addButton("Send Questionnaire", uc, () -> {
                    orchestrator.sendQuestionnaire(id);
                    refreshAfterAction(id);
                });
            }
            case APPOINTMENT_REFUSED -> {
                addButton("Offer Indemnity Agreement", uc, () -> {
                    orchestrator.offerIndemnityAgreement(id);
                    refreshAfterAction(id);
                });
            }
            default -> {}
        }
    }

    private void addButton(String text, String color, Runnable action) {
        Button btn = new Button(text);
        btn.setFont(Font.font(11));
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white;");
        btn.setOnAction(e -> action.run());
        actionBar.getChildren().add(btn);
    }

    // -------------------------------------------------------------------------
    // Drag scheduling
    // -------------------------------------------------------------------------

    private void onDragComplete(Staff staff, LocalDateTime start, LocalDateTime end) {
        Customer customer = customerList.getSelectionModel().getSelectedItem();
        if (customer == null) return;

        List<Appointment> existing = orchestrator.getAppointmentsForCustomer(customer.getId());
        existing.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.SUGGESTED)
                .forEach(a -> orchestrator.cancelScheduledAppointment(customer.getId(), a.getId()));

        List<AppointmentType> required = orchestrator.getRequiredNextAppointments(customer.getId());

        OnboardingStage stage = customer.getCurrentStage();
        if (required.isEmpty() && stage == OnboardingStage.QUESTIONNAIRE_COMPLETED) {
            required = List.of(AppointmentType.INITIAL_MEDICAL);
        }

        List<AppointmentType> handleable = required.stream()
                .filter(type -> staffResolver.canHandle(type, staff))
                .toList();

        if (handleable.isEmpty()) {
            showEvent(staff.getName() + " cannot handle any required appointment for "
                    + customer.getFullName() + " at this stage.");
            return;
        }

        AppointmentType chosenType;
        if (handleable.size() == 1) {
            chosenType = handleable.get(0);
        } else {
            ChoiceDialog<AppointmentType> dialog = new ChoiceDialog<>(handleable.get(0), handleable);
            dialog.setTitle("Appointment Type");
            dialog.setHeaderText("Select appointment type for " + customer.getFullName());
            dialog.setContentText("Type:");
            var result = dialog.showAndWait();
            if (result.isEmpty()) return;
            chosenType = result.get();
        }

        try {
            orchestrator.scheduleAppointmentByDrag(customer.getId(), staff.getId(), start, end, chosenType);
        } catch (CustomerNotAvailableException ex) {
            showEvent("Cannot schedule: " + ex.getMessage());
            return;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showEvent("Cannot schedule: " + ex.getMessage());
            return;
        }
        refreshAfterAction(customer.getId());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void selectCustomer(Customer customer) {
        customerList.getSelectionModel().select(customer);
    }

    private void showEvent(String message) {
        eventLog.setText(message);
        eventLogClearTimer.playFromStart();
    }

    private void showPersistentEvent(String message) {
        eventLogClearTimer.stop();
        eventLog.setText(message);
    }

    private void refreshAfterAction(UUID customerId) {
        refreshCustomerList();
        orchestrator.getAllCustomers().stream()
                .filter(c -> c.getId().equals(customerId))
                .findFirst()
                .ifPresent(fresh -> {
                    customerList.getSelectionModel().select(fresh);
                    calendarView.setSelectedCustomer(fresh);
                    rebuildStatusBanner(fresh);
                    rebuildActionBar(fresh);
                });
    }

    private void refreshCustomerList() {
        Customer selected = customerList.getSelectionModel().getSelectedItem();
        List<Customer> all = new ArrayList<>(orchestrator.getAllCustomers());
        all.sort(this::criticalitySort);
        suppressEventLogClear = true;
        customerList.getItems().setAll(all);
        if (selected != null) {
            all.stream().filter(c -> c.getId().equals(selected.getId()))
                    .findFirst()
                    .ifPresent(c -> customerList.getSelectionModel().select(c));
        }
        suppressEventLogClear = false;
    }

    private void refreshCalendar() {
        Customer sel = customerList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            orchestrator.getAllCustomers().stream()
                    .filter(c -> c.getId().equals(sel.getId()))
                    .findFirst()
                    .ifPresent(fresh -> {
                        calendarView.setSelectedCustomer(fresh);
                        rebuildStatusBanner(fresh);
                        rebuildActionBar(fresh);
                    });
        } else {
            calendarView.refresh();
        }
    }

    private void updateWeekLabel() {
        String from = calendarView.getWeekStart().format(WEEK_FMT);
        String to   = calendarView.getWeekStart().plusDays(6).format(WEEK_FMT);
        weekLabel.setText(from + " – " + to);
    }

    private void updateSimDayLabel() {
        simDayLabel.setText("Today: " + clock.today().format(DAY_FMT));
    }

    /**
     * Simulates all CONFIRMED appointments on the current simulated day,
     * then advances the clock by one day and scrolls the calendar to that day's week.
     */
    private void simulateNextDay() {
        processAppointmentsUpTo(clock.today(), LocalTime.of(23, 59));
        String dayStr = clock.today().format(DAY_FMT);
        clock.advanceOneDay();
        simulationAdvancer.resetDay();
        if (autoSpawnEnabled) customerSpawner.spawnDaily();
        updateSimDayLabel();
        calendarView.showDateIfFollowing(clock.today());
        calendarView.updateTimeIndicator();
        updateWeekLabel();
        showEvent("Day advanced past " + dayStr + ".");
        Customer sel = customerList.getSelectionModel().getSelectedItem();
        if (sel != null) refreshAfterAction(sel.getId());
        else refreshCustomerList();
    }

    private void tickSimulation() {
        LocalTime oldTime = clock.time();
        LocalTime newTime = oldTime.plusSeconds(144);

        if (newTime.isBefore(oldTime)) {
            processAppointmentsUpTo(clock.today(), LocalTime.of(23, 59));
            clock.advanceOneDay();
            simulationAdvancer.resetDay();
            if (autoSpawnEnabled) customerSpawner.spawnDaily();
            updateSimDayLabel();
            calendarView.showDateIfFollowing(clock.today());
            updateWeekLabel();
            refreshCustomerList();
        } else {
            clock.advanceTimeTo(newTime);
            processAppointmentsUpTo(clock.today(), newTime);
        }
        calendarView.updateTimeIndicator();
    }

    private void processAppointmentsUpTo(LocalDate date, LocalTime cutoff) {
        boolean anyProcessed = simulationAdvancer.processAppointmentsUpTo(date, cutoff);
        if (anyProcessed) {
            Customer sel = customerList.getSelectionModel().getSelectedItem();
            if (sel != null) refreshAfterAction(sel.getId());
            else refreshCustomerList();
        }
    }

    private boolean isScheduledStage(OnboardingStage stage) {
        return orchestrator.isScheduledStage(stage);
    }

    private String formatRole(StaffRole role) {
        return role.displayName();
    }

    private String formatResourceTag(String tag) {
        if (tag == null) return "";
        return switch (tag) {
            case "MEDICAL_ROOM"          -> "Medical";
            case "SHUTTLE_ROOM"          -> "Shuttle Sim";
            case "VR_HEADSET"            -> "VR Headset";
            case "TRANSLATION_HEADPHONE" -> "Headphone";
            default -> tag;
        };
    }

    private String formatAppointmentType(AppointmentType type) {
        return type.displayName();
    }

    // -------------------------------------------------------------------------
    // Customer list cell with attention badge + takeoff countdown
    // -------------------------------------------------------------------------

    private class CustomerCell extends ListCell<Customer> {

        CustomerCell() {
            addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY
                        && !isEmpty() && getItem() != null
                        && getItem().equals(getListView().getSelectionModel().getSelectedItem())) {
                    getListView().getSelectionModel().clearSelection();
                    e.consume();
                }
            });
        }

        @Override
        protected void updateItem(Customer c, boolean empty) {
            super.updateItem(c, empty);
            if (empty || c == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
                return;
            }

            Label nameLbl = new Label(c.getFullName());
            nameLbl.setFont(Font.font("System", FontWeight.BOLD, 12));

            String lang = c.getPreferredLanguage();

            String subText = deriveNextAppointmentText(c);
            Label subLbl = new Label(subText);
            subLbl.setFont(Font.font(10));

            boolean failed = isCustomerFailed(c);
            boolean approved = isCustomerApproved(c);

            if (failed) {
                nameLbl.setStyle("-fx-text-fill: #9E9E9E;");
                subLbl.setStyle("-fx-text-fill: #BDBDBD;");
            } else if (approved) {
                nameLbl.setStyle("-fx-text-fill: #3949AB;");
                subLbl.setStyle("-fx-text-fill: #5C6BC0;");
            } else {
                nameLbl.setStyle("-fx-text-fill: #1A1A1A;");
                subLbl.setStyle("-fx-text-fill: #555555;");
            }

            HBox nameRow = new HBox(4, nameLbl);
            nameRow.setAlignment(Pos.CENTER_LEFT);
            if (!"EN".equals(lang)) {
                Label langBadge = new Label(lang);
                langBadge.setFont(Font.font("System", FontWeight.BOLD, 8));
                langBadge.setTextFill(Color.WHITE);
                langBadge.setPadding(new Insets(1, 3, 1, 3));
                langBadge.setStyle("-fx-background-color: #5C6BC0; -fx-background-radius: 3;");
                nameRow.getChildren().add(langBadge);
            }

            VBox info = new VBox(2, nameRow, subLbl);
            HBox.setHgrow(info, Priority.ALWAYS);

            // Right-side badges: takeoff countdown + attention
            HBox badges = new HBox(4);
            badges.setAlignment(Pos.CENTER_RIGHT);

            if (failed) {
                Label failedBadge = new Label("Failed");
                failedBadge.setFont(Font.font("System", FontWeight.BOLD, 10));
                failedBadge.setTextFill(Color.WHITE);
                failedBadge.setAlignment(Pos.CENTER);
                failedBadge.setPadding(new Insets(2, 5, 2, 5));
                failedBadge.setStyle("-fx-background-color: #9E9E9E; -fx-background-radius: 8;");
                Tooltip.install(failedBadge, new Tooltip("Customer cannot complete onboarding"));
                badges.getChildren().add(failedBadge);
            } else if (approved) {
                Label readyBadge = new Label("🚀 Ready");
                readyBadge.setFont(Font.font("System", FontWeight.BOLD, 10));
                readyBadge.setTextFill(Color.WHITE);
                readyBadge.setAlignment(Pos.CENTER);
                readyBadge.setPadding(new Insets(2, 5, 2, 5));
                readyBadge.setStyle("-fx-background-color: #5C6BC0; -fx-background-radius: 8;");
                Tooltip.install(readyBadge, new Tooltip("Approved for flight"));
                badges.getChildren().add(readyBadge);
            } else {
                // Takeoff countdown badge
                long daysLeft = daysUntilTakeoff(c);
                if (daysLeft >= 0) {
                    String takeoffColor = urgencyColor(c);
                    Label takeoffBadge = new Label(daysLeft + "d");
                    takeoffBadge.setFont(Font.font("System", FontWeight.BOLD, 10));
                    takeoffBadge.setTextFill(Color.WHITE);
                    takeoffBadge.setAlignment(Pos.CENTER);
                    takeoffBadge.setPadding(new Insets(2, 5, 2, 5));
                    takeoffBadge.setStyle("-fx-background-color: " + takeoffColor
                            + "; -fx-background-radius: 8;");
                    Tooltip.install(takeoffBadge, new Tooltip("Takeoff in " + daysLeft + " days"));
                    badges.getChildren().add(takeoffBadge);
                }
            }

            // Attention badge
            if (c.isNeedsAttention() && !failed && !approved) {
                String reason = c.getAttentionReason() != null ? c.getAttentionReason() : "";
                String badgeColor = urgencyColor(c);

                Label badge = new Label("!");
                badge.setFont(Font.font("System", FontWeight.BOLD, 14));
                badge.setTextFill(Color.WHITE);
                badge.setAlignment(Pos.CENTER);
                badge.setMinSize(22, 22);
                badge.setMaxSize(22, 22);
                badge.setStyle("-fx-background-color: " + badgeColor + "; -fx-background-radius: 11;");
                Tooltip.install(badge, new Tooltip(reason.isEmpty() ? "Needs attention" : reason));
                badges.getChildren().add(badge);
            }

            HBox row = new HBox(8, info, badges);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4));
            String rowTint = failed ? "#F5F5F5" : approved ? "#E8EAF6" : urgencyTint(c);
            row.setStyle("-fx-background-color: " + rowTint + "; -fx-background-radius: 4;");
            setGraphic(row);
            setStyle("");
            setText(null);
        }

        private String deriveNextAppointmentText(Customer c) {
            List<Appointment> appts = orchestrator.getAppointmentsForCustomer(c.getId());
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE dd MMM");

            return appts.stream()
                    .filter(a -> (a.getStatus() == AppointmentStatus.CONFIRMED
                               || a.getStatus() == AppointmentStatus.SUGGESTED
                               || a.getStatus() == AppointmentStatus.SENT)
                               && a.getScheduledAt().isAfter(now))
                    .min(Comparator.comparing(Appointment::getScheduledAt))
                    .map(a -> "Next: " + formatAppointmentType(a.getType())
                            + " – " + a.getScheduledAt().format(fmt))
                    .orElseGet(() -> c.isNeedsAttention() ? "Waiting – PM action needed" : "");
        }
    }
}
