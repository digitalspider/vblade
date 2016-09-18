package au.com.digitalspider.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import au.com.digitalspider.Util;
import au.com.digitalspider.format.OutputFormat;
import au.com.digitalspider.handler.AbstractOutputHandler;

public class Esp8266Servlet extends HttpServlet {

	private static final long serialVersionUID = 7389427524804745024L;
	private Logger log = Logger.getLogger(getClass());
	
	public static final String USER_DIR = System.getProperty("user.home");
	public static final String ESP_DATA_DIR_PATH = USER_DIR + File.separator + ".esp8266";
	public static final File ESP_DATA_DIR = new File(ESP_DATA_DIR_PATH);
	public static final Map<String, File> DATA_FILES = new HashMap<String, File>();
	public static final String FILESUFFIX_DATA = ".txt";
	public static final int BATCH_SIZE = 1000;
	public static final int DEFAULT_LINES = 1000;
	private static VelocityEngine ve;
	private Map<String,List<AbstractOutputHandler>> handlersMap = new HashMap<String,List<AbstractOutputHandler>>();

	@Override
	public void init() throws ServletException {
		super.init();
		try {
			// reload directories
			reloadDirs();
			// Print fileContextPath
			String fileContextPath = getServletConfig().getServletContext().getRealPath("/");
			System.out.println("fileContextPath= "+fileContextPath);
			// Initialise velocity
			ve = Util.setupVelocity(getServletConfig());
			// Initialise handlers
			handlersMap = Util.setupHandlers(getServletConfig());
		} catch (Exception e) {
			log.error(e,e);
			throw new ServletException(e);
		}
	}
	
	private void reloadDirs() {
		// Initialise file system ~/.esp8266
		ESP_DATA_DIR.mkdirs();
		File[] files = ESP_DATA_DIR.listFiles();
		for (File file : files) {
			String filename = file.getName();
			if (filename.endsWith(FILESUFFIX_DATA)) {
				DATA_FILES.put(filename, file);
			}
		}
	}

