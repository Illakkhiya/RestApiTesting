package zendesk;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.jayway.jsonpath.JsonPath;

import java.util.List;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import static io.restassured.RestAssured.*;
import static org.testng.Assert.*;

public class TicketTest {

	final static String USERNAME = "illakkhiya@gmail.com/token";
	final static String APITOKEN = "IOSfKeZ20vGGpvXN6BX1mCnrnpcOplnY2tAuwtzF";

	@BeforeTest
	public void setUp() {
		//Initializing baseURI for all tests
		RestAssured.baseURI = "https://illakkhiyatest.zendesk.com";
	}
	
	private String getCreateTicketBody()
	{
		//sample body for creating ticket
		return "{\r\n" + 
				"  \"ticket\": {\r\n" + 
				"    \"subject\":  \"coding test\",\r\n" + 
				"    \"comment\":  { \"body\": \"first ticket\" },\r\n" + 
				"    \"priority\": \"urgent\"\r\n" + 
				"  }\r\n" + 
				"}";
	}

	/*
	 * Creates a ticket and returns HTTP Response 
	 */
	private Response createTicket() {
		//creating a ticket and validating the response status code
		return given().auth().basic(USERNAME, APITOKEN).header("Content-Type", "application/json")
				.body(getCreateTicketBody()).when().post("/api/v2/tickets.json").then().statusCode(201).extract()
				.response();
	}

	/**
	 * Returns a ticket id from Response object 
	 */
	private Integer getTicketId(Response res) {
		return JsonPath.read(res.asString(), "ticket.id");
	}
	
	/*
	 * deletes a given ticket
	 */
	
	private void deleteTicket(int id) {
		given().auth().basic(USERNAME, APITOKEN).when().delete("/api/v2/tickets/" + id + ".json").then()
				.statusCode(204);
	}

	@Test(priority = 0)
	public void createTicketTest() {

		Response res = createTicket();

		// validating that response contains a valid ID
		assertNotNull(getTicketId(res), "Id must not be null");
		assertTrue(getTicketId(res) > 0, "Id should be greater than 0");
		deleteTicket(getTicketId(res));
	}
	/*
	 * returns a sample Json String for adding  a comment to a ticket
	 */
	private String getCommentBody()
	{
		return "{\r\n" + 
				"  \"ticket\": {\r\n" + 
				"    \"comment\":  { \"body\": \"Adding a comment to a ticket\" }\r\n" + 
				"  }\r\n" + 
				"}";
	}
	
	/*
	 * returns a number of comments in a ticket
	 */
	private int getNumberOfComments(int id) {
		Response res = given().auth().basic(USERNAME, APITOKEN).when().get("/api/v2/tickets/" + id + "/comments.json")
				.then().statusCode(200).extract().response();
		List<Integer> list = JsonPath.parse(res.asString()).read("comments[*].id");
		int commentsCount = list.size();
		return commentsCount;
	}

	@Test(priority = 1)
	public void addCommentTest() {
		Response res = createTicket();
		Integer id = getTicketId(res);
		int commentsCountBefore = getNumberOfComments(id);
		assertEquals(commentsCountBefore, 1);

		// adding a comment to a ticket
		res = given().auth().basic(USERNAME, APITOKEN).header("Content-Type", "application/json").body(getCommentBody())
				.when().put("/api/v2/tickets/" + id + ".json").then().statusCode(200).extract().response();
		int commentCountAfter = getNumberOfComments(id);
		// verifying the comments count should be 2
		assertEquals(commentCountAfter, 2);
		deleteTicket(id);
	}

	@Test(priority = 2)
	public void listTicketTest() {
		// create 10 sample tickets to list
		for (int i = 0; i < 10; i++) {
			Response res = createTicket();
		}

		Response res = given().auth().basic(USERNAME, APITOKEN).when().get("/api/v2/tickets.json").then()
				.statusCode(200).extract().response();
		List<Integer> ids = JsonPath.parse(res.asString()).read("tickets[*].id");
		assertTrue(ids.size() == 10, "Number Of tickets should be greater than 0");
		// delete the created 10 tickets
		for (int id : ids) {
			deleteTicket(id);
		}
	}

	@Test(priority = 3)
	public void deleteTicketTest() {
		Response res = createTicket();
		Integer id = getTicketId(res);
		deleteTicket(id);
		// validating that the ticket is no more available
		given().auth().basic(USERNAME, APITOKEN).when().get("/api/v2/tickets/" + id + ".json").then().statusCode(404);

	}
	
	@AfterMethod
	public void cleanUp() {
		Response res = given().auth().basic(USERNAME, APITOKEN).when().get("/api/v2/tickets.json").then()
				.statusCode(200).extract().response();
		List<Integer> ids = JsonPath.parse(res.asString()).read("tickets[*].id");
		// delete all created tickets after every test
		for (int id : ids) {
			deleteTicket(id);
		}
	}

}
