package ticketsystem.ApplicationLayer;

import java.util.ArrayList;
import java.util.List;

import ticketsystem.DTO.DiscountConditionDTO;
import ticketsystem.DTO.DiscountDTO;
import ticketsystem.DTO.DiscountPolicyDTO;
import ticketsystem.DomainLayer.discount.AndDiscountCondition;
import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.CouponDiscount;
import ticketsystem.DomainLayer.discount.DateRangeCondition;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountCondition;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.discount.DiscountTypes;
import ticketsystem.DomainLayer.discount.MaxTicketsCondition;
import ticketsystem.DomainLayer.discount.MinTicketsCondition;
import ticketsystem.DomainLayer.discount.VisibleDiscount;

public class DiscountPolicyMapper {

    public DiscountPolicyDTO toDTO(DiscountPolicy policy) {
        DiscountPolicyDTO dto = new DiscountPolicyDTO();

        if (policy == null) {
            return dto;
        }

        dto.setCompositionType(policy.getDiscountCompositionType());

        List<DiscountDTO> discounts = new ArrayList<>();
        if (policy.getDiscounts() != null) {
            for (DiscountTypes discount : policy.getDiscounts()) {
                discounts.add(toDTO(discount));
            }
        }

        dto.setDiscounts(discounts);
        return dto;
    }

    public DiscountDTO toDTO(DiscountTypes discount) {
        if (discount == null) {
            throw new IllegalArgumentException("Discount cannot be null");
        }

        DiscountDTO dto = new DiscountDTO();
        dto.setName(discount.getName());
        dto.setPercentage(discount.getPercentage());

        if (discount instanceof CouponDiscount coupon) {
            dto.setType("COUPON");
            dto.setCouponCode(coupon.getCouponCode());
            dto.setEndTime(coupon.getEndTime());
            return dto;
        }

        if (discount instanceof ConditionalDiscount conditional) {
            dto.setType("CONDITIONAL");
            dto.setConditions(toConditionDTOs(conditional.getCondition()));
            dto.setConditionText(buildConditionText(dto.getConditions()));
            return dto;
        }

        if (discount instanceof VisibleDiscount) {
            dto.setType("VISIBLE");
            return dto;
        }

        throw new IllegalArgumentException("Unsupported discount domain type");
    }

    public DiscountPolicy toDomain(DiscountPolicyDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Discount policy cannot be null");
        }

        DiscountCompositionType compositionType = dto.getCompositionType();

        if (compositionType == null) {
            throw new IllegalArgumentException("Discount composition type cannot be null");
        }

        DiscountPolicy policy = new DiscountPolicy(compositionType);

        if (dto.getDiscounts() == null) {
            return policy;
        }

        for (DiscountDTO discountDTO : dto.getDiscounts()) {
            policy.addDiscount(toDomain(discountDTO));
        }

        return policy;
    }

    private DiscountTypes toDomain(DiscountDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Discount cannot be null");
        }

        String type = normalizeType(dto.getType());

        return switch (type) {
            case "VISIBLE", "SIMPLE" -> new VisibleDiscount(
                    dto.getName(),
                    dto.getPercentage()
            );

            case "COUPON" -> new CouponDiscount(
                    dto.getName(),
                    dto.getCouponCode(),
                    dto.getPercentage(),
                    dto.getEndTime()
            );
            case "CONDITIONAL" -> new ConditionalDiscount(
                    dto.getName(),
                    dto.getPercentage(),
                    buildCondition(dto.getConditions())
            );

            default -> throw new IllegalArgumentException("Unsupported discount type: " + dto.getType());
        };
    }

    private DiscountCondition buildCondition(List<DiscountConditionDTO> conditionDTOs) {
        if (conditionDTOs == null || conditionDTOs.isEmpty()) {
            throw new IllegalArgumentException("Conditional discount must contain at least one condition");
        }

        List<DiscountCondition> conditions = new ArrayList<>();

        for (DiscountConditionDTO conditionDTO : conditionDTOs) {
            conditions.add(toDomainCondition(conditionDTO));
        }

        if (conditions.size() == 1) {
            return conditions.get(0);
        }

        return new AndDiscountCondition(conditions);
    }

    private DiscountCondition toDomainCondition(DiscountConditionDTO dto) {
        if (dto == null || dto.getType() == null || dto.getType().isBlank()) {
            throw new IllegalArgumentException("Discount condition cannot be empty");
        }

        String type = dto.getType().trim().toUpperCase();

        return switch (type) {
            case "MIN_TICKET", "MIN_TICKETS" -> {
                if (dto.getTicketThreshold() == null) {
                    throw new IllegalArgumentException("Minimum tickets condition requires ticket threshold");
                }
                yield new MinTicketsCondition(dto.getTicketThreshold());
            }

            case "MAX_TICKET", "MAX_TICKETS" -> {
                if (dto.getTicketThreshold() == null) {
                    throw new IllegalArgumentException("Maximum tickets condition requires ticket threshold");
                }
                yield new MaxTicketsCondition(dto.getTicketThreshold());
            }

            case "DATE", "DATE_RANGE" -> {
                if (dto.getStartTime() == null || dto.getEndTime() == null) {
                    throw new IllegalArgumentException("Date range condition requires start time and end time");
                }
                yield new DateRangeCondition(dto.getStartTime(), dto.getEndTime());
            }

            default -> throw new IllegalArgumentException("Unsupported discount condition: " + dto.getType());
        };
    }

    private List<DiscountConditionDTO> toConditionDTOs(DiscountCondition condition) {
        List<DiscountConditionDTO> result = new ArrayList<>();

        if (condition == null) {
            return result;
        }

        if (condition instanceof AndDiscountCondition and) {
            for (DiscountCondition inner : and.getConditions()) {
                result.addAll(toConditionDTOs(inner));
            }
            return result;
        }

        if (condition instanceof MinTicketsCondition min) {
            result.add(new DiscountConditionDTO(
                    "MIN_TICKET",
                    min.getMinTickets(),
                    null,
                    null
            ));
            return result;
        }

        if (condition instanceof MaxTicketsCondition max) {
            result.add(new DiscountConditionDTO(
                    "MAX_TICKET",
                    max.getMaxTickets(),
                    null,
                    null
            ));
            return result;
        }

        if (condition instanceof DateRangeCondition date) {
            result.add(new DiscountConditionDTO(
                    "DATE",
                    null,
                    date.getStartTime(),
                    date.getEndTime()
            ));
            return result;
        }

        throw new IllegalArgumentException("Unsupported discount condition domain type");
    }

    private String buildConditionText(List<DiscountConditionDTO> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();

        for (DiscountConditionDTO condition : conditions) {
            if (condition == null || condition.getType() == null) {
                continue;
            }

            String type = condition.getType().trim().toUpperCase();

            switch (type) {
                case "MIN_TICKET", "MIN_TICKETS" ->
                        parts.add("MIN_TICKETS " + condition.getTicketThreshold());

                case "MAX_TICKET", "MAX_TICKETS" ->
                        parts.add("MAX_TICKETS " + condition.getTicketThreshold());

                case "DATE", "DATE_RANGE" ->
                        parts.add("DATE " + condition.getStartTime() + " - " + condition.getEndTime());

                default -> {
                }
            }
        }

        return String.join(" AND ", parts);
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Discount type cannot be empty");
        }

        return type.trim().toUpperCase();
    }
}