package com.example;   

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "login", autoLayout = false)   // bez menu bocznego
@PageTitle("Logowanie")
public class LoginView extends Main {

    public LoginView() {
        LoginI18n i18n = LoginI18n.createDefault();

        LoginI18n.Form form = i18n.getForm();
        form.setTitle("Wypożyczalnia kostiumów");
        form.setUsername("Email");
        form.setPassword("Hasło");
        form.setSubmit("Zaloguj się");
        form.setForgotPassword("Nie pamiętam hasła");

        LoginI18n.ErrorMessage error = i18n.getErrorMessage();
        error.setTitle("Nieprawidłowe dane");
        error.setMessage("Sprawdź email i hasło, a następnie spróbuj ponownie.");

        LoginForm login = new LoginForm(i18n);
        // Na razie bez logowania: tylko komunikat zamiast prawdziwej autoryzacji.
        login.addLoginListener(e ->
                Notification.show("Logowanie nie jest jeszcze podłączone"));
        login.addForgotPasswordListener(e ->
                Notification.show("Resetowanie hasła nie jest jeszcze podłączone"));

        VerticalLayout layout = new VerticalLayout(login);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.setSizeFull();

        add(layout);
        setSizeFull();
    }
}