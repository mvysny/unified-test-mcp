package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.AbstractLogin;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * At {@code login}: a LoginForm, and a button opening a LoginOverlay. Either takes any username with the password
 * {@code secret}, and shows its error for any other.
 *
 * <pre>
 * Log in: Username [    ] Password [    ] [Log in] [Forgot password]
 * [Sign in]   opens the LoginOverlay "Mail"
 * Status: Signed out
 * </pre>
 */
@Route("login")
public class LoginView extends VerticalLayout {
    private final Span status = new Span("Signed out");

    public LoginView() {
        LoginForm form = new LoginForm();
        form.addLoginListener(e -> logIn(form, e));
        form.addForgotPasswordListener(e -> status.setText("Forgot password"));

        LoginOverlay overlay = new LoginOverlay();
        overlay.setTitle("Mail");
        overlay.setDescription("Your mail, anywhere");
        overlay.addLoginListener(e -> {
            if (logIn(overlay, e)) {
                overlay.setOpened(false);
            }
        });

        add(form, new Button("Sign in", e -> overlay.setOpened(true)), status);
    }

    private boolean logIn(AbstractLogin login, AbstractLogin.LoginEvent e) {
        boolean ok = e.getPassword().equals("secret");
        login.setError(!ok);
        login.setEnabled(true);
        if (ok) {
            status.setText("Signed in as " + e.getUsername());
        }
        return ok;
    }
}
