package au.com.digitalspider.format;

import java.util.Collections;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import au.com.digitalspider.Util;

public class OutputFormat {

	public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	
	public static String formatHeader(VelocityEngine ve, String fileExtension) {
		String result = "";
		if (fileExtension == null || fileExtension.equals("txt")) {
			result = ""; 
		}
		else if (fileExtension.equals("json") || fileExtension.equals("xml") || fileExtension.equals("chart")) {
			String template = "formatTxtTo"+Util.firstCharUpperCase(fileExtension)+"Header.vm";
			VelocityContext context = Util.getDefaultVelocityContext();
			result = Util.useVelocityTemplate(ve, template, context);
		}
		return result;
	}
	
	public static String formatFooter(VelocityEngine ve, String fileExtension) {
		String result = "";
		if (fileExtension == null || fileExtension.equals("txt")) {
			result = ""; 
		}
		else if (fileExtension.equals("json") || fileExtension.equals("xml") || fileExtension.equals("chart")) {
			String template = "formatTxtTo"+Util.firstCharUpperCase(fileExtension)+"Footer.vm";
			VelocityContext context = Util.getDefaultVelocityContext();
			result = Util.useVelocityTemplate(ve, template, context);
		}
		return result;
	}
	
	public static String formatContent(VelocityEngine ve, List<String> rawContent, String fileExtension) {
		String result = "";
		if (fileExtension == null || fileExtension.equals("txt")) {
			StringBuilder stringBuilder = new StringBuilder();
			for (String line : rawContent) {
				stringBuilder.append(line+"<br/>");
			}
			result = stringBuilder.toString(); 
		}
		else if (fileExtension.equals("json") || fileExtension.equals("xml") || fileExtension.equals("chart")) {
			String template = "formatTxtTo"+Util.firstCharUpperCase(fileExtension)+"Data.vm";
			VelocityContext context = Util.getDefaultVelocityContext();
			if (fileExtension.equals("chart")) {
				Collections.reverse(rawContent);
			}
			context.put("dataList", Util.mapLinesToDataPoints(rawContent));
			result = Util.useVelocityTemplate(ve, template, context);
		}
		return result;
	}
}
