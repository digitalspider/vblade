package au.com.digitalspider.format;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
import org.junit.Ignore;
import org.junit.Test;

import junit.framework.TestCase;

public class OutputFormatTest extends TestCase {

	VelocityEngine ve = new VelocityEngine();
	Logger log = Logger.getLogger(getClass());
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		String velocityConfig = System.getProperty("user.dir")+"/src/main/webapp/WEB-INF/velocity.properties";
		log.debug(velocityConfig);
		Properties props = new Properties();
		props.load(new FileInputStream(new File(velocityConfig)));
		for (Entry<Object,Object> prop : props.entrySet()) {
			if (prop.getValue().toString().contains("${CONTEXTDIR}")) {
				prop.setValue(prop.getValue().toString().replace("${CONTEXTDIR}", System.getProperty("user.dir")+"/src/main/webapp"));
			}
		}
		ve = new VelocityEngine();
		ve.init(props);
	}
	
	@Test
	public void testFormatXML() {
		List<String> data1 = new ArrayList<String>();
		data1.add("20160207|200754|10.1.1.6|8752|-1");
		data1.add("20160207|190505|10.1.1.6|8584|0.11729885057471\n");
		List<String> data2 = new ArrayList<String>();
		data2.add("20160207|190435|10.1.1.6|8632|0.25689655172414\n");
		data2.add("20160207|183409|10.1.1.6|8776|0.42155172413793");
		
		String expected = "<dataSet>\n"+
				"<data date=\"20160207\" time=\"200754\" ip=\"10.1.1.6\" heap=\"8752\">-1</data>\n"+
				"<data date=\"20160207\" time=\"190505\" ip=\"10.1.1.6\" heap=\"8584\">0.11729885057471</data>\n"+
				"<data date=\"20160207\" time=\"190435\" ip=\"10.1.1.6\" heap=\"8632\">0.25689655172414</data>\n"+
				"<data date=\"20160207\" time=\"183409\" ip=\"10.1.1.6\" heap=\"8776\">0.42155172413793</data>\n"+
				"</dataSet>\n";
		
		String actual = OutputFormat.formatHeader(ve, "xml");
		actual += OutputFormat.formatContent(ve, data1, "xml");
		actual += OutputFormat.formatContent(ve, data2, "xml");
		actual += OutputFormat.formatFooter(ve, "xml");
		assertEquals(expected,actual);
	}
	
	@Test
	public void testFormatJSON() {
		List<String> data1 = new ArrayList<String>();
		data1.add("20160207|200754|10.1.1.6|8752|-1");
		data1.add("20160207|190505|10.1.1.6|8584|0.11729885057471\n");
		List<String> data2 = new ArrayList<String>();
		data2.add("20160207|190435|10.1.1.6|8632|0.25689655172414\n");
		data2.add("20160207|183409|10.1.1.6|8776|0.42155172413793");
		
		String expected = "{ dataSet : [\n"+
				",{ date: \"20160207\", time: \"200754\", ip: \"10.1.1.6\", heap: \"8752\", value: \"-1\"}\n"+
				",{ date: \"20160207\", time: \"190505\", ip: \"10.1.1.6\", heap: \"8584\", value: \"0.11729885057471\"}\n"+
				",{ date: \"20160207\", time: \"190435\", ip: \"10.1.1.6\", heap: \"8632\", value: \"0.25689655172414\"}\n"+
				",{ date: \"20160207\", time: \"183409\", ip: \"10.1.1.6\", heap: \"8776\", value: \"0.42155172413793\"}\n"+
				"]}\n";
		
		String actual = OutputFormat.formatHeader(ve, "json");
		actual += OutputFormat.formatContent(ve, data1, "json");
		actual += OutputFormat.formatContent(ve, data2, "json");
		actual += OutputFormat.formatFooter(ve, "json");
		assertEquals(expected,actual);
	}
	
	@Test
	@Ignore
	public void testFormatChart() {
		List<String> data1 = new ArrayList<String>();
		data1.add("20160207|200754|10.1.1.6|8752|-1");
		data1.add("20160207|190505|10.1.1.6|8584|0.11729885057471\n");
		List<String> data2 = new ArrayList<String>();
		data2.add("20160207|190435|10.1.1.6|8632|0.25689655172414\n");
		data2.add("20160207|183409|10.1.1.6|8776|0.42155172413793");
		
		String expected = "dataPoints.push({x:183409, y:0.42155172413793},{x:190435, y:0.25689655172414});\n"+
							"dataPoints.push({x:190505, y:0.11729885057471},{x:200754, y:-1});\n";
		
		String actual = OutputFormat.formatHeader(ve, "chart");
		actual += OutputFormat.formatContent(ve, data2, "chart");
		actual += OutputFormat.formatContent(ve, data1, "chart");
		actual += OutputFormat.formatFooter(ve, "chart");
		//assertEquals(expected,actual);
		assertTrue(actual.contains(expected));
	}
}

