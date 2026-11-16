package com.example;

import java.util.Locale;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/** Elementy wspólne dla widoków modułów (nagłówek, komunikaty, walidacja). */
final class WspolneElementy {

    private static final Locale PL = Locale.forLanguageTag("pl-PL");

    private WspolneElementy() {
    }

    /** Przycisk powrotu do strony głównej + tytuł + opcjonalne przyciski akcji po prawej. */
    static Div naglowek(String tytul, String opis, Component... akcje) {
        Button wroc = new Button("Strona główna", VaadinIcon.ARROW_LEFT.create(),
                e -> UI.getCurrent().navigate(""));
        wroc.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        H2 h = new H2(tytul);
        h.getStyle().set("margin", "0");
        Paragraph p = new Paragraph(opis);
        p.getStyle().set("margin", "0").set("opacity", "0.7");

        HorizontalLayout wiersz = new HorizontalLayout(new Div(h, p));
        if (akcje.length > 0) {
            wiersz.add(new HorizontalLayout(akcje));
        }
        wiersz.setWidthFull();
        wiersz.setAlignItems(FlexComponent.Alignment.CENTER);
        wiersz.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        wiersz.getStyle().set("flex-wrap", "wrap");

        Div blok = new Div(wroc, wiersz);
        blok.setWidthFull();
        blok.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)");
        return blok;
    }

    static void sukces(String tekst) {
        Notification n = Notification.show(tekst, 4000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    static void blad(String tekst) {
        Notification n = Notification.show(tekst, 4000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    /** Pole tekstowe wymagane (puste lub same spacje = błąd). */
    static boolean wymagane(HasValidation pole, String wartosc) {
        return wymagane(pole, wartosc == null || wartosc.isBlank(), "To pole jest wymagane");
    }

    /** Dowolne pole z własnym warunkiem błędu. */
    static boolean wymagane(HasValidation pole, boolean brak, String komunikat) {
        pole.setInvalid(brak);
        if (brak) {
            pole.setErrorMessage(komunikat);
        }
        return !brak;
    }

    static void bladPola(HasValidation pole, String komunikat) {
        pole.setErrorMessage(komunikat);
        pole.setInvalid(true);
    }

    static String zl(double kwota) {
        return String.format(PL, "%.2f zł", kwota);
    }
}