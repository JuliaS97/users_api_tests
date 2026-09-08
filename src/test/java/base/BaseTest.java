package base;

import io.restassured.RestAssured;
import org.testng.annotations.BeforeClass;
import utils.ConfigReader;

/**
 * Bütün test siniflərinin miras aldığı əsas class.
 * RestAssured.baseURI burada bir dəfə quraşdırılır.
 */
public class BaseTest {

    @BeforeClass
    public void setUp() {
        RestAssured.baseURI = ConfigReader.get("baseUrl");
    }
}
