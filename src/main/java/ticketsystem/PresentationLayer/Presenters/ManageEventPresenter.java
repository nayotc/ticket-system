package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.ApplicationLayer.LotteryService;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DTO.Event.EventMapDTO;
import ticketsystem.DTO.DiscountDTO;
import ticketsystem.DTO.DiscountPolicyDTO;
import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DTO.PurchaseRuleDTO;
import ticketsystem.DTO.PurchaseRuleType;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
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
                    rollbackCreatedEvent(request.sessionId(), eventId);
                    logger.logEvent("Failed to create lottery for event " + eventId + ": " + lotteryException.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
                    throw new PresentationException("אירעה שגיאה בעת יצירת הגרלה. נסו שוב.");
                }
            }

            return eventId;

        } catch (PresentationException exception) {
            throw exception;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw presentationException(exception.getMessage());
        } catch (Exception exception) {
            logger.logEvent("Unexpected error while creating event: " + exception.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("אירעה שגיאה בעת יצירת האירוע. נסו שוב.");
        }
    }

    @Override
    public EventDTO getEvent(String sessionId, Long eventId) {
        try {
            validateEventId(eventId);
            return eventService.getEvent(sessionId, eventId);
        } catch (PresentationException exception) {
            throw exception;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw presentationException(exception.getMessage());
        } catch (Exception exception) {
            logger.logEvent("Unexpected error while loading event " + eventId + ": " + exception.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("אירעה שגיאה בעת טעינת פרטי האירוע. נסו שוב.");
        }
    }

    @Override
    public boolean defineEventMap(String sessionId, Long eventId, EventMapDTO mapDTO) {
        try {
            validateDefineEventMapRequest(eventId, mapDTO);
            return Boolean.TRUE.equals(eventService.defineEventMap(sessionId, eventId, mapDTO));
        } catch (PresentationException exception) {
            throw exception;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw presentationException(exception.getMessage());
        } catch (Exception exception) {
            logger.logEvent("Unexpected error while defining map for event " + eventId + ": " + exception.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("אירעה שגיאה בעת שמירת מפת האולם. נסו שוב.");
        }
    }



    @Override
    public boolean updateEvent(EditEvent.UpdateEventRequest request) {
        try {
            validateUpdateEventRequest(request);
            return Boolean.TRUE.equals(eventService.updateEvent(request.sessionId(), request.event()));
        } catch (PresentationException exception) {
            throw exception;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw presentationException(exception.getMessage());
        } catch (Exception exception) {
            logger.logEvent("Unexpected error while updating event: " + exception.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("אירעה שגיאה בעת עדכון פרטי האירוע. נסו שוב.");
        }
    }

    @Override
    public EditEvent.PurchasePolicyExpressionDraftDTO loadEventPurchasePolicy(String token, Long eventId) {
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
    }

    @Override
    public void saveEventPurchasePolicy(String token, Long eventId, EditEvent.PurchasePolicyExpressionDraftDTO purchaseDraft) {
        try {
            validateEventId(eventId);
            PurchasePolicyDTO appPurchasePolicy = mapToAppPurchasePolicyExpression(purchaseDraft);
            eventService.setEventPurchasePolicy(token, eventId, appPurchasePolicy);
        } catch (PresentationException exception) {
            throw exception;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw presentationException(exception.getMessage());
        } catch (Exception exception) {
            logger.logEvent("Unexpected error while saving purchase policy for event " + eventId + ": " + exception.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("אירעה שגיאה בעת שמירת מדיניות הרכישה. נסו שוב.");
        }
    }


    @Override
    public void cancelEvent(String token, Long eventId) {
        try {
            validateEventId(eventId);
            eventService.cancelEvent(token, eventId);
        } catch (PresentationException exception) {
            throw exception;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw presentationException(exception.getMessage());
        } catch (Exception exception) {
            logger.logEvent("Unexpected error while canceling event " + eventId + ": " + exception.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("אירעה שגיאה בעת ביטול האירוע. נסו שוב.");
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
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw presentationException(exception.getMessage());
        } catch (Exception exception) {
            logger.logEvent("Unexpected error while saving discount policy for event " + eventId + ": " + exception.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("אירעה שגיאה בעת שמירת מדיניות ההנחות. נסו שוב.");
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
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw presentationException(exception.getMessage());
        } catch (Exception exception) {
            logger.logEvent("Unexpected error while updating sale status for event " + eventId + ": " + exception.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("אירעה שגיאה בעת עדכון מצב המכירה. נסו שוב.");
        }
    }

    @Override
    public boolean hasLottery(String sessionId, Long eventId) {
        try {
            validateEventId(eventId);
            return lotteryService.hasLotteryForEvent(sessionId, eventId);
        } catch (PresentationException exception) {
            throw exception;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw presentationException(exception.getMessage());
        } catch (Exception exception) {
            logger.logEvent("Unexpected error while checking lottery for event " + eventId + ": " + exception.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("אירעה שגיאה בעת בדיקת הגרלה לאירוע. נסו שוב.");
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
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw presentationException(exception.getMessage());
        } catch (Exception exception) {
            logger.logEvent("Unexpected error while conducting lottery for event " + eventId + ": " + exception.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("אירעה שגיאה בעת ביצוע ההגרלה. נסו שוב.");
        }
    }
    @Override
public int getEventCapacity(String sessionId, Long eventId) {
    try {
        validateEventId(eventId);
        return eventService.getEventCapacity(sessionId, eventId);

    } catch (PresentationException exception) {
        throw exception;
    } catch (IllegalArgumentException | IllegalStateException exception) {
        throw presentationException(exception.getMessage());
    } catch (Exception exception) {
        logger.logEvent(
                "Unexpected error while loading event capacity for event "
                        + eventId + ": " + exception.getMessage(),
                LogbackSystemLogger.LogLevel.DEBUG
        );
        throw new PresentationException("אירעה שגיאה בעת טעינת קיבולת האירוע. נסו שוב.");
    }
}
@Override
public int getSoldTicketsCount(String sessionId, Long eventId) {
    try {
        validateEventId(eventId);
        return eventService.getSoldTicketsCount(sessionId, eventId);

    } catch (PresentationException exception) {
        throw exception;
    } catch (IllegalArgumentException | IllegalStateException exception) {
        throw presentationException(exception.getMessage());
    } catch (Exception exception) {
        logger.logEvent(
                "Unexpected error while loading sold tickets count for event "
                        + eventId + ": " + exception.getMessage(),
                LogbackSystemLogger.LogLevel.DEBUG
        );
        throw new PresentationException("אירעה שגיאה בעת טעינת נתוני המכירות. נסו שוב.");
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
            default -> throw new PresentationException("Unknown purchase rule field selected.");
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

        default -> throw new PresentationException("Unknown discount type selected.");
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

    private String translateError(String message) {
        if (message == null || message.isBlank()) {
            return "אירעה שגיאה. נסו שוב.";
        }

        if (message != null && (
                message.contains("JWT") ||
                message.contains("expired") ||
                message.contains("Invalid") ||
                message.contains("Invalid session ID") ||
                message.contains("Token is missing or null") ||
                message.contains("Session is no longer active") ||
                message.contains("Invalid or expired security token")
        )) {
            return message;
        }

        if (message.startsWith("Map elements cannot overlap")) {
            return "לא ניתן לשמור את מפת האולם כי קיימים אלמנטים שחופפים במיקום. הזז את האלמנטים כך שלא יכסו אחד את השני.";
        }
        if (message.contains("Cancellation failed")
            || message.contains("Some refunds were not completed")) {
        return "ביטול האירוע נכשל. חלק מההחזרים לא הושלמו. ניתן לנסות שוב מאוחר יותר.";
    }

        return switch (message) {
            case "Invalid token.", "Invalid session ID" -> "אנא התחבר מחדש";
            case "User does not have permission to create event for this company.",
                 "User does not have permission to create an event" -> "אין לך הרשאות ליצור אירוע עבור החברה הזו.";
            case "User does not have permission to define event map" -> "אין לך הרשאה להגדיר מפת אולם לאירוע הזה.";
            case "Event name cannot be empty." -> "שם האירוע לא יכול להיות ריק.";
            case "Event date cannot be in the past." -> "תאריך האירוע לא יכול להיות בעבר.";
            case "Event location cannot be empty." -> "יש לבחור מיקום לאירוע.";
            case "Traffic threshold must be a positive integer." -> "סף התנועה חייב להיות מספר שלם חיובי.";
            case "Event category cannot be empty." -> "קטגוריית האירוע לא יכולה להיות ריקה.";
            case "Artist name cannot be empty." -> "שם האמן לא יכול להיות ריק.";
            case "Price must be a positive number." -> "המחיר חייב להיות מספר חיובי.";
            case "Event not found" -> "האירוע לא נמצא.";
            case "Event ID cannot be null" -> "מזהה האירוע חסר.";
            case "Map data cannot be null" -> "פרטי המפה חסרים.";
            case "Map size must be positive" -> "גודל המפה חייב להיות חיובי.";
            case "Map must contain at least one element" -> "המפה חייבת להכיל לפחות אלמנט אחד.";
            case "Cannot change name of an active event" -> "לא ניתן לשנות שם של אירוע פעיל.";
            case "Cannot change ticket price of an active event" -> "לא ניתן לשנות מחיר כרטיס של אירוע פעיל.";
            case "Event was updated by another request" -> "האירוע עודכן במקום אחר. טען מחדש ונסה שוב.";
            case "Purchase policy data cannot be null" -> "פרטי מדיניות הרכישה חסרים.";
            case "Discount composition type cannot be null" -> "יש לבחור לוגיקת שילוב הנחות.";
            case "Discount data cannot be null" -> "פרטי ההנחה חסרים.";
            case "Discount name cannot be empty" -> "שם ההנחה לא יכול להיות ריק.";
            case "Discount percentage must be positive" -> "אחוז ההנחה חייב להיות חיובי.";
            case "Discount type cannot be empty" -> "יש לבחור סוג הנחה.";
            case "Unsupported discount type" -> "סוג ההנחה אינו נתמך.";
            case "Discount condition cannot be empty" -> "יש להזין תנאי להנחה מותנית.";
            case "Unsupported discount condition" -> "התנאי שהוזן אינו קיים ב-ConditionalDiscount.Condition.";
            case "Sale status cannot be null" -> "יש לבחור מצב מכירה.";
            case "Cannot move to pre-sale from current sale status" -> "ניתן להתחיל מכירה מוקדמת רק כאשר המכירה טרם נפתחה.";
            case "Cannot open regular sale from current sale status" -> "ניתן לפתוח מכירה רגילה רק לפני מכירה או מתוך מכירה מוקדמת.";
            case "User does not have permission to update event sale status" -> "אין לך הרשאה לעדכן מצב מכירה לאירוע הזה.";
            case "Event map must contain at least one seating area or standing area" -> "מפת האירוע חייבת להכיל לפחות אזור ישיבה או אזור עמידה אחד.";
            case "Event is already canceled" -> "האירוע כבר מבוטל.";
            case "Event Event does not exist" -> "האירוע לא קיים.";
            case "Event cancellation failed. Please try again later to complete the cancellation process." -> "ביטול אירוע נכשל, נסה שוב מאוחר יותר. ";
            default -> message;
        };
    }

    private PresentationException presentationException(String message) {
        return new PresentationException(translateError(message));
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
    } catch (IllegalArgumentException | IllegalStateException exception) {
        throw presentationException(exception.getMessage());
    } catch (Exception exception) {
        logger.logEvent(
                "Unexpected error while loading discount policy for event " + eventId + ": " + exception.getMessage(),
                LogbackSystemLogger.LogLevel.DEBUG
        );
        throw new PresentationException("אירעה שגיאה בעת טעינת מדיניות ההנחות. נסו שוב.");
    }
}

private String safeString(String value) {
    return value == null ? "" : value;
}
}
