package de.spacemate.ui;

import de.spacemate.app.SimulatedClock;
import de.spacemate.model.*;
import de.spacemate.orchestration.OnboardingOrchestrator;
import de.spacemate.repository.StaffRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;

public class WeekCalendarView extends BorderPane {

    private static final int FIRST_HOUR  = 0;
    private static final int LAST_HOUR   = 23;
    private static final int HOUR_HEIGHT = 60;
    private static final int COL_WIDTH   = 120;

    private static final String CELL_STYLE =
            "-fx-border-color: #CCCCCC; -fx-border-width: 0 1 1 0;";
    private static final String CELL_UNAVAILABLE_STYLE =
            "-fx-border-color: #CCCCCC; -fx-border-width: 0 1 1 0; -fx-background-color: #EEEEEE;";
    private static final String HEADER_STYLE =
            "-fx-border-color: #CCCCCC; -fx-border-width: 0 1 1 0; -fx-background-color: #F5F5F5;";
    private static final String TODAY_HEADER_STYLE =
            "-fx-border-color: #CCCCCC; -fx-border-width: 0 1 1 0; -fx-background-color: #4A90D9;";
    private static final String TAKEOFF_HEADER_STYLE =
            "-fx-border-color: #CCCCCC; -fx-border-width: 0 1 1 0; -fx-background-color: #E53935;";

    private static final Color COLOR_FREE_SLOT = Color.web("#A8D5A2");
    private static final Color COLOR_SUGGESTED = Color.web("#F5A623");
    private static final Color COLOR_SENT = Color.web("#42A5F5");
    private static final Color COLOR_CONFIRMED = Color.web("#7ED321");
    private static final Color COLOR_COMPLETED = Color.web("#9B9B9B");

    private final OnboardingOrchestrator orchestrator;
    private final StaffRepository staffRepository;
    private final Runnable onSelectionChanged;
    private final SimulatedClock clock;

    @FunctionalInterface
    public interface DragScheduleHandler {
        void onDragComplete(Staff staff, LocalDateTime start, LocalDateTime end);
    }

    private DragScheduleHandler dragScheduleHandler;
    private Consumer<Customer> customerClickHandler;

    private LocalDate weekStart;
    private boolean followClock = true;
    private Customer selectedCustomer;
    private final Set<UUID> activeStaffIds = new LinkedHashSet<>();
    private final Set<UUID> activeResourceIds = new LinkedHashSet<>();
    private final GridPane grid = new GridPane();
    private final GridPane headerBar = new GridPane();
    private final Line timeIndicatorLine = new Line();
    private boolean showCompleted = false;

    public void setShowCompleted(boolean show) {
        this.showCompleted = show;
        refresh();
    }
    private Pane overlay;

    // Drag state
    private boolean isDragging = false;
    private LocalDateTime dragStart;
    private LocalDateTime dragEnd;
    private Staff dragStaff;
    private LocalDateTime dragWindowEnd;
    private Rectangle dragPreview;

    private AppointmentDetailPopover detailPopover;
    private UUID popoverAppointmentId;
    private long popoverHideTime;

