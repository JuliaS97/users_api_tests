package tests;

import base.BaseTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import utils.TokenManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.*;

/**
 * /users endpoint qrupu üçün test sinfi.
 *
 * Test axını (dependsOnMethods vasitəsilə):
 *   01 -> 02 (list-dən götürülən id ilə)
 *   03 -> 04 -> 05 (yaradılan istifadəçi id-si ilə)
 *   06 tamamilə müstəqildir (öz istifadəçisini yaradır/silir).
 */
public class UserTest extends BaseTest {

    // Task 01 -> 02 üçün paylaşılan state
    private List<Map<String, Object>> usersFromList;
    private Object existingUserId;

    // Task 03 -> 04 -> 05 üçün paylaşılan state
    private Object createdUserId;
    private Map<String, Object> createPayload;

    private static final Object NON_EXISTENT_ID = 999999999;

    // =========================================================
    // Task 01: GET /users - siyahının strukturu
    // =========================================================
    @Test(priority = 1)
    public void getAllUsers_shouldReturnValidListStructure() {
        Response response = given()
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().response();

        List<Map<String, Object>> users = response.jsonPath().getList("$");

        assertNotNull(users, "Cavab body-si array formatında olmalıdır");
        assertFalse(users.isEmpty(), "İstifadəçi siyahısı boş olmamalıdır");

        for (Map<String, Object> user : users) {
            assertTrue(user.containsKey("id"), "Hər elementdə 'id' sahəsi olmalıdır");
            assertTrue(user.containsKey("name"), "Hər elementdə 'name' sahəsi olmalıdır");
            assertTrue(user.containsKey("email"), "Hər elementdə 'email' sahəsi olmalıdır");
        }

        this.usersFromList = users;
        this.existingUserId = users.get(0).get("id");
    }

    // =========================================================
    // Task 02: GET /users/:id - mövcud istifadəçi
    // =========================================================
    @Test(priority = 2, dependsOnMethods = "getAllUsers_shouldReturnValidListStructure")
    public void getUserById_existingUser_shouldMatchListData() {
        Map<String, Object> expected = usersFromList.get(0);

        Response response = given()
                .when()
                .get("/users/{id}", existingUserId)
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().response();

        Map<String, Object> actual = response.jsonPath().getMap("$");

        assertEquals(actual.get("id"), expected.get("id"), "id siyahıdakı ilə eyni olmalıdır");
        assertEquals(actual.get("name"), expected.get("name"), "name siyahıdakı ilə eyni olmalıdır");
        assertEquals(actual.get("email"), expected.get("email"), "email siyahıdakı ilə eyni olmalıdır");
    }

    // =========================================================
    // Task 02: GET /users/:id - mövcud olmayan istifadəçi
    // =========================================================
    @Test(priority = 2)
    public void getUserById_nonExistentUser_shouldReturnNotFound() {
        Response response = given()
                .when()
                .get("/users/{id}", NON_EXISTENT_ID)
                .then()
                .extract().response();

        // NOT: dəqiq status kodu (404 gözlənilən, amma API sənədinə görə dəyişə bilər) yoxlanılır.
        assertEquals(response.statusCode(), 404,
                "Mövcud olmayan id üçün 404 gözlənilir, alınan: " + response.statusCode());
        assertFalse(response.getBody().asString().isBlank(),
                "Xəta cavabında boş olmayan body gözlənilir");
    }

    // =========================================================
    // Task 03: POST /users - yeni istifadəçi yaratmaq
    // =========================================================
    @Test(priority = 3)
    public void createUser_shouldReturnCreatedUserMatchingRequest() {
        createPayload = new HashMap<>();
        createPayload.put("name", "Test User");
        createPayload.put("email", "test.user." + System.currentTimeMillis() + "@example.com");

        Response response = given()
                .header("Authorization", "Bearer " + TokenManager.getToken())
                .contentType("application/json")
                .body(createPayload)
                .when()
                .post("/users")
                .then()
                .statusCode(201)
                .contentType("application/json")
                .extract().response();

        Map<String, Object> created = response.jsonPath().getMap("$");

        assertNotNull(created.get("id"), "Yaradılan istifadəçinin id-si olmalıdır");
        assertEquals(created.get("name"), createPayload.get("name"),
                "Qaytarılan name göndərilənlə eyni olmalıdır");
        assertEquals(created.get("email"), createPayload.get("email"),
                "Qaytarılan email göndərilənlə eyni olmalıdır");

        createdUserId = created.get("id");
    }

    // =========================================================
    // Task 03: POST /users - tələb olunan sahə çatışmır
    // =========================================================
    @Test(priority = 3)
    public void createUser_missingRequiredField_shouldReturnBadRequest() {
        Map<String, Object> invalidPayload = new HashMap<>();
        invalidPayload.put("name", "Incomplete User");
        // "email" qəsdən buraxılıb

        Response response = given()
                .header("Authorization", "Bearer " + TokenManager.getToken())
                .contentType("application/json")
                .body(invalidPayload)
                .when()
                .post("/users")
                .then()
                .extract().response();

        int status = response.statusCode();
        assertTrue(status >= 400 && status < 500,
                "Tələb olunan sahə çatışmadıqda 4xx status gözlənilir, alınan: " + status);
    }

