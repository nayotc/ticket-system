package ticketsystem.PresentationLayer.Views;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import ticketsystem.DTO.MemberDTO;
import ticketsystem.DTO.MyAccountDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.PresentationLayer.Components.AppCard;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.StatusBadge;
import ticketsystem.PresentationLayer.Components.ViewHeader;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.MainLayout;
import ticketsystem.PresentationLayer.Session.UiSession;
import ticketsystem.PresentationLayer.Presenters.MyAccountPresenter;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

@Route(value = UiRoutes.MY_ACCOUNT, layout = MainLayout.class)
public class MyAccount extends PageContainer implements BeforeEnterObserver {

    private MyAccountPresenter presenter;

    private  Span avatarInitials = new Span("מש");
    private final H3 profileName = new H3("משתמש מערכת");

    private final TextField fullNameField = new TextField("שם מלא");
    private final EmailField emailField = new EmailField("כתובת אימייל");
    private final TextField phoneField = new TextField("מספר טלפון");
    //private final TextField usernameField = new TextField("שם משתמש");
    private final PasswordField currentPasswordField = new PasswordField("סיסמה נוכחית");
    private final PasswordField newPasswordField = new PasswordField("סיסמה חדשה");
    private final PasswordField confirmPasswordField = new PasswordField("אימות סיסמה חדשה");

    private final Grid<MyPurchaseRow> historyGrid = new Grid<>(MyPurchaseRow.class, false);
    private final Div emptyHistoryState = new Div();

    public MyAccount(MyAccountPresenter presenter) {
        this.presenter = presenter;
        addClassName("my-account-page");
        
        configureFields();
        configureHistoryGrid();

        add(
                new ViewHeader(
                        "אזור אישי",
                        "ניהול פרטי החשבון והיסטוריית הרכישות שלך."
                ),
                createContent()
        );
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!UiSession.isLoggedIn()) {
            event.rerouteTo(UiRoutes.HOME);
            return;
        }

