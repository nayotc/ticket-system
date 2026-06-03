package ticketsystem.PresentationLayer.Views.Management;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ticketsystem.DTO.Event.*;
import ticketsystem.PresentationLayer.Components.ActionBar;
import ticketsystem.PresentationLayer.Components.AppCard;
import ticketsystem.PresentationLayer.Components.FormCard;
import ticketsystem.PresentationLayer.Components.Notifications;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.ManagementLayout;
import ticketsystem.PresentationLayer.Session.UiSession;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@PageTitle("TixNow | Map Builder")
@Route(value = UiRoutes.HALL_MAP_BUILDER, layout = ManagementLayout.class)
public class HallMapBuilder extends Div implements BeforeEnterObserver {

    private static final int CELL_SIZE = 36;
    private static final int DEFAULT_MAP_HEIGHT = 30;
    private static final int DEFAULT_MAP_WIDTH = 40;
    private static final int NEW_ELEMENT_OFFSET = 2;
    private static final int MIN_ZOOM = 60;
    private static final int MAX_ZOOM = 160;
    private static final int ZOOM_STEP = 10;

    private final HallMapBuilderPresenter presenter;

    private final Div canvas = new Div();
    private final Div propertiesPanel = new Div();
    private final Div statsPanel = new Div();
    private final Span eventTitle = new Span("יצירת מפת אולם");
    private final Span selectedElementBadge = new Span("לא נבחר אלמנט");

    private final IntegerField mapHeightField = new IntegerField("גובה מפה");
    private final IntegerField mapWidthField = new IntegerField("רוחב מפה");

    private final TextField elementNameField = new TextField("שם אלמנט");
    private final IntegerField elementXField = new IntegerField("עמודה X");
    private final IntegerField elementYField = new IntegerField("שורה Y");
    private final IntegerField elementWidthField = new IntegerField("רוחב");
    private final IntegerField elementHeightField = new IntegerField("גובה");

    private final TextField seatingNameField = new TextField("שם אזור ישיבה");
    private final IntegerField seatingRowsField = new IntegerField("שורות");
    private final IntegerField seatingColumnsField = new IntegerField("מושבים בשורה");
    private static final String MAP_OVERLAP_MESSAGE = "לא ניתן למקם אלמנט על אלמנט אחר";

    private final TextField standingNameField = new TextField("שם אזור עמידה");
    private final IntegerField standingCapacityField = new IntegerField("קיבולת מרבית");
    private final Span zoomValue = new Span("100%");
    private int zoomPercent = 100;
    private int cellSize = CELL_SIZE;

    private Long companyId;
    private Long eventId;
    private EventDTO eventDTO;
    private final List<IMapElementDTO> elements = new ArrayList<>();
    private int selectedIndex = -1;
    private long nextElementId = 1;
    private boolean updatingProperties;

    public HallMapBuilder(HallMapBuilderPresenter presenter) {
        this.presenter = presenter;

        addClassName("hall-map-builder-page");
        getElement().setAttribute("dir", "rtl");

        addAttachListener(event -> getParent()
                .ifPresent(parent -> parent.addClassName("hall-map-builder-content")));
        addDetachListener(event -> getParent()
                .ifPresent(parent -> parent.removeClassName("hall-map-builder-content")));

        configureFields();
        add(createHeader(), createWorkspace());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String token = UiSession.getMemberToken();
        if (token == null || token.isBlank()) {
            showWarning("כדי לערוך מפת אולם יש להתחבר למערכת");
            event.forwardTo(UiRoutes.LOGIN);
            return;
        }

        companyId = parseLong(event.getRouteParameters().get("companyId").orElse(null));
        eventId = parseLong(event.getRouteParameters().get("eventId").orElse(null));

        if (eventId == null) {
            showError("מזהה האירוע חסר או לא תקין");
            event.forwardTo(UiRoutes.EVENTS);
            return;
        }

        loadEvent(token);
    }

    private void configureFields() {
        configurePositiveField(mapHeightField, DEFAULT_MAP_HEIGHT, 1, 200);
        configurePositiveField(mapWidthField, DEFAULT_MAP_WIDTH, 1, 200);

        mapHeightField.addValueChangeListener(event -> renderCanvas());
        mapWidthField.addValueChangeListener(event -> renderCanvas());

        seatingNameField.setValue("אזור ישיבה");
        configurePositiveField(seatingRowsField, 10, 1, 80);
        configurePositiveField(seatingColumnsField, 20, 1, 80);

        standingNameField.setValue("אזור עמידה");
        configurePositiveField(standingCapacityField, 500, 1, 100000);

        elementNameField.addValueChangeListener(event -> updateSelectedElementFromProperties());
        elementXField.addValueChangeListener(event -> updateSelectedElementFromProperties());
        elementYField.addValueChangeListener(event -> updateSelectedElementFromProperties());
        elementWidthField.addValueChangeListener(event -> updateSelectedElementFromProperties());
        elementHeightField.addValueChangeListener(event -> updateSelectedElementFromProperties());
    }

