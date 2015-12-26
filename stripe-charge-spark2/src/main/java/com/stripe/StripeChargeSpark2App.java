package com.stripe;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.ini4j.Ini;
import org.ini4j.IniPreferences;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stripe.exception.StripeException;
import com.stripe.model.Charge;


public final class StripeChargeSpark2App {
    private static final int CHARGE_AMOUNT = 400; // amount in cents
    private static final String CHARGE_CURRENCY = "usd";

    private static final Logger LOGGER
        = LoggerFactory.getLogger(StripeChargeSpark2App.class);

    private StripeChargeSpark2App() { }

    public static void main(final String[] args) throws IOException {
        // Read Stripe platform's client ID and secret API key
        ClassLoader classLoader = StripeChargeSpark2App.class.getClassLoader();
        File keyFile = new File(classLoader.getResource("stripe/keys.ini")
                                .getFile());
        IniPreferences prefs = new IniPreferences(new Ini(keyFile));
        final String secretApiKey
            = prefs.node("stripe").get("secret_api_key", null);
        final String publishableApiKey
            = prefs.node("stripe").get("publishable_api_key", null);

        // Set the secret API key
        Stripe.apiKey = secretApiKey;

        // Path to static files
        staticFileLocation("/public");

        get("/", (request, response) -> {
            // Display the index.ftl template, with the parameters for the
            // Checkout form
            Map<String, Object> viewObjects
                = new HashMap<String, Object>();
            viewObjects.put("publishable_key", publishableApiKey);
            viewObjects.put("amount", CHARGE_AMOUNT);
            viewObjects.put("currency", CHARGE_CURRENCY);
            return new ModelAndView(viewObjects, "index.ftl");
        }, new FreeMarkerEngine());

        post("/charge", (request, response) -> {
            Map<String, Object> viewObjects
                = new HashMap<String, Object>();

            // Get the parameters returned by Checkout
            String token = request.queryParams("stripeToken");
            String email = request.queryParams("stripeEmail");

            try {
                // Create the charge
                Map<String, Object> chargeParams
                    = new HashMap<String, Object>();
                chargeParams.put("amount", CHARGE_AMOUNT);
                chargeParams.put("currency", CHARGE_CURRENCY);
                chargeParams.put("source", token);
                chargeParams.put("description", "Charge for " + email);

                Charge charge = Charge.create(chargeParams);

                // Display the success page, with the charge ID
                viewObjects.put("charge_id", charge.getId());
                return new ModelAndView(viewObjects, "success.ftl");
            } catch (StripeException e) {
                // An error happened during the charge creation: display
                // the error message
                viewObjects.put("error", e.getMessage());
                return new ModelAndView(viewObjects, "error.ftl");
            }
        }, new FreeMarkerEngine());
    }
}
