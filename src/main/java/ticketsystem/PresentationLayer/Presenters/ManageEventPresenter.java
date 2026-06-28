package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.ApplicationLayer.LotteryService;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DTO.Event.EventMapDTO;
import ticketsystem.DTO.DiscountDTO;
import ticketsystem.DTO.DiscountPolicyDTO;
import ticketsystem.DTO.Event.IAreaDTO;
import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DTO.PurchaseRuleDTO;
import ticketsystem.DTO.PurchaseRuleType;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.PresentationLayer.Notifications.MessageTranslator;
import ticketsystem.PresentationLayer.Views.Management.CreateEvent;
import ticketsystem.PresentationLayer.Views.Management.EditEvent;
import ticketsystem.PresentationLayer.Views.Management.EditEvent.DiscountConditionType;
import ticketsystem.PresentationLayer.Views.Management.HallMapBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ManageEventPresenter implements CreateEvent.CreateEventPresenter, HallMapBuilder.HallMapBuilderPresenter, EditEvent.EditEventPresenter {

    private final EventService eventService;
    private final LotteryService lotteryService;
    private final LogbackSystemLogger logger;

    public ManageEventPresenter(EventService eventService, LotteryService lotteryService, LogbackSystemLogger logger) {
        this.eventService = eventService;
        this.lotteryService = lotteryService;
        this.logger = logger;
    }

    @Override
    public Long createEvent(CreateEvent.CreateEventRequest request) {
        try {
            validateCreateEventRequest(request);

            Long eventId = eventService.insertEvent(
                    request.sessionId(),
                    request.eventName(),
                    request.companyId(),
                    request.date(),
                    request.location(),
                    request.trafficThreshold(),
                    request.category(),
                    request.artist(),
                    request.price(),
                    request.mapHeight(),
                    request.mapWidth()
            );

            if (request.hasLottery()) {
                try {
                    lotteryService.addLottery(
                            request.sessionId(),
                            eventId,
                            request.companyId(),
                            request.lotteryWinnersNumber()
                    );
                } catch (Exception lotteryException) {
                    try {
                        eventService.rollbackCreatedEvent(request.sessionId(), eventId);
                    } catch (Exception rollbackException) {
                        lotteryException.addSuppressed(rollbackException);
                    }
                    throw presentationException("createEventLottery", lotteryException, "לא ניתן ליצור את האירוע עם ההגרלה. נסו שוב.");
                }
            }

            return eventId;

        } catch (PresentationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw presentationException("operationName", exception, "לא ניתן ליצור את האירוע. בדקו את הפרטים ונסו שוב.");
        }
    }

    @Override
    public EventDTO getEvent(String sessionId, Long eventId) {
        try {
            validateEventId(eventId);
            return eventService.getEvent(sessionId, eventId);
        } catch (PresentationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw presentationException("operationName", exception, "לא ניתן לטעון את פרטי האירוע. רעננו את העמוד ונסו שוב.");
        }
    }

    @Override
    public boolean defineEventMap(String sessionId, Long eventId, EventMapDTO mapDTO) {
        try {
            validateDefineEventMapRequest(eventId, mapDTO);
            return Boolean.TRUE.equals(eventService.defineEventMap(sessionId, eventId, mapDTO));
        } catch (PresentationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw presentationException("operationName", exception, "לא ניתן לשמור את מפת האולם. בדקו את המפה ונסו שוב.");
        }
    }

    @Override
    public boolean updateActiveEventMap(String sessionId, Long eventId, List<IAreaDTO> newAreas, List<IAreaDTO> updatedAreas){
        try {
            if (newAreas == null || updatedAreas == null){
                throw new IllegalArgumentException("newAreas or updatedAreas cannot be null");
            }
            validateEventId(eventId);
            return Boolean.TRUE.equals(eventService.updateActiveEvantMap(sessionId, eventId, newAreas, updatedAreas));
        } catch (PresentationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw presentationException("operationName", exception, "לא ניתן לעדכן את מפת האולם. רעננו את העמוד ונסו שוב.");
        }
    }

    @Override
    public EventMapDTO getEventMap(String sessionId, Long eventId) {
        try {
            validateEventId(eventId);
            return eventService.getEventMap(sessionId, eventId);
        } catch (PresentationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw presentationException("operationName", exception, "לא ניתן לטעון את מפת האירוע. רעננו את העמוד ונסו שוב.");
        }
    }

    @Override
    public boolean updateEvent(EditEvent.UpdateEventRequest request) {
        try {
            validateUpdateEventRequest(request);
            return Boolean.TRUE.equals(eventService.updateEvent(request.sessionId(), request.event()));
        } catch (PresentationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw presentationException("operationName", exception,  "לא ניתן לעדכן את פרטי האירוע. בדקו את הפרטים ונסו שוב.");
        }
    }

    @Override
    public EditEvent.PurchasePolicyExpressionDraftDTO loadEventPurchasePolicy(String token, Long eventId) {
        try {
            validateEventId(eventId);

            /*
             * EventService currently exposes setEventPurchasePolicy(...), but the uploaded service
             * does not expose a matching getEventPurchasePolicy(...).
             * This returns an empty draft so the editor can work without inventing a new service API.
             */
            return new EditEvent.PurchasePolicyExpressionDraftDTO(
                    String.valueOf(eventId),
                    new EditEvent.PurchaseExpressionNodeDTO(
                            UUID.randomUUID().toString(),
                            EditEvent.PurchaseNodeType.GROUP,
                            EditEvent.LogicalOperator.AND,
                            null,
                            new ArrayList<>()
                    )
            );
        } catch (PresentationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw presentationException("operationName", exception,  "לא ניתן לטעון את פרטי מדיניות הרכישה. רעננו ונסו שוב.");
        }
    }

    @Override
    public void saveEventPurchasePolicy(String token, Long eventId, EditEvent.PurchasePolicyExpressionDraftDTO purchaseDraft) {
        try {
            validateEventId(eventId);
            PurchasePolicyDTO appPurchasePolicy = mapToAppPurchasePolicyExpression(purchaseDraft);
            eventService.setEventPurchasePolicy(token, eventId, appPurchasePolicy);
        } catch (PresentationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw presentationException("operationName", exception,"לא ניתן לשמור את מדיניות הרכישה. בדקו את הכללים ונסו שוב.");
        }
    }


    @Override
    public void cancelEvent(String token, Long eventId) {
        try {
            validateEventId(eventId);
            eventService.cancelEvent(token, eventId);
        } catch (PresentationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw presentationException("operationName", exception, "לא ניתן לבטל את האירוע כעת. נסו שוב מאוחר יותר.");
        }
    }



    @Override
    public void saveEventDiscountPolicy(String token, Long eventId, EditEvent.DiscountPolicyDraftDTO discountDraft) {
        try {
            validateEventId(eventId);

            DiscountPolicyDTO appDiscountPolicy = mapToAppDiscountPolicy(discountDraft);
            eventService.setEventDiscountPolicy(token, eventId, appDiscountPolicy);

        } catch (PresentationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw presentationException("operationName", exception, "לא ניתן לשמור את מדיניות ההנחות. בדקו את ההנחות ונסו שוב.");
        }
    }

    @Override
    public void updateEventSaleStatus(String token, Long eventId, SaleStatus targetStatus) {
        try {
            validateEventId(eventId);
            if (targetStatus == null) {
                throw new IllegalArgumentException("Sale status cannot be null");
            }

            eventService.updateEventSaleStatus(token, eventId, targetStatus);
        } catch (PresentationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw presentationException("operationName", exception, "לא ניתן לעדכן את מצב המכירה. רעננו את העמוד ונסו שוב.");
        }
    }

    @Override
    public boolean hasLottery(String sessionId, Long eventId) {
        try {
            validateEventId(eventId);
            return lotteryService.hasLotteryForEvent(sessionId, eventId);
        } catch (PresentationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw presentationException("operationName", exception, "לא ניתן לבדוק את פרטי ההגרלה. נסו שוב.");
        }
    }

    @Override
    public void conductLottery(String sessionId, Long eventId, Long companyId) {
        try {
            validateEventId(eventId);
            Long lotteryId = lotteryService.getLotteryIdByEventId(eventId); // Validate lottery existence before conducting draw
            lotteryService.closeLotteryRegistration(sessionId, lotteryId, companyId); // Ensure registration is closed before conducting draw
            lotteryService.conductLotteryDraw(sessionId, lotteryId, companyId);
        } catch (PresentationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw presentationException("operationName", exception,  "לא ניתן לבצע את ההגרלה כעת. בדקו את מצב ההגרלה ונסו שוב.");
        }
    }
    @Override
public int getEventCapacity(String sessionId, Long eventId) {
    try {
        validateEventId(eventId);
        return eventService.getEventCapacity(sessionId, eventId);

    } catch (PresentationException exception) {
        throw exception;
    } catch (Exception exception) {
        throw presentationException("operationName", exception,  "לא ניתן לטעון את קיבולת האירוע. רעננו את העמוד ונסו שוב.");
    }
}
@Override
public int getSoldTicketsCount(String sessionId, Long eventId) {
    try {
        validateEventId(eventId);
        return eventService.getSoldTicketsCount(sessionId, eventId);

    } catch (PresentationException exception) {
        throw exception;
    } catch (Exception exception) {
        throw presentationException("operationName", exception,"לא ניתן לטעון את נתוני המכירות. רעננו את העמוד ונסו שוב.");
    }
}



    private PurchasePolicyDTO mapToAppPurchasePolicyExpression(EditEvent.PurchasePolicyExpressionDraftDTO draft) {
        if (draft == null || draft.root() == null) {
            return new PurchasePolicyDTO(alwaysAllowRule());
        }

        return new PurchasePolicyDTO(mapToAppPurchaseNode(draft.root()));
    }

    private PurchaseRuleDTO mapToAppPurchaseNode(EditEvent.PurchaseExpressionNodeDTO node) {
        if (node == null) {
            return alwaysAllowRule();
        }

        if (node.type() == EditEvent.PurchaseNodeType.RULE) {
            if (node.rule() == null) {
                return alwaysAllowRule();
            }

            return mapToAppLeafRule(node.rule());
        }

        List<PurchaseRuleDTO> children = new ArrayList<>();
        if (node.children() != null) {
            for (EditEvent.PurchaseExpressionNodeDTO child : node.children()) {
                PurchaseRuleDTO mappedChild = mapToAppPurchaseNode(child);
                if (mappedChild.getType() != PurchaseRuleType.ALWAYS_ALLOW) {
                    children.add(mappedChild);
                }
            }
        }

        if (children.isEmpty()) {
            return alwaysAllowRule();
        }

        PurchaseRuleDTO appRule = new PurchaseRuleDTO();
        appRule.setType(node.operator() == EditEvent.LogicalOperator.OR ? PurchaseRuleType.OR : PurchaseRuleType.AND);
        appRule.setChildren(children);
        appRule.setValue(0);
        return appRule;
    }

    private PurchaseRuleDTO alwaysAllowRule() {
        PurchaseRuleDTO rule = new PurchaseRuleDTO();
        rule.setType(PurchaseRuleType.ALWAYS_ALLOW);
        rule.setValue(0);
        rule.setChildren(null);
        return rule;
    }

    private PurchaseRuleDTO mapToAppLeafRule(EditEvent.PurchaseRuleDTO uiRule) {
        PurchaseRuleDTO appRule = new PurchaseRuleDTO();
        appRule.setValue(uiRule.value());
        appRule.setChildren(null);

        switch (uiRule.field()) {
            case MIN_TICKETS -> appRule.setType(PurchaseRuleType.MIN_TICKETS);
            case MAX_TICKETS -> appRule.setType(PurchaseRuleType.MAX_TICKETS);
            case AGE -> appRule.setType(PurchaseRuleType.MIN_AGE);
            default -> throw new IllegalArgumentException("Unknown purchase rule field selected.");
        }

        return appRule;
    }

    private DiscountPolicyDTO mapToAppDiscountPolicy(EditEvent.DiscountPolicyDraftDTO draft) {
        DiscountPolicyDTO policyDTO = new DiscountPolicyDTO();

        if (draft == null) {
            policyDTO.setCompositionType(DiscountCompositionType.MAX);
            policyDTO.setDiscounts(new ArrayList<>());
            return policyDTO;
        }

        policyDTO.setCompositionType(
                draft.compositionStrategy() == EditEvent.DiscountCompositionStrategy.SUM
                        ? DiscountCompositionType.SUM
                        : DiscountCompositionType.MAX
        );

        List<DiscountDTO> appDiscounts = new ArrayList<>();
        if (draft.discounts() != null) {
            for (EditEvent.DiscountDTO uiDiscount : draft.discounts()) {
                appDiscounts.add(mapToAppDiscount(uiDiscount));
            }
        }

        policyDTO.setDiscounts(appDiscounts);
        
        return policyDTO;
    }
    private DiscountDTO mapToAppDiscount(EditEvent.DiscountDTO uiDiscount) {
    if (uiDiscount == null) {
        throw new IllegalArgumentException("Discount data cannot be null");
    }

    DiscountDTO appDiscount = new DiscountDTO();
    appDiscount.setName(uiDiscount.name());
    appDiscount.setPercentage(java.math.BigDecimal.valueOf(uiDiscount.value()));

    switch (uiDiscount.type()) {
        case SIMPLE -> appDiscount.setType("VISIBLE");

        case COUPON -> {
            appDiscount.setType("COUPON");
            appDiscount.setCouponCode(uiDiscount.couponCode());

            if (uiDiscount.validUntil() != null) {
                appDiscount.setEndTime(uiDiscount.validUntil().atTime(23, 59));
            }
        }

        case CONDITIONAL -> {
            appDiscount.setType("CONDITIONAL");

            if (uiDiscount.conditions() == null || uiDiscount.conditions().isEmpty()) {
                throw new IllegalArgumentException("Conditional discount must contain at least one condition");
            }

            List<ticketsystem.DTO.DiscountConditionDTO> appConditions = new ArrayList<>();

            for (EditEvent.DiscountConditionDTO condition : uiDiscount.conditions()) {
                if (condition == null || condition.conditionType() == null) {
                    throw new IllegalArgumentException("Discount condition cannot be empty");
                }

                ticketsystem.DTO.DiscountConditionDTO appCondition =
                        new ticketsystem.DTO.DiscountConditionDTO();

                switch (condition.conditionType()) {
                    case MIN_TICKET -> {
                        if (condition.ticketThreshold() == null) {
                            throw new IllegalArgumentException("Minimum tickets condition requires ticket threshold");
                        }

                        appCondition.setType("MIN_TICKET");
                        appCondition.setTicketThreshold(condition.ticketThreshold());
                    }

                    case MAX_TICKET -> {
                        if (condition.ticketThreshold() == null) {
                            throw new IllegalArgumentException("Maximum tickets condition requires ticket threshold");
                        }

                        appCondition.setType("MAX_TICKET");
                        appCondition.setTicketThreshold(condition.ticketThreshold());
                    }

                    case DATE -> {
                        if (condition.startTime() == null || condition.endTime() == null) {
                            throw new IllegalArgumentException("Date condition requires start time and end time");
                        }

                        appCondition.setType("DATE");
                        appCondition.setStartTime(condition.startTime());
                        appCondition.setEndTime(condition.endTime());
                    }
                }

                appConditions.add(appCondition);
            }

            appDiscount.setConditions(appConditions);
            appDiscount.setConditionText(
                    appConditions.stream()
                            .map(ticketsystem.DTO.DiscountConditionDTO::getType)
                            .collect(java.util.stream.Collectors.joining(" AND "))
            );

        }

        default -> throw new IllegalArgumentException("Unknown discount type selected.");
    }

    return appDiscount;
}

    private String toConditionName(EditEvent.DiscountConditionType conditionType) {
        if (conditionType == null) {
            throw new IllegalArgumentException("Discount condition cannot be empty");
        }

        return switch (conditionType) {
            case MIN_TICKET -> "MIN_TICKETS";
            case MAX_TICKET -> "MAX_TICKETS";
            case DATE -> "DATE";
        };
    }

    private Integer resolveTicketThreshold(String conditionText) {
        if (conditionText == null || conditionText.isBlank()) {
            return null;
        }

        String digits = conditionText.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void validateUpdateEventRequest(EditEvent.UpdateEventRequest request) {
        if (request == null || request.event() == null) {
            throw new IllegalArgumentException("Event data cannot be null");
        }

        validateEventId(request.event().id());
    }

    private void rollbackCreatedEvent(String sessionId, Long eventId) {
        try {
            eventService.rollbackCreatedEvent(sessionId, eventId);
        } catch (Exception rollbackException) {
            logger.logEvent(rollbackException.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("יצירת ההגרלה נכשלה, האירוע לא נוצר. יש לפנות למנהל מערכת.");
        }
    }

    private void validateCreateEventRequest(CreateEvent.CreateEventRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("פרטי האירוע חסרים.");
        }

        if (request.hasLottery() && (request.lotteryWinnersNumber() == null || request.lotteryWinnersNumber() <= 0)) {
            throw new IllegalArgumentException("כאשר נבחרת הגרלה חובה להזין מספר זוכים חיובי.");
        }
    }

    private void validateDefineEventMapRequest(Long eventId, EventMapDTO mapDTO) {
        validateEventId(eventId);

        if (mapDTO == null) {
            throw new IllegalArgumentException("Map data cannot be null");
        }

        if (mapDTO.size() == null || mapDTO.size().first() == null || mapDTO.size().second() == null
                || mapDTO.size().first() <= 0 || mapDTO.size().second() <= 0) {
            throw new IllegalArgumentException("Map size must be positive");
        }

        if (mapDTO.elements() == null || mapDTO.elements().isEmpty()) {
            throw new IllegalArgumentException("Map must contain at least one element");
        }
    }

    private void validateEventId(Long eventId) {
        if (eventId == null || eventId <= 0) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
    }

    private String resolveUserMessage(String originalMessage, String fallbackMessage) {
        String safeFallback = fallbackMessage == null || fallbackMessage.isBlank() ? "לא ניתן להשלים את הפעולה. נסו שוב." : fallbackMessage;

        if (originalMessage == null || originalMessage.isBlank()) {
            return safeFallback;
        }

        String normalizedMessage = originalMessage.toLowerCase();

        /*
         * Authentication errors are actionable.
         * Do not expose JWT or token implementation details.
         */
        if (normalizedMessage.contains("jwt")
                || normalizedMessage.contains("token")
                || normalizedMessage.contains("session")
                || normalizedMessage.contains("expired")) {
            return "פג תוקף ההתחברות. התחברו מחדש ונסו שוב.";
        }

        /*
         * Permission errors are actionable, but the user does not need
         * the internal permission name.
         */
        if (originalMessage.contains("User does not have permission")) {
            return "אין לך הרשאה לבצע פעולה זו.";
        }

        /*
         * Do not mention an ID, repository result or internal lookup.
         */
        if (originalMessage.equals("Event not found")
                || originalMessage.equals("Event does not exist")
                || originalMessage.equals("Error: Event not found.")
                || originalMessage.equals("Event Event does not exist")) {
            return "האירוע המבוקש אינו זמין עוד. רעננו את העמוד ונסו שוב.";
        }

        /*
         * Messages with dynamic technical suffixes.
         */
        if (originalMessage.startsWith("Map elements cannot overlap")) {
            return "לא ניתן לשמור את המפה כי קיימים בה אלמנטים חופפים. הזיזו אותם ונסו שוב.";
        }

        if (originalMessage.startsWith("Element is outside map bounds")) {
            return "אחד האלמנטים נמצא מחוץ לגבולות המפה. הזיזו אותו ונסו שוב.";
        }

        if (originalMessage.startsWith("Element location cannot be negative")) {
            return "לא ניתן למקם אלמנט מחוץ לשטח המפה.";
        }

        if (originalMessage.startsWith("Element size must be positive")) {
            return "גובה ורוחב האלמנט חייבים להיות גדולים מאפס.";
        }

        if (originalMessage.contains("Cancellation failed") || originalMessage.contains("Some refunds were not completed")) {
            return "לא ניתן היה להשלים את ביטול האירוע. נסו שוב מאוחר יותר.";
        }

        return switch (originalMessage) {
            /*
             * Safe validations created by this presenter.
             */
            case "פרטי האירוע חסרים." -> "יש להזין את פרטי האירוע.";

            case "כאשר נבחרת הגרלה חובה להזין מספר זוכים חיובי." -> originalMessage;

            /*
             * Event form validation.
             */
            case "Event name cannot be null or empty", "Event name cannot be empty." -> "יש להזין שם לאירוע.";
            case "Event date must be in the future", "Event date cannot be in the past." ->
                    "יש לבחור תאריך עתידי לאירוע.";
            case "Event location cannot be null", "Event location cannot be empty." -> "יש לבחור מיקום לאירוע.";
            case "Traffic threshold must be a positive number", "Traffic threshold must be a positive integer." ->
                    "סף העומס חייב להיות מספר חיובי.";
            case "Event category cannot be null", "Event category cannot be empty." -> "יש לבחור קטגוריה לאירוע.";
            case "Artist name cannot be null or empty", "Artist name cannot be empty." -> "יש להזין שם אמן או מופיע.";
            case "Price must be a non-negative number", "Price must be a positive number." ->
                    "מחיר הכרטיס אינו יכול להיות שלילי.";
            case "Cannot change name of an active event" -> "לא ניתן לשנות את שם האירוע לאחר הפעלתו.";
            case "Cannot change ticket price of an active event" -> "לא ניתן לשנות את מחיר הכרטיס לאחר הפעלת האירוע.";
            case "Cannot change event's company" -> "לא ניתן להעביר את האירוע לחברת הפקה אחרת.";
            case "Event was updated by another request" ->
                    "פרטי האירוע השתנו מאז פתיחת העמוד. רעננו את העמוד ונסו שוב.";
            /*
             * Map input and business rules.
             */
            case "Map size must be positive" -> "גובה ורוחב המפה חייבים להיות גדולים מאפס.";
            case "Map must contain at least one element" -> "יש להוסיף לפחות אלמנט אחד למפה.";
            case "Event map must contain at least one seating area or standing area" ->
                    "יש להוסיף למפה לפחות אזור ישיבה או אזור עמידה.";
            case "Event map can only be defined for a draft event" -> "ניתן להגדיר מפה מלאה רק לפני הפעלת האירוע.";
            case "Event map has already been defined" ->
                    "מפת האירוע כבר נשמרה. רעננו את העמוד כדי לראות את המפה העדכנית.";
            case "This map operation is only allowed for an active event" ->
                    "ניתן לבצע עדכון של המפה רק לאחר הפעלת האירוע.";
            case "Existing area location cannot be changed" -> "לא ניתן להזיז אזור קיים לאחר הפעלת האירוע.";
            case "Standing area capacity cannot be reduced",
                 "Standing area capacity cannot be reduced for an active event" ->
                    "לא ניתן להקטין את הקיבולת של אזור עמידה לאחר הפעלת האירוע.";
            case "Rows cannot be reduced for an active event", "Seating area rows cannot be reduced" ->
                    "לא ניתן להסיר שורות מאזור ישיבה לאחר הפעלת האירוע.";
            case "Columns cannot be reduced for an active event", "Seating area columns cannot be reduced" ->
                    "לא ניתן להסיר עמודות מאזור ישיבה לאחר הפעלת האירוע.";
            case "Area type cannot be changed" -> "לא ניתן לשנות את סוג האזור לאחר הפעלת האירוע.";
            case "Rows must be positive" -> "מספר השורות חייב להיות גדול מאפס.";
            case "Columns must be positive" -> "מספר העמודות חייב להיות גדול מאפס.";
            /*
             * Sale status.
             */
            case "Sale status cannot be null" -> "יש לבחור מצב מכירה.";
            case "Cannot move to pre-sale from current sale status" -> "לא ניתן לפתוח מכירה מוקדמת ממצב המכירה הנוכחי.";
            case "Cannot open regular sale from current sale status" -> "לא ניתן לפתוח מכירה רגילה ממצב המכירה הנוכחי.";
            case "Cannot return sale status to not started" -> "לא ניתן להחזיר את המכירה למצב טרם התחלה.";
            case "Sale status should move to sold out or ended only by the relevant business flow" ->
                    "מצב המכירה הזה מתעדכן אוטומטית ואינו ניתן לבחירה ידנית.";
            /*
             * Cancellation.
             */
            case "Event is already canceled", "Event is already cancelled" -> "האירוע כבר מבוטל.";
            case "Cannot cancel an event that has already occurred" -> "לא ניתן לבטל אירוע שכבר התקיים.";
            case "Only inactive or cancelled events can be removed" -> "לא ניתן למחוק אירוע פעיל.";
            /*
             * Discounts and policies.
             */
            case "Discount name cannot be empty" -> "יש להזין שם להנחה.";
            case "Discount percentage must be positive" -> "אחוז ההנחה חייב להיות גדול מאפס.";
            case "Discount type cannot be empty" -> "יש לבחור סוג הנחה.";
            case "Conditional discount must contain at least one condition" ->
                    "יש להוסיף לפחות תנאי אחד להנחה המותנית.";
            case "Discount condition cannot be empty" -> "יש להשלים את פרטי תנאי ההנחה.";
            case "Minimum tickets condition requires ticket threshold" -> "יש להזין את מספר הכרטיסים המינימלי.";
            case "Maximum tickets condition requires ticket threshold" -> "יש להזין את מספר הכרטיסים המקסימלי.";
            case "Date condition requires start time and end time" -> "יש לבחור מועד התחלה ומועד סיום לתנאי ההנחה.";
            case "Minimum price cannot be greater than maximum price" ->
                    "מחיר המינימום אינו יכול להיות גבוה ממחיר המקסימום.";

            /*
             * Everything else is technical or unexpected.
             */
            default -> safeFallback;
        };
    }

    private PresentationException presentationException(String operation, Exception originalException, String fallbackMessage) {
        String originalMessage = originalException == null ? null : originalException.getMessage();

        logger.logError(
                "Presenter operation failed"
                        + ", operation=" + operation
                        + ", exceptionType="
                        + (originalException == null
                        ? "unknown"
                        : originalException.getClass().getName())
                        + ", originalMessage=" + originalMessage,
                originalException
        );

        return new PresentationException(resolveUserMessage(originalMessage, fallbackMessage));
    }

    private EditEvent.DiscountPolicyDraftDTO mapToUiDiscountPolicyDraft(
        Long eventId,
        DiscountPolicyDTO policyDTO) {

    if (policyDTO == null) {
        return new EditEvent.DiscountPolicyDraftDTO(
                String.valueOf(eventId),
                EditEvent.DiscountCompositionStrategy.MAXIMUM,
                new ArrayList<>()
        );
    }

    List<EditEvent.DiscountDTO> uiDiscounts = new ArrayList<>();

    if (policyDTO.getDiscounts() != null) {
        for (ticketsystem.DTO.DiscountDTO appDiscount : policyDTO.getDiscounts()) {
            uiDiscounts.add(mapToUiDiscount(appDiscount));
        }
    }

    return new EditEvent.DiscountPolicyDraftDTO(
            String.valueOf(eventId),
            policyDTO.getCompositionType() == DiscountCompositionType.SUM
                    ? EditEvent.DiscountCompositionStrategy.SUM
                    : EditEvent.DiscountCompositionStrategy.MAXIMUM,
            uiDiscounts
    );
}

private EditEvent.DiscountDTO mapToUiDiscount(ticketsystem.DTO.DiscountDTO appDiscount) {
    if (appDiscount == null) {
        throw new IllegalArgumentException("Discount data cannot be null");
    }

    EditEvent.DiscountType uiType = mapToUiDiscountType(appDiscount.getType());

    return new EditEvent.DiscountDTO(
            null,
            appDiscount.getName(),
            uiType,
            EditEvent.DiscountValueType.PERCENTAGE,
            appDiscount.getPercentage() == null ? 0 : appDiscount.getPercentage().doubleValue(),
            uiType == EditEvent.DiscountType.COUPON ? safeString(appDiscount.getCouponCode()) : "",
            uiType == EditEvent.DiscountType.COUPON && appDiscount.getEndTime() != null
                    ? appDiscount.getEndTime().toLocalDate()
                    : null,
            uiType == EditEvent.DiscountType.CONDITIONAL
                    ? mapToUiDiscountConditions(appDiscount.getConditions())
                    : new ArrayList<>()
    );
}
private EditEvent.DiscountType mapToUiDiscountType(String type) {
    if (type == null) {
        return EditEvent.DiscountType.SIMPLE;
    }

    return switch (type.trim().toUpperCase()) {
        case "VISIBLE", "SIMPLE" -> EditEvent.DiscountType.SIMPLE;
        case "COUPON" -> EditEvent.DiscountType.COUPON;
        case "CONDITIONAL" -> EditEvent.DiscountType.CONDITIONAL;
        default -> EditEvent.DiscountType.SIMPLE;
    };
}

private List<EditEvent.DiscountConditionDTO> mapToUiDiscountConditions(
        List<ticketsystem.DTO.DiscountConditionDTO> appConditions) {

    List<EditEvent.DiscountConditionDTO> uiConditions = new ArrayList<>();

    if (appConditions == null) {
        return uiConditions;
    }

    for (ticketsystem.DTO.DiscountConditionDTO appCondition : appConditions) {
        if (appCondition == null || appCondition.getType() == null) {
            continue;
        }

        EditEvent.DiscountConditionType uiType =
                mapToUiDiscountConditionType(appCondition.getType());

        uiConditions.add(
                new EditEvent.DiscountConditionDTO(
                        uiType,
                        appCondition.getTicketThreshold(),
                        appCondition.getStartTime(),
                        appCondition.getEndTime()
                )
        );
    }

    return uiConditions;
}
private EditEvent.DiscountConditionType mapToUiDiscountConditionType(String type) {
    if (type == null) {
        throw new IllegalArgumentException("Discount condition cannot be empty");
    }

    return switch (type.trim().toUpperCase()) {
        case "MIN_TICKET", "MIN_TICKETS" -> EditEvent.DiscountConditionType.MIN_TICKET;
        case "MAX_TICKET", "MAX_TICKETS" -> EditEvent.DiscountConditionType.MAX_TICKET;
        case "DATE", "DATE_RANGE" -> EditEvent.DiscountConditionType.DATE;
        default -> throw new IllegalArgumentException("Unsupported discount condition: " + type);
    };
}

@Override
public EditEvent.DiscountPolicyDraftDTO loadEventDiscountPolicy(String token, Long eventId) {
    try {
        validateEventId(eventId);

        DiscountPolicyDTO policyDTO = eventService.getEventDiscountPolicy(token, eventId);

        return mapToUiDiscountPolicyDraft(eventId, policyDTO);

    } catch (PresentationException exception) {
        throw exception;
    } catch (Exception exception) {
        throw presentationException("operationName", exception, "לא ניתן לטעון את פרטי מדיניות ההנחות. רעננו את העמוד ונסו שוב");
    }
}

private String safeString(String value) {
    return value == null ? "" : value;
}
}