    private void configurePositiveField(IntegerField field, int value, int min, int max) {
        field.setMin(min);
        field.setMax(max);
        field.setStepButtonsVisible(true);
        field.setValue(value);
        field.setWidthFull();
    }

    private Component createHeader() {
        Div header = new Div();
        header.addClassName("hall-builder-header");

        Div titleBlock = new Div();
        titleBlock.addClassName("hall-builder-title-block");

        eventTitle.addClassName("hall-builder-title");
        titleBlock.add(eventTitle);

        Button clear = new Button("ניקוי מפה", event -> clearMap());
        clear.setIcon(VaadinIcon.TRASH.create());
        clear.addClassName("hall-builder-secondary-button");

        Button save = new Button("שמירה ופרסום", event -> saveMap());
        save.setIcon(new Span("💾"));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClassName("hall-builder-save-button");

        Div leftControls = new Div();
        leftControls.addClassName("hall-builder-header-left");
        leftControls.add(createStepIndicator(), clear, save);

        header.add(leftControls, titleBlock);
        return header;
    }

    private Component createStepIndicator() {
        Div step = new Div();
        step.addClassName("hall-builder-step");
        step.add(new Span("שלב 2 מתוך 2"), createStepBars());
        return step;
    }

    private Component createFloatingZoomControls() {
        Div zoom = new Div();
        zoom.addClassName("hall-floating-zoom-controls");

        Button zoomIn = createZoomButton("+", () -> zoomIn());
        Button zoomOut = createZoomButton("-", () -> zoomOut());

        zoomValue.addClassName("hall-floating-zoom-value");

        zoom.add(zoomIn, zoomValue, zoomOut);
        return zoom;
    }

    private Button createZoomButton(String symbol, Runnable action) {
        Button button = new Button();
        button.addClassName("hall-floating-zoom-button");

        Div icon = new Div();
        icon.addClassName("hall-zoom-composite-icon");

        Span sign = new Span(symbol);
        sign.addClassName("hall-zoom-sign");

        icon.add(VaadinIcon.SEARCH.create(), sign);

        button.setIcon(icon);
        button.getElement().setAttribute(
                "aria-label",
                symbol.equals("+") ? "הגדלת תצוגה" : "הקטנת תצוגה"
        );
        button.addClickListener(event -> action.run());

        return button;
    }

    private void zoomIn() {
        zoomPercent = Math.min(MAX_ZOOM, zoomPercent + ZOOM_STEP);
        refreshZoom();
    }

    private void zoomOut() {
        zoomPercent = Math.max(MIN_ZOOM, zoomPercent - ZOOM_STEP);
        refreshZoom();
    }

    private void refreshZoom() {
        zoomValue.setText(zoomPercent + "%");
        renderCanvas();
    }

    private void setCellSize() {
        this.cellSize = Math.max(18, (int) Math.round(CELL_SIZE * (zoomPercent / 100.0)));
    }

    private Component createStepBars() {
        Div bars = new Div();
        bars.addClassName("hall-builder-step-bars");

        Div stepOne = new Div();
        stepOne.addClassName("hall-builder-step-bar");

        Div stepTwo = new Div();
        stepTwo.addClassName("hall-builder-step-bar");
        stepTwo.addClassName("hall-builder-step-bar-active");

        bars.add(stepOne, stepTwo);
        return bars;
    }

    private Component createWorkspace() {
        Div workspace = new Div();
        workspace.addClassName("hall-builder-workspace");

        Div canvasArea = new Div();
        canvasArea.addClassName("hall-builder-canvas-area");
        canvas.addClassName("hall-builder-canvas");
        canvasArea.add(canvas, createFloatingZoomControls());

        Div toolsPanel = new Div();
        toolsPanel.addClassName("hall-builder-tools-panel");
        toolsPanel.add(createPropertiesPanel(),
                createStatsPanel(),
                createToolButtonsCard(),
                createAreaCards(),
                createMapSettingsCard()

        );

        workspace.add(canvasArea, toolsPanel);
        return workspace;
    }

    private Component createMapSettingsCard() {
        HorizontalLayout sizeFields = new HorizontalLayout(mapHeightField, mapWidthField);
        sizeFields.addClassName("hall-builder-two-fields");
        sizeFields.setWidthFull();

        return new FormCard(
                "מידות מפה",
                "הגדר את גודל האולם ביחידות רשת.",
                sizeFields
        );
    }

