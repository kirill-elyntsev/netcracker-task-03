package com.elyntsev;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.opera.OperaDriver;

import com.elyntsev.configuration.Config;
import com.relevantcodes.extentreports.ExtentReports;
import com.relevantcodes.extentreports.ExtentTest;
import com.relevantcodes.extentreports.LogStatus;

public class YandexNewsServiceTest {

	public static WebDriver driver;
	public List<WebElement> elements;

	public static ExtentReports extent;
	public static ExtentTest test;
	
	@BeforeClass
	public static void setup() {
		
		extent = new ExtentReports("src/test/resources/report/report.html", true);
		test = extent.startTest("Yandex news portal test");
		test.log(LogStatus.INFO, "Test is initiated");

		System.setProperty(Config.getProperty("webdriver"), Config.getProperty("path"));
		driver = new OperaDriver();
		driver.manage().window().maximize();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.get(Config.getProperty("startpage"));
		driver.navigate().to(Config.getProperty("rubricpath"));
	}

	@Before
	public void getElements() {
		elements = driver
				.findElement(By.xpath(Config.getProperty("newspath")))
				.findElements(By.xpath(
						".//div[@class='mg-grid__col mg-grid__col_xs_4' or @class ='mg-grid__col mg-grid__col_xs_8']"));
	}

	@Test
	public void articleTitleTest() {

		elements.stream().forEach(x -> {
			try {

				String title = x.findElement(By.tagName("h2")).getText();
				assertNotNull(title);
				System.out.println(title);
				test.log(LogStatus.PASS, "Title is checked: " + title);
				
			} catch (AssertionError e) {
				test.log(LogStatus.FAIL, "Title is missing");
			}

		});
	}

	@Test
	public void articlePictureTest() {

		elements.stream().forEach(x -> {

			try {

				String pictureSrc = x.findElement(By.xpath(".//img[@class='neo-image neo-image_loaded']"))
						.getAttribute("src");
				assertNotNull(pictureSrc);
				System.out.println(pictureSrc);
				test.log(LogStatus.PASS, "Picture is checked: " + pictureSrc);

			} catch (AssertionError e) {
				test.log(LogStatus.FAIL, "Picture is missing");
			}

		});
	}

	@Test
	public void articleDescriptionTest() {

		elements.stream().forEach(x -> {

			try {

				String description = x.findElement(By.xpath(".//div[@class='mg-card__annotation']"))
						.getAttribute("innerHTML");
				assertNotNull(description);
				System.out.println(description);
				test.log(LogStatus.PASS, "Description is checked: " + description);

			} catch (AssertionError e) {
				test.log(LogStatus.FAIL, "Description is missing");
			}

		});

	}

	@Test
	public void articleHaveLinkTest() {

		elements.stream().forEach(x -> {

			try {
				String link = x.findElement(By.tagName("a")).getAttribute("href");
				assertNotNull(link);
				System.out.println(link);
				test.log(LogStatus.PASS, "Link is checked: " + link);

			} catch (AssertionError e) {
				test.log(LogStatus.FAIL, "Link is missing");
			}

		});

	}

	@Test
	public void articlePageTest() {

		List<String> titles = new ArrayList<>();
		List<String> links = new ArrayList<>();
		List<Optional<String>> sourceLinks = new ArrayList<>();

		elements.stream().forEach(x -> {
			try {

				String title = x.findElement(By.tagName("h2")).getText();
				assertNotNull(title);
				titles.add(title);

				String link = x.findElement(By.tagName("a")).getAttribute("href");
				assertNotNull(link);
				links.add(link);

			} catch (AssertionError e) {
				test.log(LogStatus.FAIL, "Title is missing");
			}

		});

		for (int i = 0; i < links.size(); i++) {

			try {
				String url = links.get(i);
				driver.navigate().to(url);

				Optional<String> sourceLink = Optional.ofNullable(driver
						.findElement(By.xpath("//div[@class='mg-snippet mg-snippet_without-text news-story__snippet']"))
						.findElement(By.tagName("a")).getAttribute("href"));

				sourceLinks.add(sourceLink);

				String titleArticle = driver.findElement(By.tagName("h1")).getText();
				assertTrue(titles.get(i).equals(titleArticle));
				test.log(LogStatus.PASS, "Titles is equals");
			} catch (AssertionError e) {
				test.log(LogStatus.FAIL, "Titles is not equals");
			}
		}

		driver.navigate().to(Config.getProperty("rubricpath"));

		for (int i = 0; i < sourceLinks.size(); i++) {
			try {
				assertTrue(sourceLinks.get(i).isPresent());
				test.log(LogStatus.PASS, "Source link exists: " + sourceLinks.get(i).get());

				try {

					HttpURLConnection conn = null;
					int code = 0;

					try {
						conn = (HttpURLConnection) (new URL(sourceLinks.get(i).get()).openConnection());
						conn.setRequestMethod("HEAD");
						conn.connect();
						code = conn.getResponseCode();
						assertTrue(code < 400);
						test.log(LogStatus.PASS,
								"Source link is valid (Code " + code + "): " + sourceLinks.get(i).get());

					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}

				} catch (AssertionError e) {
					test.log(LogStatus.FAIL, "Source link is broken");
				}

			} catch (AssertionError e) {
				test.log(LogStatus.FAIL, "No source link");
			}
		}
	}

	@AfterClass
	public static void close() {
		test.log(LogStatus.INFO, "Test completed");
		extent.endTest(test);
		extent.flush();
		driver.close();

	}

}