        loadDataFromPresenter();
    }

    public void setPresenter(MyAccountPresenter presenter) {
        this.presenter = presenter;
        loadDataFromPresenter();
    }

    private Component createContent() {
        Div content = new Div();
        content.addClassName("my-account-content");
        content.add(createProfileCard(), createHistoryCard());
        return content;
    }

    private Component createProfileCard() {
        AppCard card = new AppCard();
        card.addClassName("my-account-profile-card");

        Div glow = new Div();
        glow.addClassName("my-account-profile-glow");

        Div identityRow = new Div();
        identityRow.addClassName("my-account-profile-identity");

        avatarInitials.addClassName("my-account-avatar");

        Div identityText = new Div();
        identityText.addClassName("my-account-identity-text");

        profileName.addClassName("my-account-profile-name");


        identityText.add(profileName);
        identityRow.add(avatarInitials, identityText);

        Div form = new Div();
        form.addClassName("my-account-profile-form");

        Button saveButton = new Button("שמור שינויים", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClassName("my-account-save-button");
        saveButton.addClickListener(event -> savePersonalDetails());

        form.add(
                fullNameField,
                emailField,
                phoneField,
                //usernameField,
               // currentPasswordField,
                newPasswordField,
                confirmPasswordField,
                saveButton
        );

        card.add(glow, identityRow, form);
        return card;
    }

    private Component createHistoryCard() {
        AppCard card = new AppCard();
        card.addClassName("my-account-history-card");

        Div header = new Div();
        header.addClassName("my-account-history-header");

        Div titleBlock = new Div();

        H2 title = new H2("היסטוריית רכישות");
        title.addClassName("my-account-card-title");

        Paragraph subtitle = new Paragraph("רכישות שבוצעו דרך החשבון שלך.");
        subtitle.addClassName("my-account-card-subtitle");

        titleBlock.add(title, subtitle);

        Button refreshButton = new Button("רענן", VaadinIcon.REFRESH.create());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClassName("my-account-refresh-button");
        refreshButton.addClickListener(event -> loadDataFromPresenter());

        header.add(titleBlock, refreshButton);

        emptyHistoryState.addClassName("my-account-empty-history");
        emptyHistoryState.add(
                new Span(""),
                new H3("אין רכישות להצגה"),
                new Paragraph("ברגע שתבצע רכישה, היא תופיע כאן.")
        );

        card.add(header, historyGrid, emptyHistoryState);
        return card;
    }

private void configureFields() {
    fullNameField.setPlaceholder("לדוגמה: ישראל ישראלי");
    emailField.setPlaceholder("name@example.com");
    phoneField.setPlaceholder("050-0000000");

    newPasswordField.setPlaceholder("השאר ריק אם אין שינוי");
    confirmPasswordField.setPlaceholder("השאר ריק אם אין שינוי");

    newPasswordField.clear();
    confirmPasswordField.clear();

    newPasswordField.getElement().setAttribute("autocomplete", "new-password");
    confirmPasswordField.getElement().setAttribute("autocomplete", "new-password");

    fullNameField.addClassName("my-account-field");
    emailField.addClassName("my-account-field");
    phoneField.addClassName("my-account-field");
    newPasswordField.addClassName("my-account-field");
    confirmPasswordField.addClassName("my-account-field");

    emailField.getElement().setAttribute("dir", "ltr");
    phoneField.getElement().setAttribute("dir", "ltr");

    emailField.getElement().getStyle().set("text-align", "right");
    phoneField.getElement().getStyle().set("text-align", "right");
}
    private void configureHistoryGrid() {
        historyGrid.addClassName("my-account-history-grid");
        historyGrid.setWidthFull();
        historyGrid.setAllRowsVisible(true);

        historyGrid.addColumn(MyPurchaseRow::getPurchaseId)
                .setHeader("מס' הזמנה")
                .setAutoWidth(true)
                .setFlexGrow(0);

        historyGrid.addColumn(MyPurchaseRow::getEventName)
                .setHeader("אירוע")
                .setAutoWidth(true)
                .setFlexGrow(1);

        historyGrid.addColumn(MyPurchaseRow::getTicketsCount)
                .setHeader("כרטיסים")
                .setAutoWidth(true)
                .setFlexGrow(0);

        historyGrid.addColumn(MyPurchaseRow::getTotalAmount)
                .setHeader("סה\"כ")
                .setAutoWidth(true)
                .setFlexGrow(0);

        historyGrid.addComponentColumn(row -> new StatusBadge(row.getStatusLabel(), row.getStatusType()))
                .setHeader("סטטוס")
                .setAutoWidth(true)
                .setFlexGrow(0);

        historyGrid.setItemDetailsRenderer(
                new ComponentRenderer<>(this::createTicketsDetails)
        );

        historyGrid.addComponentColumn(order -> {
            Button button = new Button("צפה", VaadinIcon.EYE.create());
            button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            button.addClickListener(e ->
                    historyGrid.setDetailsVisible(order, !historyGrid.isDetailsVisible(order))
            );
            return button;
        }).setHeader("");
    }

    private Button createPurchaseAction(MyPurchaseRow row) {
        Button button = new Button(row.getActionText(), VaadinIcon.EYE.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        button.addClassName("my-account-table-action");

        button.addClickListener(event ->
                historyGrid.setDetailsVisible(row, !historyGrid.isDetailsVisible(row))
        );

        return button;
    }

    private void savePersonalDetails() {
        if (presenter == null) {
            return;
        }

        if (!Objects.equals(newPasswordField.getValue(), confirmPasswordField.getValue())) {
            showError("הסיסמה החדשה ואימות הסיסמה אינם זהים.");
            return;
        }
        openPasswordConfirmationDialog();

    }

private void openPasswordConfirmationDialog() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("אימות פעולה");

    PasswordField passwordField = new PasswordField("סיסמה נוכחית");
    passwordField.setWidthFull();
    passwordField.setRevealButtonVisible(false);
    passwordField.clear();
    passwordField.getElement().setAttribute("autocomplete", "current-password");

    Button confirmButton = new Button("אישור", event -> {
        if (passwordField.getValue() == null || passwordField.getValue().isBlank()) {
            showError("יש להזין סיסמה נוכחית");
            return;
        }

        AccountProfileEditData data = new AccountProfileEditData(
                fullNameField.getValue(),
                emailField.getValue(),
                phoneField.getValue(),
                passwordField.getValue(),
                newPasswordField.getValue()
        );

        try {
            presenter.updatePersonalDetails(UiSession.getMemberToken(), data);

            newPasswordField.clear();
            confirmPasswordField.clear();
            passwordField.clear();

            showSuccess("הפרטים עודכנו בהצלחה.");
            dialog.close();
            loadDataFromPresenter();

        } catch (RuntimeException ex) {
            showError(ex.getMessage() == null ? "שמירת הפרטים נכשלה." : ex.getMessage());
        }
    });

    confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("ביטול", event -> {
        passwordField.clear();
        dialog.close();
    });

    dialog.add(passwordField);
    dialog.getFooter().add(cancelButton, confirmButton);
    dialog.open();
}
    private void loadDataFromPresenter() {
        if (presenter == null || UiSession.getMemberToken() == null) {
            return;
        }

        try {
            MyAccountDTO member = presenter.loadProfile(UiSession.getMemberToken());
            if (member != null) {
                setProfile(new AccountProfileViewData(
                        member.getEmail(),
                        member.getFullName(),
                        member.getEmail(),
                        member.getPhone(),member.getBirthDate()
                ));
               // avatarInitials = new Span(member.getFullName());
            }

            List<OrderDTO> orders = presenter.loadPurchaseHistory(UiSession.getMemberToken());
           
            setPurchaseHistory(orders);

        } catch (RuntimeException ex) {
            showError(ex.getMessage() == null ? "טעינת האזור האישי נכשלה." : ex.getMessage());
        }
    }


public void setProfile(AccountProfileViewData profile) {
    fullNameField.setValue(nullToEmpty(profile.fullName()));
    emailField.setValue(nullToEmpty(profile.email()));
    phoneField.setValue(nullToEmpty(profile.phone()));

    newPasswordField.clear();
    confirmPasswordField.clear();

    String displayName = !nullToEmpty(profile.fullName()).isBlank()
            ? profile.fullName()
            : profile.username();

    profileName.setText(nullToEmpty(displayName));
    avatarInitials.setText(createInitials(displayName));
}
    public void setPurchaseHistory(List<OrderDTO> orders) {
        List<MyPurchaseRow> rows = orders == null
                ? List.of()
                : orders.stream()
                .filter(Objects::nonNull)
                .map(this::mapOrderDtoToRow)
                .toList();

        setHistoryRows(rows);
    }

    private void setHistoryRows(List<MyPurchaseRow> rows) {
        historyGrid.setItems(rows);
        boolean empty = rows == null || rows.isEmpty();
        historyGrid.setVisible(!empty);
        emptyHistoryState.setVisible(empty);
    }

    private MyPurchaseRow mapOrderDtoToRow(OrderDTO order) {
        String purchaseId = firstNonBlank(
                asText(readRaw(order, "purchaseId", "id", "orderId")),
                "לא זמין"
        );

        Object eventId = readRaw(order, "eventId");
        String eventName = firstNonBlank(
                asText(readRaw(order, "eventName", "name")),
                eventId == null ? "אירוע" : "אירוע #" + eventId
        );

        String date = firstNonBlank(
                asText(readRaw(order, "purchaseDate", "createdAt", "orderDate", "date")),
                "לא זמין"
        );

        String tickets = firstNonBlank(
                countTickets(order),
                "0"
        );

        String total = formatMoney(readRaw(order, "totalPrice", "totalAmount", "price", "total"));

        String status = firstNonBlank(
                asText(readRaw(order, "status", "orderStatus", "purchaseStatus")),
                "הושלם"
        );
        List<PurchaseDTO> ticketsList =order.getTickets() == null ? List.of() : order.getTickets();

        return new MyPurchaseRow(
                "#" + purchaseId,
                eventName,
                tickets,
                total,
                translateStatus(status),
                statusType(status),
                "צפה",ticketsList
        );
    }


    private String countTickets(OrderDTO order) {
        Object tickets = readRaw(order, "tickets", "purchasedTickets", "orderTickets");

        if (tickets instanceof Collection<?> collection) {
            return String.valueOf(collection.size());
        }

        return firstNonBlank(
                asText(readRaw(order, "ticketCount", "ticketsCount", "quantity")),
                null
        );
    }

    private Object readRaw(Object source, String... propertyNames) {
        if (source == null || propertyNames == null) {
            return null;
        }

        for (String propertyName : propertyNames) {
            Object value = invokeGetter(source, propertyName);
            if (value != null) {
                return value;
            }

            value = readField(source, propertyName);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private Object invokeGetter(Object source, String propertyName) {
        String capitalized = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        String[] methodNames = {"get" + capitalized, "is" + capitalized, propertyName};

        for (String methodName : methodNames) {
            try {
                Method method = source.getClass().getMethod(methodName);
                return method.invoke(source);
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private Object readField(Object source, String propertyName) {
        try {
            Field field = source.getClass().getDeclaredField(propertyName);
            field.setAccessible(true);
            return field.get(source);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof LocalDate localDate) {
            return localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }

        return String.valueOf(value);
    }

    private String formatMoney(Object value) {
        if (value == null) {
            return "₪0";
        }

        try {
            BigDecimal amount = new BigDecimal(String.valueOf(value));
            return "₪" + amount.stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ignored) {
            return String.valueOf(value);
        }
    }

    private StatusBadge.Type statusType(String status) {
        String normalized = nullToEmpty(status).toLowerCase();

        if (normalized.contains("cancel") || normalized.contains("בוטל")) {
            return StatusBadge.Type.ERROR;
        }

        if (normalized.contains("pending") || normalized.contains("wait") || normalized.contains("ממתין")) {
            return StatusBadge.Type.INFO;
        }

        if (normalized.contains("success")
                || normalized.contains("complete")
                || normalized.contains("approved")
                || normalized.contains("מאושר")
                || normalized.contains("הושלם")) {
            return StatusBadge.Type.SUCCESS;
        }

        return StatusBadge.Type.NEUTRAL;
    }

    private String translateStatus(String status) {
        String normalized = nullToEmpty(status).toLowerCase();

        if (normalized.contains("cancel")) {
            return "בוטל";
        }

        if (normalized.contains("pending") || normalized.contains("wait")) {
            return "ממתין";
        }

        if (normalized.contains("approved") || normalized.contains("success")) {
            return "מאושר";
        }

        if (normalized.contains("complete")) {
            return "הושלם";
        }

        return firstNonBlank(status, "הושלם");
    }

    private String createInitials(String text) {
        String value = nullToEmpty(text).trim();

        if (value.isBlank()) {
            return "מש";
        }

        String[] parts = value.split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length()));
        }

        return parts[0].substring(0, 1) + parts[1].substring(0, 1);
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    public record AccountProfileViewData(
            String username,
            String fullName,
            String email,
            String phone,
            LocalDate birthday
    ) {
    }

    public record AccountProfileEditData(
            String fullName,
            String email,
            String phone,
           // String username,
            String currentPassword,
            String newPassword
    ) {
    }

    private Component createTicketsDetails(MyPurchaseRow row) {
    VerticalLayout wrapper = new VerticalLayout();
    wrapper.addClassName("tickets-details-wrapper");
    wrapper.setPadding(false);
    wrapper.setSpacing(true);
    wrapper.setWidthFull();
    
    
    for (PurchaseDTO ticket : row.getTickets()) {
        HorizontalLayout card = new HorizontalLayout();
        card.addClassName("ticket-detail-card");
        card.setWidthFull();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Span title = new Span("כרטיס #" + ticket.getTicketId());
        title.addClassName("ticket-detail-title");

        Span location = new Span(formatTicketLocation(ticket));
        location.addClassName("ticket-detail-location");

        Span price = new Span(formatMoney(ticket.getPrice()));
        price.addClassName("ticket-detail-price");

        Span status = new Span(ticket.getStatus());
        status.addClassName("ticket-detail-status");

        Button barcode = new Button(shortBarcode(ticket.getSecureBarcode()));
        barcode.addClassName("ticket-detail-barcode-button");
        barcode.addClickListener(e -> openBarcodeDialog(ticket.getSecureBarcode()));

        // Span barcode = new Span(shortBarcode(ticket.getSecureBarcode()));
        // barcode.setTitle(ticket.getSecureBarcode());
        // barcode.addClassName("ticket-detail-barcode");

        card.add(title, location, price, status, barcode);
        wrapper.add(card);
    }

    return wrapper;
}

private void openBarcodeDialog(String barcodeValue) {
    Dialog dialog = new Dialog();
    dialog.addClassName("barcode-dialog");

    VerticalLayout layout = new VerticalLayout();
    layout.setAlignItems(FlexComponent.Alignment.CENTER);
    layout.setSpacing(true);

    Span title = new Span("סריקת כרטיס");
    title.addClassName("barcode-dialog-title");

    Image barcodeImage = new Image(generateQrCodeDataUrl(barcodeValue), "QR code");
    barcodeImage.addClassName("barcode-image");

    Span code = new Span(barcodeValue);
    code.addClassName("barcode-full-code");

    Button close = new Button("סגור", e -> dialog.close());

    layout.add(title, barcodeImage, code, close);
    dialog.add(layout);
    dialog.open();
}

private String generateQrCodeDataUrl(String value) {
    try {
        int width = 420;
        int height = 140;

        BitMatrix bitMatrix = new MultiFormatWriter()
                .encode(value, BarcodeFormat.QR_CODE, width, height);

        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);

        String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
        return "data:image/png;base64," + base64;

    } catch (Exception e) {
        throw new RuntimeException("Failed to generate barcode", e);
    }
}


private String formatTicketLocation(PurchaseDTO ticket) {
    if (ticket.getRow() == 0 && ticket.getChair() == 0) {
        return "עמידה · אזור כללי";
    }
    return "שורה " + ticket.getRow() + " · כיסא " + ticket.getChair();
}

private String shortBarcode(String barcode) {
    if (barcode == null || barcode.isBlank()) {
        return "ברקוד לא זמין";
    }

    return barcode.length() > 18
            ? barcode.substring(0, 8) + "..." + barcode.substring(barcode.length() - 6)
            : barcode;
}

    public static final class MyPurchaseRow {
        private final String purchaseId;
        private final String eventName;
        private final String ticketsCount;
        private final String totalAmount;
        private final String statusLabel;
        private final StatusBadge.Type statusType;
        private final String actionText;
        private final List<PurchaseDTO> tickets;

        public MyPurchaseRow(
                String purchaseId,
                String eventName,
                String ticketsCount,
                String totalAmount,
                String statusLabel,
                StatusBadge.Type statusType,
                String actionText,List<PurchaseDTO> tickets
        ) {
            this.purchaseId = purchaseId;
            this.eventName = eventName;
            this.ticketsCount = ticketsCount;
            this.totalAmount = totalAmount;
            this.statusLabel = statusLabel;
            this.statusType = statusType;
            this.actionText = actionText;
            this.tickets=tickets;
        }

        public String getPurchaseId() {
            return purchaseId;
        }

        public String getEventName() {
            return eventName;
        }

        public String getTicketsCount() {
            return ticketsCount;
        }

        public String getTotalAmount() {
            return totalAmount;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public StatusBadge.Type getStatusType() {
            return statusType;
        }

        public String getActionText() {
            return actionText;
        }
       public List<PurchaseDTO> getTickets(){
        return tickets;
       }
    }
}