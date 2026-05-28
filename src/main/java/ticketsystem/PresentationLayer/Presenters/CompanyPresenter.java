package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.DTO.CompanyDTO;

@Component
public class CompanyPresenter {

    private final CompanyService companyService;

    public CompanyPresenter(CompanyService companyService) {
        this.companyService = companyService;
    }

    public CompanyDTO createProductionCompany(String sessionToken, String companyName) {
        try {
            return companyService.createProductionCompany(sessionToken, companyName);

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("Company creation failed. Please try again.");
        }
    }

    public CompanyDTO closeProductionCompany(String sessionToken, long companyId) {
        try {
            return companyService.closeProductionCompany(sessionToken, companyId);

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("Company closing failed. Please try again.");
        }
    }

    public CompanyDTO reopenProductionCompany(String sessionToken, long companyId) {
        try {
            return companyService.reopenProductionCompany(sessionToken, companyId);

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("Company reopening failed. Please try again.");
        }
    }

    public CompanyDTO getCompanyDetails(String sessionToken, long companyId) {
        try {
            return companyService.getCompanyDetails(sessionToken, companyId);

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("Could not load company details. Please try again.");
        }
    }
}