	/**
	 * URL should be esp8266/<chipId>/<action>/<params>
	 * actions = r/w/x/d/u = note r is default so can be assumed - should be PUT/POST/GET/DELETE
	 * params = can include type of chip, value, date to read, etc
	 * 
	 * e.g /esp8266 = list all the files for in ~/.esp8266
	 * e.g /esp8266/123456 = list all the files for the chip 123456 
	 * e.g /esp8266/123456/20160120_123456.txt = read chip 123456 for date 2016-01-20
	 * e.g /esp8266/123456/20160120_123456.json = read chip 123456 for date 2016-01-20 in json
	 * e.g /esp8266/123456/20160120_123456.xml = read chip 123456 for date 2016-01-20 in xml
	 * e.g /esp8266/123456/20160120_123456.chart = read chip 123456 for date 2016-01-20 as a chart
	 * e.g /esp8266/123456/20160120_123456.download = download the file for chip 123456 for date 2016-01-20
	 * e.g /esp8266/123456/w/D/8261/2.1023 = write chip 123456 of type Distance, heap 8261, value 2.1023
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			log.info("pathinfo="+request.getPathInfo());
			String content = "";
			String ipAddress = request.getRemoteAddr();
			String baseUrl = Util.getBaseUrl(request);
			String[] pathParts = Util.getPathParts(request);
			String chipId = null;
			if (pathParts.length>0) {
				chipId = pathParts[0];
			}

			// If no chip is provided, or only the chipId is provided, list the files
			if (chipId == null || chipId.trim().length() == 0 || pathParts.length<2) {
				List<String> files = new ArrayList<String>();
				if (pathParts.length==0) {
					// no chip id list all files
					reloadDirs();
					files.addAll(DATA_FILES.keySet());
				} else {
					// list only files for a given chipId
					for (String fileName : DATA_FILES.keySet()) {
						if (fileName.contains(chipId)) {
							files.add(fileName);
						}
					}
				}
				Collections.sort(files);
				Collections.reverse(files);
				
				StringWriter sw = new StringWriter();
				Template template = ve.getTemplate("listFiles.vm");
				VelocityContext context = Util.getDefaultVelocityContext();
				context.put( "files", files );
				context.put( "baseUrl", baseUrl );
				context.put( "fileSuffix", FILESUFFIX_DATA );
				template.merge(context, sw);
				content = sw.toString();
				
				writeResponse(request, response, "txt", content);
				return;
			}
			
			// If this is a write request /esp8266/chipid/w/type/heap/distance handle this
			if (pathParts.length>4 && pathParts[1].trim().length()>0 && pathParts[1].equals("w") && 
					pathParts[2].trim().length()>0 && pathParts[3].trim().length()>0 && pathParts[4].trim().length()>0) {
				String type = pathParts[2];
				String heap = pathParts[3];
				String distance = pathParts[4];
				//System.out.println("type="+type);
				//System.out.println("heap="+heap);
				//System.out.println("dist="+distance);

				List<AbstractOutputHandler> handlers = handlersMap.get(type);
				for(AbstractOutputHandler handler : handlers) {
					handler.init(request,response,ve,ipAddress,chipId,type,heap,distance);
					handler.call();
				}

				if (heap.length() > 0 && distance.length() > 0) {
					content = "val=" + distance + "<br/>";
					content += "session=" + request.getSession(true).getId();
				}
			
				writeResponse(request, response, "txt", content);
				return ;
			}
			
			// If a file is requested
			int linesRequested = DEFAULT_LINES;
			if (pathParts.length>1 && pathParts[1].trim().length()>0) {
				String filename = pathParts[1];
				if (pathParts.length>2 && pathParts[2].trim().length()>0) {
					try {
						linesRequested = Integer.parseInt(pathParts[1]);
					} catch (Exception e) {
						System.err.println("pathParts[1] = "+pathParts[1]+". This was supposed to be a number");
					}
					filename = pathParts[2];
				}
				String textFilename = Util.getTextFileName(filename);

				// Get the file requested
				if (DATA_FILES.containsKey(textFilename)) {
					File file = DATA_FILES.get(textFilename);
					String type = Util.getFileExtension(filename);
					StringBuffer htmlContent = new StringBuffer();
					int lineCount = Util.getLinesCountForFile(file);
					linesRequested = linesRequested > lineCount ? lineCount : linesRequested;
					int batch = linesRequested / BATCH_SIZE;
					while (batch>0) {
						log.debug("batch="+batch+" linesRequested="+linesRequested);
						List<String> fileContent = Util.getLastNLogLines(file, BATCH_SIZE, linesRequested);
						htmlContent.insert(0,OutputFormat.formatContent(ve, fileContent, type));
						linesRequested -= BATCH_SIZE;
						batch = linesRequested / BATCH_SIZE;
					}
					log.debug("batch="+batch+" linesRequested="+linesRequested);
					List<String> fileContent = Util.getLastNLogLines(file, BATCH_SIZE, linesRequested);
					htmlContent.insert(0,OutputFormat.formatContent(ve, fileContent, type));
					htmlContent.insert(0,OutputFormat.formatHeader(ve, type));
					htmlContent.append(OutputFormat.formatFooter(ve, type));
					content = htmlContent.toString();
					writeResponse(request, response, type, content);
					return ;
				}
			}
			
			// Write an empty response
			writeResponse(request, response, "txt", content);
			
		} catch (Exception e) {
			log.error(e,e);
			Util.writeError(request, response, e);
		}
	}

	public static void writeResponse(HttpServletRequest request, HttpServletResponse response, String type, String content)
			throws IOException {
		if (type.equals("json")) {
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			response.setStatus(HttpServletResponse.SC_OK);
			out.println(content);
		}
		else if (type.equals("xml")) {
			response.setContentType("text/xml");
			PrintWriter out = response.getWriter();
			response.setStatus(HttpServletResponse.SC_OK);
			out.println(OutputFormat.XML_HEADER);
			out.println(content);			
		}
		else {
			response.setContentType("text/html");
			PrintWriter out = response.getWriter();
			response.setStatus(HttpServletResponse.SC_OK);
			out.println("<!DOCTYPE HTML>");
			out.println("<html>");
			if (!content.contains("<head")) {
				out.println("<h2><a href='/'>Home</a> | <a href='/gpio'>WebGpio</a> | <a href='/gpio'>WebI2C</a> | <a href='/esp8266'>Esp8266</a></h2>");
				out.println("<div>"+content+"</div>");
			} else {
				out.println(content);
			}
			out.println("</html>");
		}
	}

}