    private Component createToolButtonsCard() {
        Div grid = new Div();
        grid.addClassName("hall-tool-grid");

        grid.add(
                toolButton(" במה", " 🎤", () -> addPlainElement("במה מרכזית", "Stage", 10, 4, 10, 3)),
                toolButton(" כניסה", " ⤷ ", () -> addPlainElement("כניסה ראשית", "Entrance", 1, 12, 4, 3)),
                toolButton(" יציאה",  " ⤶ ", () -> addPlainElement("יציאת חירום", "Exit", 1, 18, 4, 3)),
                toolButton("בר",  " 🍸", () -> addPlainElement("בר", "Bar", 26, 12, 5, 4)),
                toolButton(" עזרה ראשונה", " ✚" , () -> addPlainElement("עזרה ראשונה", "FirstAid", 26, 18, 5, 4)),
                toolButton(" אחר", " • ", () -> addPlainElement("אלמנט אחר", "Other", 12, 22, 4, 3))
        );

        return new FormCard("אלמנטים באולם", "הוסף אלמנטים קבועים למפה.", grid);
    }

    private Button toolButton(String text, String iconText, Runnable action) {
        Button button = new Button(text);
        Span icon = new Span(iconText);
        icon.addClassName("hall-tool-button-icon");
        button.setIcon(icon);
        button.addClassName("hall-tool-button");
        button.addClickListener(event -> action.run());
        return button;
    }

    private Component createAreaCards() {
        Div areas = new Div();
        areas.addClassName("hall-builder-areas");

        Button addSeating = new Button("הוסף אזור ישיבה", event -> addSeatingArea());
        addSeating.addClassName("hall-builder-outline-primary");

        Div seatingFields = new Div();
        seatingFields.addClassName("hall-area-fields");
        seatingFields.add(seatingNameField, seatingRowsField, seatingColumnsField, addSeating);

        AppCard seatingCard = new AppCard();
        seatingCard.addClassNames("hall-area-card", "hall-seating-card");
        seatingCard.add(areaTitle("אזור ישיבה", "💺"), seatingFields);

        Button addStanding = new Button("הוסף אזור עמידה", event -> addStandingArea());
        addStanding.addClassName("hall-builder-outline-secondary");

        Div standingFields = new Div();
        standingFields.addClassName("hall-area-fields");
        standingFields.add(standingNameField, standingCapacityField, addStanding);

        AppCard standingCard = new AppCard();
        standingCard.addClassNames("hall-area-card", "hall-standing-card");
        standingCard.add(areaTitle("אזור עמידה", "👥"), standingFields);

        areas.add(seatingCard, standingCard);
        return areas;
    }

    private Component areaTitle(String title, String iconText) {
        Div wrapper = new Div();
        wrapper.addClassName("hall-area-title");
        Span icon = new Span(iconText);
        icon.addClassName("hall-area-title-icon");
        wrapper.add(icon, new H3(title));
        return wrapper;
    }

    private Component createStatsPanel() {
        statsPanel.addClassName("hall-builder-stats");
        renderStats();
        return statsPanel;
    }

    private Component createPropertiesPanel() {
        propertiesPanel.addClassName("hall-builder-properties");
        renderPropertiesPanel();
        return propertiesPanel;
    }

    private void loadEvent(String token) {
        try {
            eventDTO = presenter.getEvent(token, eventId);
            eventTitle.setText("יצירת מפת אולם: " + safeText(eventDTO.name(), "אירוע"));

            PairDTO<Integer, Integer> size = eventDTO.map() != null && eventDTO.map().size() != null
                    ? eventDTO.map().size()
                    : eventDTO.mapSize();

            mapHeightField.setValue(size != null && size.first() != null ? size.first() : DEFAULT_MAP_HEIGHT);
            mapWidthField.setValue(size != null && size.second() != null ? size.second() : DEFAULT_MAP_WIDTH);

            elements.clear();
            if (eventDTO.map() != null && eventDTO.map().elements() != null) {
                elements.addAll(eventDTO.map().elements());
            }

            nextElementId = calculateNextElementId();
            selectedIndex = elements.isEmpty() ? -1 : 0;
            renderAll();
        } catch (Exception exception) {
            showError(messageOrDefault(exception, "לא ניתן לטעון את פרטי האירוע"));
            renderAll();
        }
    }

    private void addPlainElement(String name, String type, int x, int y, int width, int height) {
        int safeWidth = positive(width);
        int safeHeight = positive(height);
        PairDTO<Integer, Integer> location = nextAvailableLocation(x, y, safeWidth, safeHeight);

        if (location == null) {
            showWarning("אין מקום פנוי להוספת האלמנט במפה. הזז או מחק אלמנטים קיימים.");
            return;
        }

        elements.add(new ElementDTO(
                nextElementId++,
                name,
                location,
                new PairDTO<>(safeWidth, safeHeight),
                type
        ));

        selectedIndex = elements.size() - 1;
        renderAll();
    }