    public WeekCalendarView(OnboardingOrchestrator orchestrator,
                            StaffRepository staffRepository,
                            SimulatedClock clock,
                            Runnable onSelectionChanged) {
        this.orchestrator = orchestrator;
        this.staffRepository = staffRepository;
        this.clock = clock;
        this.onSelectionChanged = onSelectionChanged;
        this.weekStart = clock.today().with(DayOfWeek.MONDAY);

        timeIndicatorLine.setStroke(Color.RED);
        timeIndicatorLine.setStrokeWidth(2);
        timeIndicatorLine.setMouseTransparent(true);
        timeIndicatorLine.setVisible(false);
        timeIndicatorLine.setViewOrder(-1);

        buildGrid();
        buildHeaderBar();
        setTop(headerBar);
        setCenter(buildScrollable());
        refresh();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void setDragScheduleHandler(DragScheduleHandler handler) {
        this.dragScheduleHandler = handler;
    }

    public void setCustomerClickHandler(Consumer<Customer> handler) {
        this.customerClickHandler = handler;
    }

    public void setSelectedCustomer(Customer customer) {
        this.selectedCustomer = customer;
        refresh();
    }

    public void setStaffVisible(UUID staffId, boolean visible) {
        if (visible) activeStaffIds.add(staffId);
        else activeStaffIds.remove(staffId);
        refresh();
    }

    public void setResourceVisible(UUID resourceId, boolean visible) {
        if (visible) activeResourceIds.add(resourceId);
        else activeResourceIds.remove(resourceId);
        refresh();
    }

    public void goToPreviousWeek() {
        weekStart = weekStart.minusWeeks(1);
        followClock = false;
        refresh();
    }

    public void goToNextWeek() {
        weekStart = weekStart.plusWeeks(1);
        followClock = false;
        refresh();
    }

    public LocalDate getWeekStart() { return weekStart; }

    public void showDate(LocalDate date) {
        weekStart = date.with(DayOfWeek.MONDAY);
        followClock = true;
        refresh();
    }

    public void showDateIfFollowing(LocalDate date) {
        if (followClock) showDate(date);
    }

    public void updateTimeIndicator() {
        LocalTime time = clock.time();
        int hour = time.getHour();
        int minute = time.getMinute();

        if (hour < FIRST_HOUR || hour > LAST_HOUR) {
            timeIndicatorLine.setVisible(false);
            return;
        }

        double yOffset = (hour - FIRST_HOUR) * HOUR_HEIGHT + (minute / 60.0) * HOUR_HEIGHT;
        double xStart = 55;
        double xEnd = 55 + 7 * COL_WIDTH;

        timeIndicatorLine.setStartX(xStart);
        timeIndicatorLine.setStartY(yOffset);
        timeIndicatorLine.setEndX(xEnd);
        timeIndicatorLine.setEndY(yOffset);
        timeIndicatorLine.setVisible(true);
    }

    public void refresh() {
        grid.getChildren().clear();
        overlay.getChildren().removeIf(n -> n != grid && n != timeIndicatorLine);

        Set<LocalDateTime> customerFree = selectedCustomer != null
                ? orchestrator.getCustomerAvailability(selectedCustomer.getId())
                : Collections.emptySet();

        drawEmptyCells(customerFree);
        drawTimeLabels();
        drawHeaders();
        renderStaffAvailability(customerFree);
        renderResourceAvailability(customerFree);
        renderAppointments();
        updateTimeIndicator();
    }

    // -------------------------------------------------------------------------
    // Grid construction
    // -------------------------------------------------------------------------

    private void buildGrid() {
        grid.setStyle("-fx-background-color: white;");

        ColumnConstraints timeCol = new ColumnConstraints(55);
        grid.getColumnConstraints().add(timeCol);
        for (int d = 0; d < 7; d++) {
            ColumnConstraints dc = new ColumnConstraints(COL_WIDTH);
            dc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(dc);
        }

        for (int h = FIRST_HOUR; h <= LAST_HOUR; h++) {
            RowConstraints rc = new RowConstraints(HOUR_HEIGHT);
            grid.getRowConstraints().add(rc);
        }
    }

    private void buildHeaderBar() {
        headerBar.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #CCCCCC; -fx-border-width: 0 0 1 0;");
        ColumnConstraints timeCol = new ColumnConstraints(55);
        headerBar.getColumnConstraints().add(timeCol);
        for (int d = 0; d < 7; d++) {
            ColumnConstraints dc = new ColumnConstraints(COL_WIDTH);
            dc.setHgrow(Priority.ALWAYS);
            headerBar.getColumnConstraints().add(dc);
        }
        RowConstraints row = new RowConstraints(32);
        headerBar.getRowConstraints().add(row);
    }

    private javafx.scene.control.ScrollPane buildScrollable() {
        overlay = new Pane(grid, timeIndicatorLine);
        grid.prefWidthProperty().bind(overlay.widthProperty());
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(overlay);
        sp.setFitToWidth(true);
        sp.setFitToHeight(false);
        return sp;
    }

    // -------------------------------------------------------------------------
    // Cell rendering
    // -------------------------------------------------------------------------

    private void drawEmptyCells(Set<LocalDateTime> customerFree) {
        LocalDateTime now = clock.now();
        for (int h = FIRST_HOUR; h <= LAST_HOUR; h++) {
            int row = hourRow(h);
            Pane timeCell = new Pane();
            timeCell.setStyle(CELL_STYLE);
            grid.add(timeCell, 0, row);

            for (int d = 1; d <= 7; d++) {
                LocalDate day = weekStart.plusDays(d - 1);
                LocalDateTime cellTime = day.atTime(h, 0);
                LocalDateTime cellEnd = cellTime.plusHours(1);

                boolean topPast = !cellTime.plusMinutes(30).isAfter(now);
                boolean bottomPast = !cellEnd.isAfter(now);

                boolean topUnavail = topPast || (!customerFree.isEmpty() && !customerFree.contains(cellTime));
                boolean bottomUnavail = bottomPast || (!customerFree.isEmpty() && !customerFree.contains(cellTime.plusMinutes(30)));

                if (topUnavail != bottomUnavail) {
                    Pane topHalf = new Pane();
                    topHalf.setStyle(topUnavail ? CELL_UNAVAILABLE_STYLE : CELL_STYLE);
                    Pane bottomHalf = new Pane();
                    bottomHalf.setStyle(bottomUnavail ? CELL_UNAVAILABLE_STYLE : CELL_STYLE);
                    VBox split = new VBox(topHalf, bottomHalf);
                    VBox.setVgrow(topHalf, Priority.ALWAYS);
                    VBox.setVgrow(bottomHalf, Priority.ALWAYS);
                    split.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                    split.setStyle("-fx-border-color: #CCCCCC; -fx-border-width: 0 1 1 0;");
                    grid.add(split, d, row);
                } else {
                    Pane cell = new Pane();
                    cell.setStyle(topUnavail ? CELL_UNAVAILABLE_STYLE : CELL_STYLE);
                    cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                    grid.add(cell, d, row);
                }
            }
        }
    }

    private void drawHeaders() {
        headerBar.getChildren().clear();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE dd");
        LocalDate takeoff = selectedCustomer != null ? selectedCustomer.getTakeoffDate() : null;

        Pane corner = new Pane();
        corner.setStyle(HEADER_STYLE);
        headerBar.add(corner, 0, 0);

        for (int d = 0; d < 7; d++) {
            LocalDate day = weekStart.plusDays(d);
            boolean isToday   = day.equals(clock.today());
            boolean isTakeoff = day.equals(takeoff);

            String text = day.format(dayFmt) + (isTakeoff ? " 🚀" : "");
            Label lbl = new Label(text);
            lbl.setFont(Font.font("System", FontWeight.BOLD, 12));
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setMaxHeight(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            lbl.setPadding(new Insets(4));

            if (isTakeoff) {
                lbl.setStyle(TAKEOFF_HEADER_STYLE);
                lbl.setTextFill(Color.WHITE);
            } else if (isToday) {
                lbl.setStyle(TODAY_HEADER_STYLE);
                lbl.setTextFill(Color.WHITE);
            } else {
                lbl.setStyle(HEADER_STYLE);
            }
            headerBar.add(lbl, d + 1, 0);
        }
    }

    private void drawTimeLabels() {
        for (int h = FIRST_HOUR; h <= LAST_HOUR; h++) {
            Label lbl = new Label(String.format("%02d:00", h));
            lbl.setFont(Font.font(10));
            lbl.setPadding(new Insets(2, 4, 0, 2));
            lbl.setAlignment(Pos.TOP_RIGHT);
            lbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            lbl.setStyle(CELL_STYLE);
            grid.add(lbl, 0, hourRow(h));
        }
    }

    // -------------------------------------------------------------------------
    // Staff availability zones (green) + drag interaction
    // -------------------------------------------------------------------------

    private void renderStaffAvailability(Set<LocalDateTime> customerFree) {
        if (activeStaffIds.isEmpty()) return;

        for (UUID staffId : activeStaffIds) {
            staffRepository.findById(staffId).ifPresent(staff -> {
                List<StaffAvailability> windows = orchestrator.getStaffAvailability(staffId);
                for (StaffAvailability window : windows) {
                    LocalDate windowDay = window.getStart().toLocalDate();
                    int col = dayColumn(windowDay);
                    if (col < 1) continue;

                    List<TimeSlot> booked = orchestrator.getBookedSlotsByStaffAndDate(staffId, windowDay);
                    renderFreeSegments(staff, window, booked, col, customerFree);
                }
            });
        }
    }

    private void renderResourceAvailability(Set<LocalDateTime> customerFree) {
        if (activeResourceIds.isEmpty()) return;

        for (UUID resourceId : activeResourceIds) {
            List<ResourceAvailability> windows = orchestrator.getResourceAvailability(resourceId);
            for (ResourceAvailability window : windows) {
                LocalDate windowDay = window.getStart().toLocalDate();
                int col = dayColumn(windowDay);
                if (col < 1) continue;

                LocalDateTime now = clock.now();
                LocalDateTime windowStart = window.getStart();
                LocalDateTime windowEnd = window.getEnd();
                if (!windowEnd.isAfter(now)) continue;
                if (windowStart.isBefore(now)) windowStart = now;

                Color zoneColor = window.getResource().getCategory() == ResourceCategory.ROOM
                        ? Color.web("#90CAF9", 0.3)
                        : Color.web("#CE93D8", 0.3);

                List<ResourceAssignment> bookings = orchestrator.getBookedResourceSlots(resourceId, windowDay);
                List<LocalDateTime[]> freeSegments = computeFreeSegments(windowStart, windowEnd, bookings);

                for (LocalDateTime[] seg : freeSegments) {
                    placeResourceSegments(seg[0], seg[1], col, zoneColor, customerFree);
                }
            }
        }
    }

    private void placeResourceSegments(LocalDateTime start, LocalDateTime end, int col,
                                        Color zoneColor, Set<LocalDateTime> customerFree) {
        if (!customerFree.isEmpty()) {
            LocalDateTime segStart = null;
            LocalDateTime t = start;
            while (!t.isAfter(end)) {
                boolean free = customerFree.contains(t);
                if (free && segStart == null) {
                    segStart = t;
                } else if (!free && segStart != null) {
                    placeResourceRect(segStart, t, col, zoneColor);
                    segStart = null;
                }
                t = t.plusMinutes(30);
            }
            if (segStart != null) {
                placeResourceRect(segStart, end, col, zoneColor);
            }
        } else {
            placeResourceRect(start, end, col, zoneColor);
        }
    }

    private void placeResourceRect(LocalDateTime start, LocalDateTime end, int col, Color zoneColor) {
        double yStart = timeToY(start);
        double yEnd = timeToY(end);
        double height = yEnd - yStart;
        if (height < 3) return;

        double x = 55 + (col - 1) * COL_WIDTH + 1;
        double width = COL_WIDTH - 2;

        Region zone = new Region();
        zone.setBackground(new Background(new BackgroundFill(
                zoneColor, new CornerRadii(2), Insets.EMPTY)));
        zone.setLayoutX(x);
        zone.setLayoutY(yStart);
        zone.setPrefSize(width, height);
        zone.setMinSize(width, height);
        zone.setMaxSize(width, height);
        zone.setMouseTransparent(true);
        overlay.getChildren().add(zone);
    }

    private List<LocalDateTime[]> computeFreeSegments(LocalDateTime start, LocalDateTime end,
                                                       List<ResourceAssignment> bookings) {
        if (bookings.isEmpty()) {
            List<LocalDateTime[]> single = new ArrayList<>();
            single.add(new LocalDateTime[]{start, end});
            return single;
        }

        List<LocalDateTime[]> bookedSlots = bookings.stream()
                .map(b -> {
                    Appointment appt = orchestrator.getAppointmentById(b.getAppointmentId());
                    if (appt == null || appt.getStatus() == AppointmentStatus.CANCELLED) return null;
                    return new LocalDateTime[]{appt.getTimeSlot().getStart(), appt.getTimeSlot().getEnd()};
                })
                .filter(java.util.Objects::nonNull)
                .sorted((a, b2) -> a[0].compareTo(b2[0]))
                .toList();

        List<LocalDateTime[]> free = new java.util.ArrayList<>();
        LocalDateTime cursor = start;
        for (LocalDateTime[] booked : bookedSlots) {
            if (booked[0].isAfter(cursor)) {
                free.add(new LocalDateTime[]{cursor, booked[0].isBefore(end) ? booked[0] : end});
            }
            if (booked[1].isAfter(cursor)) {
                cursor = booked[1];
            }
        }
        if (cursor.isBefore(end)) {
            free.add(new LocalDateTime[]{cursor, end});
        }
        return free;
    }

    private void renderFreeSegments(Staff staff, StaffAvailability window,
                                     List<TimeSlot> bookedSlots, int col,
                                     Set<LocalDateTime> customerFree) {
        List<TimeSlot> sorted = bookedSlots.stream()
                .filter(s -> s.getStart().isBefore(window.getEnd()) && s.getEnd().isAfter(window.getStart()))
                .sorted(Comparator.comparing(TimeSlot::getStart))
                .toList();

        LocalDateTime freeStart = window.getStart();
        for (TimeSlot booked : sorted) {
            if (freeStart.isBefore(booked.getStart())) {
                drawGreenZone(staff, freeStart, booked.getStart(), col, customerFree, window.getEnd());
            }
            if (booked.getEnd().isAfter(freeStart)) {
                freeStart = booked.getEnd();
            }
        }
        if (freeStart.isBefore(window.getEnd())) {
            drawGreenZone(staff, freeStart, window.getEnd(), col, customerFree, window.getEnd());
        }
    }

    private void drawGreenZone(Staff staff, LocalDateTime start, LocalDateTime end,
                                int col, Set<LocalDateTime> customerFree,
                                LocalDateTime windowEnd) {
        // If customer has availability, intersect: only draw sub-segments where customer is free
        if (!customerFree.isEmpty()) {
            LocalDateTime segStart = null;
            LocalDateTime t = start;
            while (!t.isAfter(end)) {
                boolean free = customerFree.contains(t);
                if (free && segStart == null) {
                    segStart = t;
                } else if (!free && segStart != null) {
                    placeGreenRect(staff, segStart, t, col, windowEnd);
                    segStart = null;
                }
                t = t.plusMinutes(30);
            }
            if (segStart != null) {
                placeGreenRect(staff, segStart, end, col, windowEnd);
            }
        } else {
            placeGreenRect(staff, start, end, col, windowEnd);
        }
    }

    private void placeGreenRect(Staff staff, LocalDateTime start, LocalDateTime end,
                                 int col, LocalDateTime windowEnd) {
        LocalDateTime now = clock.now();
        if (!end.isAfter(now)) return;
        if (start.isBefore(now)) start = now;

        double yStart = timeToY(start);
        double yEnd = timeToY(end);
        double height = yEnd - yStart;
        if (height < 5) return;

        double x = 55 + (col - 1) * COL_WIDTH + 1;
        double width = COL_WIDTH - 2;

        Color zoneColor = staffZoneColor(staff.getRole());

        Region zone = new Region();
        zone.setBackground(new Background(new BackgroundFill(
                zoneColor, new CornerRadii(2), Insets.EMPTY)));
        zone.setLayoutX(x);
        zone.setLayoutY(yStart);
        zone.setPrefSize(width, height);
        zone.setMinSize(width, height);
        zone.setMaxSize(width, height);
        zone.setCursor(Cursor.CROSSHAIR);

        attachDragListeners(zone, staff, start, end, col, windowEnd);
        overlay.getChildren().add(zone);
    }

    // OCP: UI color mapping — presentation concern, not business logic
    private Color staffZoneColor(StaffRole role) {
        return switch (role) {
            case EYE_SPECIALIST, CARDIOLOGIST, NEUROLOGIST, ORTHOPEDIST, PSYCHOLOGIST ->
                    Color.web("#FFB74D", 0.4);
            case SPACE_TRAINER ->
                    Color.web("#4DD0E1", 0.4);
            default ->
                    COLOR_FREE_SLOT.deriveColor(0, 1, 1, 0.4);
        };
    }

    private void attachDragListeners(Region zone, Staff staff,
                                      LocalDateTime zoneStart, LocalDateTime zoneEnd,
                                      int col, LocalDateTime windowEnd) {
        zone.setOnMousePressed(e -> {
            if (selectedCustomer == null || dragScheduleHandler == null) return;
            LocalDate day = weekStart.plusDays(col - 1);
            double absoluteY = zone.getLayoutY() + e.getY();
            dragStart = yToSnappedTime(absoluteY, day);
            if (dragStart.isBefore(zoneStart)) dragStart = zoneStart;
            if (dragStart.isBefore(clock.now())) return;
            dragEnd = dragStart.plusMinutes(30);
            dragStaff = staff;
            dragWindowEnd = zoneEnd;
            isDragging = true;

            dragPreview = new Rectangle();
            dragPreview.setFill(Color.web("#4A90D9", 0.35));
            dragPreview.setStroke(Color.web("#4A90D9"));
            dragPreview.setStrokeWidth(1.5);
            dragPreview.setArcWidth(4);
            dragPreview.setArcHeight(4);
            dragPreview.setMouseTransparent(true);
            overlay.getChildren().add(dragPreview);
            updateDragPreview(col);
            e.consume();
        });

        zone.setOnMouseDragged(e -> {
            if (!isDragging) return;
            LocalDate day = weekStart.plusDays(col - 1);
            double absoluteY = zone.getLayoutY() + e.getY();
            LocalDateTime proposed = yToSnappedTime(absoluteY, day);
            if (proposed.isAfter(dragWindowEnd)) proposed = dragWindowEnd;
            if (proposed.isBefore(dragStart.plusMinutes(30))) proposed = dragStart.plusMinutes(30);
            dragEnd = proposed;
            updateDragPreview(col);
            e.consume();
        });

        zone.setOnMouseReleased(e -> {
            if (!isDragging) return;
            isDragging = false;
            if (dragPreview != null) {
                overlay.getChildren().remove(dragPreview);
                dragPreview = null;
            }
            if (dragScheduleHandler != null && dragStaff != null) {
                dragScheduleHandler.onDragComplete(dragStaff, dragStart, dragEnd);
            }
            dragStaff = null;
            e.consume();
        });
    }

    private void updateDragPreview(int col) {
        if (dragPreview == null) return;
        double yStart = timeToY(dragStart);
        double yEnd = timeToY(dragEnd);
        double x = 55 + (col - 1) * COL_WIDTH + 1;
        double width = COL_WIDTH - 2;

        dragPreview.setX(x);
        dragPreview.setY(yStart);
        dragPreview.setWidth(width);
        dragPreview.setHeight(yEnd - yStart);
    }

    // -------------------------------------------------------------------------
    // Appointments rendering (variable height)
    // -------------------------------------------------------------------------

    private void renderAppointments() {
        if (selectedCustomer != null) {
            renderCustomerAppointments();
        } else {
            renderAllAppointments();
        }
    }

    private void renderCustomerAppointments() {
        List<Appointment> appointments = orchestrator.getAppointmentsForCustomer(selectedCustomer.getId());
        for (Appointment appt : appointments) {
            if (appt.getStatus() == AppointmentStatus.CANCELLED) continue;

            LocalDate apptDay = appt.getScheduledAt().toLocalDate();
            int col = dayColumn(apptDay);
            if (col < 1) continue;

            Color color = colorForStatus(appt.getStatus());
            boolean incompleteResources = appt.getStatus() == AppointmentStatus.CONFIRMED
                    && !orchestrator.hasRequiredResources(appt.getId());

            String borderStyle = resourceBorderStyle(incompleteResources, apptDay);

            VBox block = appointmentBlock(appt, color);
            if (borderStyle != null) {
                block.setStyle(block.getStyle() + borderStyle);
            }
            block.setCursor(Cursor.HAND);
            block.setOnMousePressed(e -> e.consume());
            block.setOnMouseClicked(e -> {
                showPopover(appt, block);
                e.consume();
            });
            placeAppointmentBlock(block, appt, col);
        }
    }

    private void renderAllAppointments() {
        List<Appointment> appointments = orchestrator.getAllAppointments();
        for (Appointment appt : appointments) {
            if (appt.getStatus() == AppointmentStatus.CANCELLED) continue;
            if (appt.getStatus() == AppointmentStatus.COMPLETED && !showCompleted) continue;

            LocalDate apptDay = appt.getScheduledAt().toLocalDate();
            int col = dayColumn(apptDay);
            if (col < 1) continue;

            Color color = colorForStatus(appt.getStatus());
            boolean incompleteResources = appt.getStatus() == AppointmentStatus.CONFIRMED
                    && !orchestrator.hasRequiredResources(appt.getId());

            String customerName = appt.getCustomer().getFullName();
            String title = customerName + " — " + formatType(appt.getType());
            String statusText = appt.getStatus().toString();

            Label titleLbl = new Label(title);
            titleLbl.setFont(Font.font("System", FontWeight.BOLD, 9));
            titleLbl.setWrapText(true);
            titleLbl.setTextFill(Color.WHITE);
            Label statusLbl = new Label(statusText);
            statusLbl.setFont(Font.font(8));
            statusLbl.setTextFill(Color.WHITE.deriveColor(0, 1, 0.85, 1));

            VBox block = new VBox(2, titleLbl, statusLbl);
            block.setPadding(new Insets(3));
            block.setBackground(new Background(new BackgroundFill(
                    color, new CornerRadii(4), Insets.EMPTY)));

            String borderStyle = resourceBorderStyle(incompleteResources, apptDay);
            if (borderStyle != null) {
                block.setStyle(borderStyle);
            }

            block.setCursor(Cursor.HAND);
            block.setOnMousePressed(e -> e.consume());
            block.setOnMouseClicked(e -> {
                showPopover(appt, block);
                e.consume();
            });

            placeAppointmentBlock(block, appt, col);
        }
    }

    private void showPopover(Appointment appt, Node anchor) {
        if (appt.getStatus() == AppointmentStatus.COMPLETED
                || appt.getStatus() == AppointmentStatus.CANCELLED) {
            return;
        }

        if (detailPopover == null) {
            detailPopover = new AppointmentDetailPopover(orchestrator, () -> {
                onSelectionChanged.run();
            });
            detailPopover.setOnHidden(() -> popoverHideTime = System.currentTimeMillis());
        }

        boolean wasShowingForSame = appt.getId().equals(popoverAppointmentId)
                && (System.currentTimeMillis() - popoverHideTime) < 200;

        if (detailPopover.isShowing()) {
            detailPopover.hide();
            if (appt.getId().equals(popoverAppointmentId)) {
                popoverAppointmentId = null;
                return;
            }
        } else if (wasShowingForSame) {
            popoverAppointmentId = null;
            return;
        }

        popoverAppointmentId = appt.getId();
        detailPopover.show(appt, anchor);
    }

    private void placeAppointmentBlock(Region block, Appointment appt, int col) {
        double yStart = timeToY(appt.getScheduledAt());
        long durationMinutes = Duration.between(appt.getTimeSlot().getStart(), appt.getTimeSlot().getEnd()).toMinutes();
        if (durationMinutes < 30) durationMinutes = 60;
        double height = (durationMinutes / 60.0) * HOUR_HEIGHT;

        double x = 55 + (col - 1) * COL_WIDTH + 2;
        double width = COL_WIDTH - 4;

        block.setLayoutX(x);
        block.setLayoutY(yStart);
        block.setPrefSize(width, height);
        block.setMinSize(width, height);
        block.setMaxSize(width, height);
        overlay.getChildren().add(block);
    }

    // -------------------------------------------------------------------------
    // Block builders
    // -------------------------------------------------------------------------

    private VBox appointmentBlock(Appointment appt, Color color) {
        String title = formatType(appt.getType());
        String statusText = appt.getStatus().toString();

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 10));
        titleLbl.setWrapText(true);
        titleLbl.setTextFill(Color.WHITE);
        Label statusLbl = new Label(statusText);
        statusLbl.setFont(Font.font(9));
        statusLbl.setTextFill(Color.WHITE.deriveColor(0, 1, 0.85, 1));

        VBox box = new VBox(2, titleLbl, statusLbl);
        box.setPadding(new Insets(4));
        box.setBackground(new Background(new BackgroundFill(
                color, new CornerRadii(4), Insets.EMPTY)));

        return box;
    }

