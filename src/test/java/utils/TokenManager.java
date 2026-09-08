package utils;

import io.restassured.response.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static io.restassured.RestAssured.given;

/**
 * POST /auth/login ilə token almaq üçün util class.
 * Credential-lar heç vaxt kodda hardcode edilmir. İki mənbədən oxunur (sıra ilə yoxlanılır):
 *   1) credentials.local.properties faylı (layihə kökündə, .gitignore-dadır, git-ə düşmür)
 *   2) API_USER_EMAIL / API_USER_PASSWORD environment variable-ları (CI üçün, məs. GitHub Actions Secrets)
 */
public class TokenManager {

    private static final String LOCAL_CREDENTIALS_FILE = "credentials.local.properties";

    private static String cachedToken;

    public static String getToken() {
        if (cachedToken == null) {
            cachedToken = login();
        }
        return cachedToken;
    }

    private static String login() {
        String[] credentials = readCredentials();
        String email = credentials[0];
        String password = credentials[1];

        if (email == null || password == null) {
            throw new IllegalStateException(
                "Credential tapılmadı. Ya layihə kökündə '" + LOCAL_CREDENTIALS_FILE +
                "' faylı yaradın (email=..., password=... sətirləri ilə), " +
                "ya da API_USER_EMAIL / API_USER_PASSWORD environment variable-larını təyin edin."
            );
        }

        Response response = given()
                .contentType("application/json")
                .body("{ \"email\": \"" + email + "\", \"password\": \"" + password + "\" }")
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        return response.jsonPath().getString("token");
    }

    /**
     * Əvvəlcə credentials.local.properties (layihə kökündə) faylını yoxlayır,
     * tapmasa environment variable-lara keçir.
     */
    private static String[] readCredentials() {
        Path localFile = Path.of(LOCAL_CREDENTIALS_FILE);

        if (Files.exists(localFile)) {
            try (InputStream input = Files.newInputStream(localFile)) {
                Properties props = new Properties();
                props.load(input);
                String email = props.getProperty("email");
                String password = props.getProperty("password");
                if (email != null && password != null) {
                    return new String[]{email, password};
                }
            } catch (IOException e) {
                throw new RuntimeException(LOCAL_CREDENTIALS_FILE + " oxunarkən xəta baş verdi", e);
            }
        }

        String email = System.getenv("API_USER_EMAIL");
        String password = System.getenv("API_USER_PASSWORD");
        return new String[]{email, password};
    }
}
