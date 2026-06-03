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
import ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.PresentationLayer.Views.Management.CreateEvent;
import ticketsystem.PresentationLayer.Views.Management.EditEvent;
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
    public EditEvent.DiscountPolicyDraftDTO loadEventDiscountPolicy(String token, Long eventId) {
        validateEventId(eventId);

        /*
         * EventService currently exposes add/remove/composition methods for event discounts,
         * but the uploaded service does not expose a full getEventDiscountPolicy(...).
         * This returns an empty draft so the editor can work and save new event discounts.
         */
        return new EditEvent.DiscountPolicyDraftDTO(
                String.valueOf(eventId),
                EditEvent.DiscountCompositionStrategy.MAXIMUM,
                new ArrayList<>()
        );
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

            if (appDiscountPolicy.getCompositionType() != null) {
                eventService.setEventDiscountCompositionType(token, eventId, appDiscountPolicy.getCompositionType());
            }

            if (discountDraft != null && discountDraft.discounts() != null) {
                for (EditEvent.DiscountDTO discount : discountDraft.discounts()) {
                    addUiDiscountToEvent(token, eventId, discount);
                }
            }
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
                appDiscount.setConditionText(toConditionName(uiDiscount.conditionType()));
                if (uiDiscount.endTime() != null) {
                    appDiscount.setEndTime(uiDiscount.endTime());
                }
            }
            default -> throw new PresentationException("Unknown discount type selected.");
        }

        return appDiscount;
    }

    private void addUiDiscountToEvent(String token, Long eventId, EditEvent.DiscountDTO discount) throws Exception {
        if (discount == null || discount.type() == null) {
            throw new IllegalArgumentException("Discount data cannot be null");
        }

        java.math.BigDecimal percentage = java.math.BigDecimal.valueOf(discount.value());

        switch (discount.type()) {
            case SIMPLE -> eventService.addVisibleDiscountToEvent(
                    token,
                    eventId,
                    discount.name(),
                    percentage
            );
            case COUPON -> eventService.addCouponDiscountToEvent(
                    token,
                    eventId,
                    discount.name(),
                    discount.couponCode(),
                    percentage,
                    discount.validUntil() == null ? null : discount.validUntil().atTime(23, 59)
            );
            case CONDITIONAL -> eventService.addConditionalDiscountToEvent(
                    token,
                    eventId,
                    discount.name(),
                    discount.startTime(),
                    discount.endTime(),
                    percentage,
                    parseCondition(discount.conditionType()),
                    discount.ticketThreshold()
            );
            default -> throw new IllegalArgumentException("Unsupported discount type");
        }
    }

    private void addAppDiscountToEvent(String token, Long eventId, DiscountDTO discount) throws Exception {
        if (discount == null || discount.getType() == null || discount.getType().isBlank()) {
            throw new IllegalArgumentException("Discount data cannot be null");
        }

        String type = discount.getType().trim().toUpperCase();

        switch (type) {
            case "VISIBLE", "SIMPLE" -> eventService.addVisibleDiscountToEvent(
                    token,
                    eventId,
                    discount.getName(),
                    discount.getPercentage()
            );
            case "COUPON" -> eventService.addCouponDiscountToEvent(
                    token,
                    eventId,
                    discount.getName(),
                    discount.getCouponCode(),
                    discount.getPercentage(),
                    discount.getEndTime()
            );
            case "CONDITIONAL" -> eventService.addConditionalDiscountToEvent(
                    token,
                    eventId,
                    discount.getName(),
                    null,
                    discount.getEndTime(),
                    discount.getPercentage(),
                    parseCondition(discount.getConditionText()),
                    resolveTicketThreshold(discount.getConditionText())
            );
            default -> throw new IllegalArgumentException("Unsupported discount type");
        }
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

    private Condition parseCondition(String conditionText) {
        if (conditionText == null || conditionText.isBlank()) {
            throw new IllegalArgumentException("Discount condition cannot be empty");
        }

        String normalized = conditionText.trim().toUpperCase();

        if (normalized.contains("DATE") || normalized.contains("TIME") || normalized.contains("תאריך")) {
            return firstExistingCondition(
                    "DATE",
                    "DATE_RANGE",
                    "TIME_RANGE",
                    "BETWEEN_DATES"
            );
        }

        if (normalized.contains("MIN") || normalized.contains("מינימום") || normalized.contains("לפחות")) {
            return firstExistingCondition(
                    "MIN_TICKET",
                    "MIN_TICKETS",
                    "MINIMUM_TICKET",
                    "MINIMUM_TICKETS"
            );
        }

        if (normalized.contains("MAX") || normalized.contains("מקסימום")) {
            return firstExistingCondition(
                    "MAX_TICKET",
                    "MAX_TICKETS",
                    "MAXIMUM_TICKET",
                    "MAXIMUM_TICKETS"
            );
        }

        throw new IllegalArgumentException("Unsupported discount condition");
    }

    private Condition parseCondition(EditEvent.DiscountConditionType conditionType) {
        if (conditionType == null) {
            throw new IllegalArgumentException("Discount condition cannot be empty");
        }

        return switch (conditionType) {
            case MIN_TICKET -> firstExistingCondition(
                    "MIN_TICKET",
                    "MIN_TICKETS",
                    "MINIMUM_TICKET",
                    "MINIMUM_TICKETS"
            );

            case MAX_TICKET -> firstExistingCondition(
                    "MAX_TICKET",
                    "MAX_TICKETS",
                    "MAXIMUM_TICKET",
                    "MAXIMUM_TICKETS"
            );

            case DATE -> firstExistingCondition(
                    "DATE",
                    "DATE_RANGE",
                    "TIME_RANGE",
                    "BETWEEN_DATES"
            );
        };
    }

    private Condition firstExistingCondition(String... candidates) {
        for (String candidate : candidates) {
            try {
                return Condition.valueOf(candidate);
            } catch (IllegalArgumentException ignored) {
                // Try next alias
            }
        }

        throw new IllegalArgumentException("Unsupported discount condition");
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
            default -> message;
        };
    }

    private PresentationException presentationException(String message) {
        return new PresentationException(translateError(message));
    }
}