    private void addSeatingArea() {
        int rows = positive(seatingRowsField.getValue());
        int columns = positive(seatingColumnsField.getValue());
        String name = safeText(seatingNameField.getValue(), "אזור ישיבה");

        int width = Math.min(Math.max(columns / 2, 6), mapWidth());
        int height = Math.min(Math.max(rows / 2, 4), mapHeight());

        PairDTO<Integer, Integer> location = nextAvailableLocation(
                Math.max(1, mapWidth() / 3),
                Math.max(1, mapHeight() / 2),
                width,
                height
        );

        if (location == null) {
            showWarning("אין מקום פנוי להוספת אזור ישיבה במפה.");
            return;
        }

        elements.add(new SeatingAreaDTO(
                nextElementId++,
                name,
                location,
                new PairDTO<>(width, height),
                "SeatingArea",
                false,
                rows,
                columns,
                createAvailableSeats(rows, columns)
        ));

        selectedIndex = elements.size() - 1;
        renderAll();
    }

    private void addStandingArea() {
        int capacity = positive(standingCapacityField.getValue());
        String name = safeText(standingNameField.getValue(), "אזור עמידה");

        int width = 8;
        int height = 5;

        PairDTO<Integer, Integer> location = nextAvailableLocation(
                Math.max(1, mapWidth() / 2),
                Math.max(1, mapHeight() / 2),
                width,
                height
        );

        if (location == null) {
            showWarning("אין מקום פנוי להוספת אזור עמידה במפה.");
            return;
        }

        elements.add(new StandingAreaDTO(
                nextElementId++,
                name,
                location,
                new PairDTO<>(width, height),
                "StandingArea",
                false,
                capacity,
                0,
                0
        ));

        selectedIndex = elements.size() - 1;
        renderAll();
    }

    private List<SeatDTO> createAvailableSeats(int rows, int columns) {
        List<SeatDTO> seats = new ArrayList<>();
        for (int row = 1; row <= rows; row++) {
            for (int number = 1; number <= columns; number++) {
                seats.add(new SeatDTO(new SeatPositionDTO(row, number), "AVAILABLE"));
            }
        }
        return seats;
    }

    private void renderAll() {
        renderCanvas();
        renderStats();
        renderPropertiesPanel();
    }

    private void renderCanvas() {
        canvas.removeAll();
        setCellSize();
        Div map = new Div();
        map.addClassName("hall-map-surface");
        map.getStyle().set("--hall-cell-size", cellSize + "px");
        map.getStyle().set("width", mapWidth() * cellSize + "px");
        map.getStyle().set("height", mapHeight() * cellSize + "px");

        Div grid = new Div();
        grid.addClassName("hall-map-grid");
        map.add(grid, createGridLabels());

        Div center = new Div();
        center.addClassName("hall-map-center-indicator");
        map.add(center);

        if (elements.isEmpty()) {
            map.add(createCanvasEmptyState());
        }

        for (int i = 0; i < elements.size(); i++) {
            map.add(createCanvasElement(i, elements.get(i)));
        }

        canvas.add(map);
        configureMapPointerDrag(map);
    }

    private Component createCanvasEmptyState() {
        Div empty = new Div();
        empty.addClassName("hall-map-empty-state");
        empty.add(new Span("⌖"), new Span("הוסף במה, אזורי ישיבה או אזורי עמידה כדי לבנות את מפת האירוע"));
        return empty;
    }

    private Component createGridLabels() {
        Div labels = new Div();
        labels.addClassName("hall-map-grid-labels");

        for (int x = 1; x <= mapWidth(); x++) {
            labels.add(gridLabel(String.valueOf(x), "hall-grid-label-x", (x - 1) * cellSize + 4, 4));
        }

        for (int y = 1; y <= mapHeight(); y++) {
            labels.add(gridLabel(String.valueOf(y), "hall-grid-label-y", 4, (y - 1) * cellSize + 4));
        }

        return labels;
    }

    private Span gridLabel(String text, String className, int left, int top) {
        Span label = new Span(text);
        label.addClassNames("hall-grid-label", className);
        label.getStyle().set("left", left + "px");
        label.getStyle().set("top", top + "px");
        return label;
    }

