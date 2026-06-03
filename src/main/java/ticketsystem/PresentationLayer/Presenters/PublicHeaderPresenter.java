package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.PresentationLayer.Components.PublicHeader;

@Component
public class PublicHeaderPresenter implements PublicHeader.HeaderPresenter {

    private final CompanyPresenter companyPresenter;
    private final ReservationPresenter reservationPresenter;
    private final SystemAdminPresenter systemAdminPresenter;

    public PublicHeaderPresenter(CompanyPresenter companyPresenter, ReservationPresenter reservationPresenter, SystemAdminPresenter systemAdminPresenter) {
        this.companyPresenter = companyPresenter;
        this.reservationPresenter = reservationPresenter;
        this.systemAdminPresenter = systemAdminPresenter;
    }

    @Override
    public Long getFirstManagedCompanyId(String sessionToken) throws Exception {
        return companyPresenter.getFirstManagedCompanyId(sessionToken);
    }

    @Override
    public int getActiveCartItemsCount(String sessionToken) {
        try {
            if (sessionToken == null || sessionToken.isBlank()) {
                return 0;
            }

            return reservationPresenter.getActiveCartItemsCount(sessionToken);

        } catch (Exception exception) {
            return 0;
        }
    }

    @Override
    public boolean canAccessSystemAdmin(String token) throws Exception {
        return systemAdminPresenter.canAccessSystemAdmin(token);
    }
    
}