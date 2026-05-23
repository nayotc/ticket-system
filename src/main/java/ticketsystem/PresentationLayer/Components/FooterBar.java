package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Span;

public class FooterBar extends Footer {

    public FooterBar() {
        addClassName("footer-bar");

        Div inner = new Div();
        inner.addClassName("footer-inner");

        // Right side - logo
        Div logoBlock = new Div();
        logoBlock.addClassName("footer-logo-block");

        Span brand = new Span("TixNow");
        brand.addClassName("footer-brand");

        logoBlock.add(brand);

        // Center - links
        Div links = new Div();
        links.addClassName("footer-links");
        links.add(
                new Anchor("#", "תנאי שימוש"),
                new Anchor("#", "מדיניות פרטיות"),
                new Anchor("#", "צור קשר")
        );

        // Left side - copyright
        Div copyBlock = new Div();
        copyBlock.addClassName("footer-copy-block");

        Span copyright = new Span("© 2026 TixNow. כל הזכויות שמורות.");
        copyright.addClassName("footer-copy");

        copyBlock.add(copyright);

        inner.add(logoBlock, links, copyBlock);
        add(inner);
    }
}