    private void configureMapPointerDrag(Div map) {
        getElement().executeJs("""
            const root = this;
            const map = $0;
            const cellSize = $1;
            const mapWidth = $2;
            const mapHeight = $3;

            let drag = null;

            function clamp(value, min, max) {
                return Math.max(min, Math.min(max, value));
            }

            function cellFromPixels(value) {
                return Math.round(value / cellSize) + 1;
            }

            map.addEventListener('pointerdown', function(event) {
                const element = event.target.closest('.hall-map-element');
                if (!element || !map.contains(element) || event.button !== 0) {
                    return;
                }

                event.preventDefault();
                event.stopPropagation();

                const mapRect = map.getBoundingClientRect();
                const elementRect = element.getBoundingClientRect();
                const widthCells = Number(element.dataset.widthCells || 1);
                const heightCells = Number(element.dataset.heightCells || 1);

                drag = {
                    element: element,
                    index: Number(element.dataset.mapIndex),
                    offsetX: event.clientX - elementRect.left,
                    offsetY: event.clientY - elementRect.top,
                    widthCells: widthCells,
                    heightCells: heightCells,
                    lastX: Number(element.dataset.x || 1),
                    lastY: Number(element.dataset.y || 1)
                };

                element.classList.add('hall-map-element-dragging');
                element.setPointerCapture(event.pointerId);
            });

            map.addEventListener('pointermove', function(event) {
                if (!drag) {
                    return;
                }

                event.preventDefault();

                const mapRect = map.getBoundingClientRect();
                const rawX = event.clientX - mapRect.left - drag.offsetX;
                const rawY = event.clientY - mapRect.top - drag.offsetY;

                const maxX = Math.max(1, mapWidth - drag.widthCells + 1);
                const maxY = Math.max(1, mapHeight - drag.heightCells + 1);

                const cellX = clamp(cellFromPixels(rawX), 1, maxX);
                const cellY = clamp(cellFromPixels(rawY), 1, maxY);

                drag.lastX = cellX;
                drag.lastY = cellY;
                drag.element.style.left = ((cellX - 1) * cellSize) + 'px';
                drag.element.style.top = ((cellY - 1) * cellSize) + 'px';
                drag.element.dataset.x = String(cellX);
                drag.element.dataset.y = String(cellY);
            });

            function finishDrag(event) {
                if (!drag) {
                    return;
                }

                const finished = drag;
                finished.element.classList.remove('hall-map-element-dragging');

                try {
                    finished.element.releasePointerCapture(event.pointerId);
                } catch (ignore) {
                }

                drag = null;
                root.$server.handleElementMoved(finished.index, finished.lastX, finished.lastY);
            }

            map.addEventListener('pointerup', finishDrag);
            map.addEventListener('pointercancel', finishDrag);
            map.addEventListener('mouseleave', function(event) {
                if (drag && event.buttons === 0) {
                    finishDrag(event);
                }
            });
            """, map.getElement(), cellSize, mapWidth(), mapHeight());
    }

    @ClientCallable
    public void handleElementMoved(int index, int x, int y) {
        if (index < 0 || index >= elements.size()) {
            return;
        }

        moveElementToGrid(index, x, y);
    }

    private Component createCanvasElement(int index, IMapElementDTO dto) {
        Div element = new Div();
        element.addClassName("hall-map-element");
        element.addClassName(resolveElementClass(dto));
        if (index == selectedIndex) {
            element.addClassName("hall-map-element-selected");
        }

        PairDTO<Integer, Integer> location = locationOf(dto);
        PairDTO<Integer, Integer> size = sizeOf(dto);

        int x = clamp(location.first(), 1, mapWidth());
        int y = clamp(location.second(), 1, mapHeight());
        int width = positive(size.first());
        int height = positive(size.second());

        element.getStyle().set("left", (x - 1) * cellSize + "px");
        element.getStyle().set("top", (y - 1) * cellSize + "px");
        element.getStyle().set("width", width * cellSize + "px");
        element.getStyle().set("height", height * cellSize + "px");
        element.getElement().setAttribute("title", nameOf(dto) + " | X=" + x + ", Y=" + y);
        element.getElement().setAttribute("data-map-index", String.valueOf(index));
        element.getElement().setAttribute("data-x", String.valueOf(x));
        element.getElement().setAttribute("data-y", String.valueOf(y));
        element.getElement().setAttribute("data-width-cells", String.valueOf(width));
        element.getElement().setAttribute("data-height-cells", String.valueOf(height));

        Span icon = new Span(resolveElementIcon(dto));
        icon.addClassName("hall-map-element-icon");

        Span name = new Span(nameOf(dto));
        name.addClassName("hall-map-element-name");

        Span meta = new Span(metaOf(dto));
        meta.addClassName("hall-map-element-meta");

        element.add(icon, name, meta);
        element.addClickListener(event -> {
            selectedIndex = index;
            renderAll();
        });
        return element;
    }

    private void moveElementToGrid(int index, int x, int y) {
        IMapElementDTO current = elements.get(index);
        PairDTO<Integer, Integer> size = sizeOf(current);

        int safeX = clamp(x, 1, maxXFor(size.first()));
        int safeY = clamp(y, 1, maxYFor(size.second()));

        if (!isLocationAvailable(index, safeX, safeY, size.first(), size.second())) {
            showWarning(MAP_OVERLAP_MESSAGE);
            selectedIndex = index;
            renderAll();
            return;
        }

        elements.set(index, updateGeometry(current, nameOf(current), safeX, safeY, size.first(), size.second()));
        selectedIndex = index;
        renderAll();
    }

