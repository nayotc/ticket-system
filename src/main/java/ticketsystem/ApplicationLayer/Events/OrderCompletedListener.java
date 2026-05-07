package ticketsystem.ApplicationLayer.Events;

import ticketsystem.DTO.OrderDTO;

public interface OrderCompletedListener {

    void onOrderCompleted(OrderDTO order);//this method will be called when the order is completed, and it will receive the order details as a parameter. The implementation of this method will define what actions to take when an order is completed, such as updating the purchase history or sending a confirmation email.
}



// we need to add this line where we initialize everithing
//orderService.addOrderListener(historyService);