    // -------------------------------------------------------------------------
    // Coordinate helpers
    // -------------------------------------------------------------------------

    private double timeToY(LocalDateTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        return (hour - FIRST_HOUR) * HOUR_HEIGHT + (minute / 60.0) * HOUR_HEIGHT;
    }

    private LocalDateTime yToSnappedTime(double y, LocalDate day) {
        double totalMinutes = (y / HOUR_HEIGHT) * 60.0 + FIRST_HOUR * 60.0;
        int snappedMinutes = (int) (Math.round(totalMinutes / 30.0) * 30);
        snappedMinutes = Math.max(0, Math.min(24 * 60 - 30, snappedMinutes));
        int hour = snappedMinutes / 60;
        int minute = snappedMinutes % 60;
        return day.atTime(hour, minute);
    }

    private int dayColumn(LocalDate date) {
        long diff = ChronoUnit.DAYS.between(weekStart, date);
        if (diff < 0 || diff > 6) return -1;
        return (int) diff + 1;
    }

    private int hourRow(int hour) {
        if (hour < FIRST_HOUR || hour > LAST_HOUR) return -1;
        return hour - FIRST_HOUR;
    }

    // OCP: UI color mapping — presentation concern with domain-complete enum
    private Color colorForStatus(AppointmentStatus status) {
        return switch (status) {
            case SUGGESTED  -> COLOR_SUGGESTED;
            case SENT       -> COLOR_SENT;
            case CONFIRMED  -> COLOR_CONFIRMED;
            case COMPLETED  -> COLOR_COMPLETED;
            case CANCELLED  -> COLOR_COMPLETED;
        };
    }

    private String resourceBorderStyle(boolean incompleteResources, LocalDate apptDay) {
        if (!incompleteResources) return null;
        String color = apptDay.equals(clock.today()) ? "#D32F2F" : "#FFC107";
        return "-fx-border-color: " + color + "; -fx-border-width: 2; -fx-border-radius: 4;";
    }

    private String formatType(AppointmentType type) {
        return type.displayName();
    }
}