    private void renderStats() {
        statsPanel.removeAll();

        int seats = elements.stream()
                .filter(SeatingAreaDTO.class::isInstance)
                .map(SeatingAreaDTO.class::cast)
                .mapToInt(area -> area.rows() * area.columns())
                .sum();

        long standingCapacity = elements.stream()
                .filter(StandingAreaDTO.class::isInstance)
                .map(StandingAreaDTO.class::cast)
                .mapToLong(StandingAreaDTO::capacity)
                .sum();

        statsPanel.add(
                statItem("אלמנטים", String.valueOf(elements.size())),
                statItem("מושבים", String.valueOf(seats)),
                statItem("קיבולת עמידה", String.valueOf(standingCapacity))
        );
    }

    private Component statItem(String label, String value) {
        Div item = new Div();
        item.addClassName("hall-stat-item");

        Span labelElement = new Span(label);
        labelElement.addClassName("hall-stat-label");

        Span valueElement = new Span(value);
        valueElement.addClassName("hall-stat-value");

        item.add(labelElement, valueElement);
        return item;
    }

    private void renderPropertiesPanel() {
        propertiesPanel.removeAll();

        Div header = new Div();
        header.addClassName("hall-properties-header");

        H2 title = new H2("מאפיינים");
        title.addClassName("hall-properties-title");

        selectedElementBadge.addClassName("hall-selected-badge");
        header.add(title, selectedElementBadge);

        propertiesPanel.add(header);

        if (selectedIndex < 0 || selectedIndex >= elements.size()) {
            selectedElementBadge.setText("לא נבחר אלמנט");
            Paragraph empty = new Paragraph("בחר אלמנט על המפה כדי לערוך שם, מיקום וגודל.");
            empty.addClassName("hall-properties-empty");
            propertiesPanel.add(empty);
            return;
        }

        IMapElementDTO selected = elements.get(selectedIndex);
        selectedElementBadge.setText(nameOf(selected));

        updatePropertiesFields(selected);

        HorizontalLayout positionFields = new HorizontalLayout(elementXField, elementYField);
        positionFields.addClassName("hall-builder-two-fields");
        positionFields.setWidthFull();

        HorizontalLayout sizeFields = new HorizontalLayout(elementWidthField, elementHeightField);
        sizeFields.addClassName("hall-builder-two-fields");
        sizeFields.setWidthFull();

        Button delete = new Button("מחיקת אלמנט", event -> deleteSelectedElement());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClassName("hall-delete-button");

        propertiesPanel.add(elementNameField, positionFields, sizeFields, delete);
    }

    private void updatePropertiesFields(IMapElementDTO dto) {
        updatingProperties = true;

        PairDTO<Integer, Integer> location = locationOf(dto);
        PairDTO<Integer, Integer> size = sizeOf(dto);

        elementNameField.setValue(nameOf(dto));
        elementXField.setValue(location.first());
        elementYField.setValue(location.second());
        elementWidthField.setValue(size.first());
        elementHeightField.setValue(size.second());

        updatingProperties = false;
    }

    private void updateSelectedElementFromProperties() {
        if (updatingProperties || selectedIndex < 0 || selectedIndex >= elements.size()) {
            return;
        }

        IMapElementDTO current = elements.get(selectedIndex);

        int width = positive(elementWidthField.getValue());
        int height = positive(elementHeightField.getValue());
        int x = clamp(positive(elementXField.getValue()), 1, maxXFor(width));
        int y = clamp(positive(elementYField.getValue()), 1, maxYFor(height));

        if (!isLocationAvailable(selectedIndex, x, y, width, height)) {
            showWarning(MAP_OVERLAP_MESSAGE);
            updatePropertiesFields(current);
            renderCanvas();
            return;
        }

        IMapElementDTO updated = updateGeometry(
                current,
                safeText(elementNameField.getValue(), nameOf(current)),
                x,
                y,
                width,
                height
        );

        elements.set(selectedIndex, updated);
        renderCanvas();
        renderStats();
    }

