package ticketsystem.PresentationLayer.Views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.PresentationLayer.Components.EventCard;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.PageHeader;
import ticketsystem.PresentationLayer.Components.SearchPanel;
import ticketsystem.PresentationLayer.Constants.Photos;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.MainLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.QueryParameters;
import ticketsystem.PresentationLayer.Presenters.EventCatalogPresenter;
import ticketsystem.PresentationLayer.Presenters.EventCatalogPresenter.HomeEventCard;

import java.util.Map;

@PageTitle("TixNow")
@Route(value = UiRoutes.HOME, layout = MainLayout.class)
public class Home extends PageContainer {

    private final EventCatalogPresenter eventCatalogPresenter;

    public Home(EventCatalogPresenter eventCatalogPresenter) {
        super();
        this.eventCatalogPresenter = eventCatalogPresenter;

        add(
                createHero(),
                createPopularEventsSection()
        );
    }

    private Div createHero() {
        Div hero = new Div();
        hero.addClassName("hero-section");

        Div background = new Div();
        background.addClassName("hero-background");
        background.getStyle().set("background-image", "url(" + Photos.HERO_BACKGROUND + ")");

        Div gradient = new Div();
        gradient.addClassName("hero-gradient");

        Div content = new Div();
        content.addClassName("hero-content");

        content.getStyle()
                .set("width", "100%")
                .set("max-width", "100%")
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center");

        PageHeader header = new PageHeader(
                "החוויה הבאה שלך מתחילה כאן",
                "מצא את הכרטיסים הטובים ביותר להופעות, פסטיבלים ואירועי ספורט. חיפוש מהיר, רכישה מאובטחת."
        );
        header.addClassName("hero-centered-header");
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        header.getStyle()
                .set("width", "100%")
                .set("max-width", "900px")
                .set("margin", "0 auto")
                .set("text-align", "center");

        Div searchPanelWrapper = createCenteredSearchPanel();

        content.add(header, searchPanelWrapper);
        hero.add(background, gradient, content);

        return hero;
    }

    private Div createCenteredSearchPanel() {
        SearchPanel searchPanel = new SearchPanel();
        searchPanel.getSearchButton().addClickListener(event -> navigateToSearchResults(searchPanel));

        searchPanel.getStyle()
                .set("margin-left", "auto")
                .set("margin-right", "auto");

        Div wrapper = new Div(searchPanel);
        wrapper.addClassName("home-search-panel-wrapper");

        wrapper.getStyle()
                .set("width", "100%")
                .set("max-width", "1240px")
                .set("margin", "0 auto")
                .set("display", "flex")
                .set("justify-content", "center")
                .set("align-items", "center")
                .set("box-sizing", "border-box");

        return wrapper;
    }

    private void navigateToSearchResults(SearchPanel searchPanel) {
        Map<String, String> params = eventCatalogPresenter.buildSearchQueryParameters(
                searchPanel.getFreeText().getValue(),
                searchPanel.getFromDate().getValue(),
                searchPanel.getToDate().getValue(),
                searchPanel.getLocation().getValue(),
                searchPanel.getCategory().getValue(),
                searchPanel.getArtist().getValue(),
                searchPanel.getMinPriceValue(),
                searchPanel.getMaxPriceValue(),
                searchPanel.getMaxPriceLimit(),
                searchPanel.getEventRateValue(),
                searchPanel.getCompanyRateValue()
        );

        if (params.isEmpty()) {
            UI.getCurrent().navigate(UiRoutes.SEARCH_RESULTS);
            return;
        }

        UI.getCurrent().navigate(
                UiRoutes.SEARCH_RESULTS,
                QueryParameters.simple(params)
        );
    }

    private Div createPopularEventsSection() {
        Div section = new Div();
        section.addClassName("section-block");

        Div sectionInner = new Div();
        sectionInner.addClassName("section-inner");

        Div titleRow = new Div();
        titleRow.addClassName("section-title-row");

        Div titleText = new Div();

        H2 title = new H2("אירועים חמים");
        title.addClassName("section-title");

        Paragraph subtitle = new Paragraph("הכרטיסים שנחטפים עכשיו");
        subtitle.addClassName("section-subtitle");

        titleText.add(title, subtitle);

        Span viewAll = new Span("ראה הכל");
        viewAll.addClassName("section-link");

        titleRow.add(titleText, viewAll);

        Div grid = new Div();
        grid.addClassName("events-grid");
        eventCatalogPresenter.getFeaturedHomeEvents()
                .stream()
                .map(this::createEventCard)
                .forEach(grid::add);

        sectionInner.add(titleRow, grid);
        section.add(sectionInner);

        return section;
    }

    private EventCard createEventCard(HomeEventCard event) {
        return new EventCard(
                event.category(),
                event.title(),
                event.date(),
                event.location(),
                event.priceText(),
                event.imageUrl(),
                event.urgent(),
                event.companyName(),
                event.companyId(),
                event.eventId(),
                event.saleStatus(),
                event.hasLottery()
        );
    }

    private Div createVipCard() {
        Div vipCard = new Div();
        vipCard.addClassName("vip-card");

        Image vipImage = new Image(Photos.VIP_EXPERIENCE, "VIP Experience");
        vipImage.addClassName("vip-image");

        Div vipOverlay = new Div();
        vipOverlay.addClassName("vip-overlay");

        Div vipText = new Div();
        vipText.addClassName("vip-text");

        Span vipBadge = new Span("חבילות VIP");
        vipBadge.addClassName("vip-badge");

        H2 vipTitle = new H2("שדרגו את הערב");
        vipTitle.addClassName("vip-title");

        Paragraph vipDescription = new Paragraph(
                "גישה למתחמים סגורים, שירות אישי, והמקומות הטובים ביותר באולם."
        );
        vipDescription.addClassName("vip-description");

        vipText.add(vipBadge, vipTitle, vipDescription);
        vipCard.add(vipImage, vipOverlay, vipText);

        return vipCard;
    }

    private Div createSmallHighlightCard(String icon, String title, String text) {
        Div card = new Div();
        card.addClassName("small-highlight-card");

        Span iconElement = new Span(icon);
        iconElement.addClassName("small-highlight-icon");

        H2 titleElement = new H2(title);
        titleElement.addClassName("small-highlight-title");

        Paragraph textElement = new Paragraph(text);
        textElement.addClassName("small-highlight-text");

        card.add(iconElement, titleElement, textElement);

        return card;
    }
}