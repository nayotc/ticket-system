package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.PresentationLayer.Components.PublicHeader;

@Component
public class PublicHeaderPresenter implements PublicHeader.HeaderPresenter {

    private final CompanyPresenter companyPresenter;

    public PublicHeaderPresenter(CompanyPresenter companyPresenter) {
        this.companyPresenter = companyPresenter;
    }

    @Override
    public Long getFirstManagedCompanyId(String sessionToken) throws Exception {
        return companyPresenter.getFirstManagedCompanyId(sessionToken);
    }

    @Override
    public int getActiveCartItemsCount(String sessionToken) {
        return 0;
    }
}