    private IMapElementDTO updateGeometry(IMapElementDTO dto, String name, int x, int y, int width, int height) {
        PairDTO<Integer, Integer> location = new PairDTO<>(x, y);
        PairDTO<Integer, Integer> size = new PairDTO<>(width, height);

        if (dto instanceof SeatingAreaDTO area) {
            return new SeatingAreaDTO(
                    area.id(),
                    name,
                    location,
                    size,
                    area.type(),
                    area.soldOut(),
                    area.rows(),
                    area.columns(),
                    area.seats()
            );
        }

        if (dto instanceof StandingAreaDTO area) {
            return new StandingAreaDTO(
                    area.id(),
                    name,
                    location,
                    size,
                    area.type(),
                    area.soldOut(),
                    area.capacity(),
                    area.reserved(),
                    area.sold()
            );
        }

        ElementDTO element = (ElementDTO) dto;
        return new ElementDTO(element.id(), name, location, size, element.type());
    }

    private void deleteSelectedElement() {
        if (selectedIndex < 0 || selectedIndex >= elements.size()) {
            return;
        }

        elements.remove(selectedIndex);
        selectedIndex = elements.isEmpty() ? -1 : Math.min(selectedIndex, elements.size() - 1);
        renderAll();
    }

    private void clearMap() {
        elements.clear();
        selectedIndex = -1;
        renderAll();
    }

    private void saveMap() {
        String token = UiSession.getMemberToken();
        if (token == null || token.isBlank()) {
            showWarning("כדי לשמור את המפה יש להתחבר למערכת");
            UI.getCurrent().navigate(UiRoutes.LOGIN);
            return;
        }

        if (hasOverlappingElements()) {
            showWarning("לא ניתן לשמור מפה עם אלמנטים חופפים. הזז את האלמנטים כך שלא יכסו אחד את השני.");
            return;
        }

        try {
            EventMapDTO mapDTO = new EventMapDTO(
                    new PairDTO<>(mapHeight(), mapWidth()),
                    new ArrayList<>(elements),
                    false
            );

            boolean saved = presenter.defineEventMap(token, eventId, mapDTO);

            if (!saved) {
                showError("שמירת המפה נכשלה");
                return;
            }

            showSuccess("המפה נשמרה והאירוע פורסם בהצלחה");

            if (companyId != null) {
                UI.getCurrent().navigate(UiRoutes.COMPANY_MANAGEMENT.replace(":companyId", String.valueOf(companyId)));
            }
        } catch (Exception exception) {
            showError(messageOrDefault(exception, "שמירת המפה נכשלה"));
        }
    }

    private PairDTO<Integer, Integer> locationOf(IMapElementDTO dto) {
        if (dto instanceof SeatingAreaDTO area) {
            return safePair(area.location(), 1, 1);
        }
        if (dto instanceof StandingAreaDTO area) {
            return safePair(area.location(), 1, 1);
        }
        ElementDTO element = (ElementDTO) dto;
        return safePair(element.location(), 1, 1);
    }

    private PairDTO<Integer, Integer> sizeOf(IMapElementDTO dto) {
        if (dto instanceof SeatingAreaDTO area) {
            return safePair(area.size(), 4, 4);
        }
        if (dto instanceof StandingAreaDTO area) {
            return safePair(area.size(), 4, 4);
        }
        ElementDTO element = (ElementDTO) dto;
        return safePair(element.size(), 4, 3);
    }

    private String nameOf(IMapElementDTO dto) {
        if (dto instanceof SeatingAreaDTO area) {
            return safeText(area.name(), "אזור ישיבה");
        }
        if (dto instanceof StandingAreaDTO area) {
            return safeText(area.name(), "אזור עמידה");
        }
        ElementDTO element = (ElementDTO) dto;
        return safeText(element.name(), "אלמנט");
    }

    private String metaOf(IMapElementDTO dto) {
        if (dto instanceof SeatingAreaDTO area) {
            return area.rows() + "x" + area.columns() + " מושבים";
        }
        if (dto instanceof StandingAreaDTO area) {
            return "קיבולת " + area.capacity();
        }
        PairDTO<Integer, Integer> size = sizeOf(dto);
        return size.first() + "x" + size.second();
    }

    private String resolveElementClass(IMapElementDTO dto) {
        if (dto instanceof SeatingAreaDTO) {
            return "hall-map-seating-area";
        }
        if (dto instanceof StandingAreaDTO) {
            return "hall-map-standing-area";
        }

        ElementDTO element = (ElementDTO) dto;
        String type = element.type() == null ? "" : element.type();
        return switch (type) {
            case "Stage" -> "hall-map-stage";
            case "Entrance" -> "hall-map-entrance";
            case "Exit" -> "hall-map-exit";
            case "Bar" -> "hall-map-bar";
            case "FirstAid" -> "hall-map-first-aid";
            case "Other" -> "hall-map-other";
            default -> "hall-map-generic-element";
        };
    }