    // =========================================================
    // Task 04: PUT /users/:id - mövcud istifadəçini yeniləmək
    // =========================================================
    @Test(priority = 4, dependsOnMethods = "createUser_shouldReturnCreatedUserMatchingRequest")
    public void updateUser_shouldPersistChangeOnServer() {
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("name", "Updated Test User");
        updatePayload.put("email", createPayload.get("email"));

        given()
                .header("Authorization", "Bearer " + TokenManager.getToken())
                .contentType("application/json")
                .body(updatePayload)
                .when()
                .put("/users/{id}", createdUserId)
                .then()
                .statusCode(200);

        // Dəyişikliyin server tərəfdə tətbiq olunduğunu təsdiqləmək üçün yenidən GET
        Response getResponse = given()
                .when()
                .get("/users/{id}", createdUserId)
                .then()
                .statusCode(200)
                .extract().response();

        assertEquals(getResponse.jsonPath().getString("name"), "Updated Test User",
                "Yenilənmiş name serverdə saxlanmalıdır");
    }

    // =========================================================
    // Task 04: PUT /users/:id - mövcud olmayan id
    // =========================================================
    @Test(priority = 4)
    public void updateUser_nonExistentId_shouldReturnNotFound() {
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("name", "Ghost User");
        updatePayload.put("email", "ghost@example.com");

        Response response = given()
                .header("Authorization", "Bearer " + TokenManager.getToken())
                .contentType("application/json")
                .body(updatePayload)
                .when()
                .put("/users/{id}", NON_EXISTENT_ID)
                .then()
                .extract().response();

        assertEquals(response.statusCode(), 404,
                "Mövcud olmayan id yenilənərkən 404 gözlənilir, alınan: " + response.statusCode());
    }

    // =========================================================
    // Task 05: DELETE /users/:id - istifadəçini silmək
    // =========================================================
    @Test(priority = 5, dependsOnMethods = "updateUser_shouldPersistChangeOnServer")
    public void deleteUser_shouldSucceed() {
        Response response = given()
                .header("Authorization", "Bearer " + TokenManager.getToken())
                .when()
                .delete("/users/{id}", createdUserId)
                .then()
                .extract().response();

        int status = response.statusCode();
        assertTrue(status == 200 || status == 204,
                "Silmə əməliyyatı üçün 200 və ya 204 gözlənilir, alınan: " + status);
    }

    // =========================================================
    // Task 05: DELETE sonrası GET - artıq mövcud olmamalıdır
    // =========================================================
    @Test(priority = 6, dependsOnMethods = "deleteUser_shouldSucceed")
    public void getUserById_afterDelete_shouldReturnNotFound() {
        Response response = given()
                .when()
                .get("/users/{id}", createdUserId)
                .then()
                .extract().response();

        assertEquals(response.statusCode(), 404,
                "Silinmiş istifadəçi üçün 404 gözlənilir, alınan: " + response.statusCode());
    }

    // =========================================================
    // Task 06: Bonus - tam end-to-end axın (müstəqil test)
    // =========================================================
    @Test(priority = 7)
    public void fullUserLifecycle_createReadUpdateReadDeleteVerify() {
        String token = TokenManager.getToken();

        // 1) CREATE
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "E2E User");
        payload.put("email", "e2e.user." + System.currentTimeMillis() + "@example.com");

        Response createResp = given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(payload)
                .when()
                .post("/users")
                .then()
                .statusCode(201)
                .extract().response();

        Object userId = createResp.jsonPath().get("id");
        assertNotNull(userId, "Yaradılan istifadəçinin id-si olmalıdır");

        // 2) READ (yoxlama)
        given()
                .when()
                .get("/users/{id}", userId)
                .then()
                .statusCode(200)
                .body("name", org.hamcrest.Matchers.equalTo(payload.get("name")))
                .body("email", org.hamcrest.Matchers.equalTo(payload.get("email")));

        // 3) UPDATE
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("name", "E2E User Updated");
        updatePayload.put("email", payload.get("email"));

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(updatePayload)
                .when()
                .put("/users/{id}", userId)
                .then()
                .statusCode(200);

        // 4) READ (yenilənmənin təsdiqi)
        given()
                .when()
                .get("/users/{id}", userId)
                .then()
                .statusCode(200)
                .body("name", org.hamcrest.Matchers.equalTo("E2E User Updated"));

        // 5) DELETE
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/users/{id}", userId)
                .then()
                .statusCode(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is(200),
                        org.hamcrest.Matchers.is(204)));

        // 6) READ (silinmənin təsdiqi)
        given()
                .when()
                .get("/users/{id}", userId)
                .then()
                .statusCode(404);
    }
}