    private String resolveElementIcon(IMapElementDTO dto) {
        if (dto instanceof SeatingAreaDTO) {
            return "💺" ;
        }
        if (dto instanceof StandingAreaDTO) {
            return "👥";
        }

        ElementDTO element = (ElementDTO) dto;
        String type = element.type() == null ? "" : element.type();
        return switch (type) {
            case "Stage" -> "🎤";
            case "Entrance" -> "↳";
            case "Exit" -> "↲";
            case "Bar" -> "🍸" ;
            case "FirstAid" -> "✚";
            case "Other" -> "•";
            default -> "•";
        };
    }

    private PairDTO<Integer, Integer> nextAvailableLocation(int preferredX, int preferredY, int width, int height) {
        int maxX = maxXFor(width);
        int maxY = maxYFor(height);

        int startX = clamp(preferredX, 1, maxX);
        int startY = clamp(preferredY, 1, maxY);

        for (int y = startY; y <= maxY; y++) {
            for (int x = startX; x <= maxX; x++) {
                if (isLocationAvailable(-1, x, y, width, height)) {
                    return new PairDTO<>(x, y);
                }
            }
        }

        for (int y = 1; y <= maxY; y++) {
            for (int x = 1; x <= maxX; x++) {
                if (isLocationAvailable(-1, x, y, width, height)) {
                    return new PairDTO<>(x, y);
                }
            }
        }

        return null;
    }

    private boolean overlapsAny(int x, int y, int width, int height) {
        return !isLocationAvailable(-1, x, y, width, height);
    }

    private boolean isLocationAvailable(int ignoredIndex, int x, int y, int width, int height) {
        for (int i = 0; i < elements.size(); i++) {
            if (i == ignoredIndex) {
                continue;
            }

            IMapElementDTO element = elements.get(i);
            PairDTO<Integer, Integer> otherLocation = locationOf(element);
            PairDTO<Integer, Integer> otherSize = sizeOf(element);

            if (rectanglesOverlap(
                    x,
                    y,
                    width,
                    height,
                    otherLocation.first(),
                    otherLocation.second(),
                    otherSize.first(),
                    otherSize.second()
            )) {
                return false;
            }
        }

        return true;
    }

    private boolean hasOverlappingElements() {
        for (int i = 0; i < elements.size(); i++) {
            PairDTO<Integer, Integer> firstLocation = locationOf(elements.get(i));
            PairDTO<Integer, Integer> firstSize = sizeOf(elements.get(i));

            for (int j = i + 1; j < elements.size(); j++) {
                PairDTO<Integer, Integer> secondLocation = locationOf(elements.get(j));
                PairDTO<Integer, Integer> secondSize = sizeOf(elements.get(j));

                if (rectanglesOverlap(
                        firstLocation.first(),
                        firstLocation.second(),
                        firstSize.first(),
                        firstSize.second(),
                        secondLocation.first(),
                        secondLocation.second(),
                        secondSize.first(),
                        secondSize.second()
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean rectanglesOverlap(
            int x,
            int y,
            int width,
            int height,
            int otherX,
            int otherY,
            int otherWidth,
            int otherHeight
    ) {
        return x < otherX + otherWidth
                && x + width > otherX
                && y < otherY + otherHeight
                && y + height > otherY;
    }

    private int maxXFor(int width) {
        return Math.max(1, mapWidth() - positive(width) + 1);
    }

    private int maxYFor(int height) {
        return Math.max(1, mapHeight() - positive(height) + 1);
    }

    private long calculateNextElementId() {
        return elements.stream()
                .map(this::idOf)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1;
    }

    private Long idOf(IMapElementDTO dto) {
        if (dto instanceof SeatingAreaDTO area) {
            return area.id();
        }
        if (dto instanceof StandingAreaDTO area) {
            return area.id();
        }
        if (dto instanceof ElementDTO element) {
            return element.id();
        }
        return null;
    }

    private PairDTO<Integer, Integer> safePair(PairDTO<Integer, Integer> pair, int first, int second) {
        if (pair == null) {
            return new PairDTO<>(first, second);
        }
        return new PairDTO<>(positive(pair.first()), positive(pair.second()));
    }

    private int mapHeight() {
        return positive(mapHeightField.getValue());
    }

    private int mapWidth() {
        return positive(mapWidthField.getValue());
    }

    private int positive(Integer value) {
        return value == null || value <= 0 ? 1 : value;
    }

    private int clamp(Integer value, int min, int max) {
        int safe = value == null ? min : value;
        return Math.max(min, Math.min(max, safe));
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String messageOrDefault(Exception exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? fallback
                : exception.getMessage();
    }

    private void showSuccess(String message) {
        Notifications.success(message);
    }

    private void showWarning(String message) {
        Notifications.warning(message);
    }

    private void showError(String message) {
        Notifications.error(message);
    }

    public interface HallMapBuilderPresenter {
        EventDTO getEvent(String sessionId, Long eventId);

        boolean defineEventMap(String sessionId, Long eventId, EventMapDTO mapDTO);
    }
}
