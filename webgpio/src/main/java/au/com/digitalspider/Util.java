package au.com.digitalspider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import au.com.digitalspider.bean.DataPoint;
import au.com.digitalspider.handler.AbstractOutputHandler;

/**
 * Hello world!
 *
 */
public class Util 
{
	private static final Logger log = Logger.getLogger(Util.class);

	public static String[] getPathParts(HttpServletRequest req) {
		String path1 = req.getServletPath(); // /gpio
		String path2 = req.getContextPath(); // null
		String path3 = req.getPathInfo(); // /4/direction
		String path4 = req.getQueryString(); // q=test&p2=4
		String path5 = req.getServerName(); // vittor-desktop
		String path6 = req.getProtocol(); // http
		String uri = req.getRequestURI(); // /gpio/4/direction
		String url = req.getRequestURL().toString(); // http://vittor-desktop:8111/gpio/4/direction
		
		if (path3!=null) {
			return path3.substring(1).split("/");
		}
		return new String[0];
	}
	
	public static void writeError(HttpServletRequest request, HttpServletResponse response, String content) throws IOException {
		PrintWriter out=response.getWriter();  
		out.println("<html><body>");
		out.println("<h2><a href='/'>Home</a> | <a href='/gpio'>WebGpio</a> | <a href='/esp8266'>Esp8266</a></h2>");
		out.println("<p><h1>Error has occurred!</h1></p>");
		out.println("<p><b>"+content+"</b></p>");
		out.println("</body></html>");
	}
	
	public static void writeError(HttpServletRequest request, HttpServletResponse response, Exception e) throws IOException {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String stackTrace = sw.toString();
		writeError(request,response,stackTrace.replace(System.getProperty("line.separator"), "<br/>\n"));
	}
	
	/**
	 * Get a configuration file from the servletConfig (init-param in web.xml) based on the the property provided.
	 * 
	 * @param servletConfig
	 * @param propertyName
	 * @return
	 */
	public static List<File> getConfigFiles(ServletConfig servletConfig, String propertyName) {
		List<File> configFiles = new ArrayList<File>();
		String fileContextPath = servletConfig.getServletContext().getRealPath("/");
		String configFileNameValue = servletConfig.getInitParameter(propertyName);
		log.info("configFileNameValue="+configFileNameValue);
		String[] configFileNames = configFileNameValue.split(",");
		for (String configFileName : configFileNames) {
			File file = new File(fileContextPath,configFileName);
			if (file.exists()) {
				log.info("file="+file.getAbsolutePath());
				configFiles.add(file);
			}
		}
		return configFiles;
	}

	/**
	 * Load an init-param from the <code>servletConfig</code>, assuming this is a reference to a
	 * properties file, and replace all "${CONTEXTDIR}" values with the actual fileContextPath.
	 *  
	 * @param servletConfig
	 * @param propertyName
	 * @return
	 * @throws IOException
	 */
	public static Properties loadInitParamProperties(ServletConfig servletConfig, String propertyName) throws IOException {
		Properties props = new Properties();
		List<File> files = getConfigFiles(servletConfig, propertyName);
		for (File file : files) {
			props.load(new FileReader(file));
		}
		String fileContextPath = servletConfig.getServletContext().getRealPath("/");
		for (Entry<Object,Object> prop : props.entrySet()) {
			if (prop.getValue().toString().contains("${CONTEXTDIR}")) {
				prop.setValue(prop.getValue().toString().replace("${CONTEXTDIR}", fileContextPath));
			}
		}
		return props;
	}

	/**
	 * Load the properties file "velocity.config" using {@link #loadInitParamProperties}, 
	 * and call <code>VelocityEngine.init(props)</code>.
	 *  
	 * @param servletConfig
	 * @return 
	 * @throws IOException
	 */
	public static VelocityEngine setupVelocity(ServletConfig servletConfig) throws IOException {
		Properties props = loadInitParamProperties(servletConfig, "velocity.config");
		VelocityEngine ve = new VelocityEngine();
		ve.init(props);
		return ve;
	}
	
	/**
	 * Load the handlers.xml file as per "handler.config" using {@link #getConfigFile}, 
	 * and then call {@link AbstractOutputHandler#readHandlerConfigFile(File)} to do actual file parsing.
	 * 
	 * @param servletConfig
	 * @return
	 * @throws IOException
	 */
	public static Map<String,List<AbstractOutputHandler>> setupHandlers(ServletConfig servletConfig) throws Exception {
		List<File> files = getConfigFiles(servletConfig, "handler.config");
		Map<String,List<AbstractOutputHandler>> handlerMap = new HashMap<String, List<AbstractOutputHandler>>();
		for (File file: files) {
			handlerMap.putAll(readHandlerConfigFile(file));
		}
		return handlerMap;
	}
	
	public static String getBaseUrl(HttpServletRequest request) {
//		System.out.println("request="+request);
//		System.out.println("request.getPathInfo()="+request.getPathInfo());
//		System.out.println("request.getServletPath()="+request.getServletPath());
//		System.out.println("request.getQueryString()="+request.getQueryString());
//		System.out.println("request.getRequestURL()="+request.getRequestURL());
		String url = request.getRequestURL().toString();
		String baseUrl = url.substring(0, url.indexOf("/",9));
		baseUrl = baseUrl + request.getServletPath();
//		System.out.println("baseUrl="+baseUrl);
		return baseUrl;
	}
	
	public static String getContents(File readFile, int linesToRead) {
		StringBuilder contents = new StringBuilder();

		try {
			BufferedReader input = new BufferedReader(new FileReader(readFile));
			try {
				String line = null;
				int i = 0;
				while ((line = input.readLine()) != null) {
					contents.append(line);
					contents.append("<br/>");
					if (++i >= linesToRead)
						break;
				}
			} finally {
				input.close();
			}
		} catch (IOException ex) {
			log.error(ex,ex);
		}

		return contents.toString();
	}

	public static String executeLinuxCmd(String cmd) throws IOException {
		String[] cmdArray = { "/bin/sh", "-c", cmd };
		log.debug("cmd="+cmdArray[2]);
		Process p = Runtime.getRuntime().exec(cmdArray);
	    String output = IOUtils.toString(p.getInputStream());
	    log.debug("output="+output);
	    return output;
	}
	
	public static InputStream executeLinuxCmdAsStream(String cmd) throws IOException {
		String[] cmdArray = { "/bin/sh", "-c", cmd };
		log.debug("cmd="+cmdArray[2]);
		Process p = Runtime.getRuntime().exec(cmdArray);
	    return p.getInputStream();
	}
	
	public static int getLinesCountForFile(File readFile) throws Exception {
		String cmd = "wc -l " + readFile+" | cut -f 1 -d ' '";
	    String output = executeLinuxCmd(cmd);
	    if (output.trim().length()>0) {
	    	return Integer.parseInt(output.trim());
	    }
		return 0;
	}
	
	public static List<String> getLastNLogLines(File readFile, int batchSize, int linesToRead) {
		List<String> content = new ArrayList<String>();
		InputStream is = null;
		try {
			String cmd = "tail -" + linesToRead + " "+readFile+" | head -"+batchSize;
			is = executeLinuxCmdAsStream(cmd);
			BufferedReader input = new BufferedReader(new java.io.InputStreamReader(is));
			String line = null;
			while ((line = input.readLine()) != null) {
				content.add(0,line);
			}
		} catch (java.io.IOException e) {
			log.error(e,e);
		} finally {
			if (is!=null) { 
				try { 
					is.close(); 
				} catch (IOException e) {
					log.error(e,e); 
				}
			}
		}
		return content;
	}
	
	/**
	 * Static method for reading the handlers.xml config file.
	 * 
	 * This class uses reflection to create the <handler> elements of type {@link AbstractOutputHandler}.
	 * 
	 * Sample expected content
	 * {@code
	 * <handlers>
	 * 	<sensor type="dist">
	 * 		<handler name="rawoutput" class="au.com.digitalspider.handler.RawOutputHandler">
	 * 			<param name="velocity.template">outputRawTemplate.vm</param>
	 * 			<param name="file.suffix">_raw.txt</param>
	 * 		</handler>
	 * 	</sensor>
	 * </handlers>
	 * }
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static Map<String,List<AbstractOutputHandler>> readHandlerConfigFile(File file) throws Exception {
		Map<String,List<AbstractOutputHandler>> handlersMap = new HashMap<String, List<AbstractOutputHandler>>();
		
		SAXReader reader = new SAXReader();
		Document doc = reader.read(file);
		Element root = doc.getRootElement();
		String eleName = root.getName();
		if (eleName==null || !eleName.equals("handlers")) {
			throw new Exception("Invalid XML. Root element is not called <handlers>, but <"+eleName+">");
		}
		
		for ( Iterator<Element> sensorElements = root.elementIterator(); sensorElements.hasNext(); ) {
			Element sensorElement = sensorElements.next();
			eleName = sensorElement.getName();
			if (eleName==null || !eleName.equals("sensor")) {
				throw new Exception("Invalid XML. Element is not called <sensor>, but <"+eleName+">");
			}
			String type = sensorElement.attributeValue("type");
		
			List<AbstractOutputHandler> handlers = new ArrayList<AbstractOutputHandler>();
			handlersMap.put(type, handlers);
			
			for ( Iterator<Element> handlerElements = sensorElement.elementIterator(); handlerElements.hasNext(); ) {
				Element handlerElement = handlerElements.next();
				eleName = handlerElement.getName();
				if (eleName==null || !eleName.equals("handler")) {
					throw new Exception("Invalid XML. Element is not called <handler>, but <"+eleName+">");
				}
				
				// Get name and class attributes
				String handlerName = null;
				String handlerClassName = null;
				AbstractOutputHandler handler = null;
				for (Iterator<Attribute> handlerAttributes = handlerElement.attributeIterator(); handlerAttributes.hasNext(); ) {
					Attribute att = handlerAttributes.next();
					if (att.getName().equals("name")) {
						handlerName = att.getValue();
					}
					else if (att.getName().equals("class")) {
						handlerClassName=att.getValue();
					}
				}
				if (handlerName==null || handlerName.trim().length()==0 || handlerClassName==null || handlerClassName.trim().length()==0 ) {
					throw new Exception("handler does not have the required name='' and class='' attributes defined");
				}
				// Create new handler
				Class<?> clazz = Class.forName(handlerClassName);
				handler = (AbstractOutputHandler) clazz.newInstance();
				if (handler == null) {
					throw new Exception("handler name="+handlerName+", class="+handlerClassName+" could not be initialised");
				}
				handler.setName(handlerName);
				handlers.add(handler);
				log.debug("Handler created "+handler);

				for (Iterator<Element> paramElements = handlerElement.elementIterator(); paramElements.hasNext(); ) {
					Element paramElement = paramElements.next();
					eleName = paramElement.getName();
					if (eleName==null || !eleName.equals("param")) {
						throw new Exception("Invalid XML. Element is not called <param>, but <"+eleName+">");
					}
					
					String paramName = paramElement.attributeValue("name");
					String paramValue = paramElement.getStringValue();
					if (paramName!=null && paramName.trim().length()>0 && paramValue!=null && paramValue.trim().length()>0 ) {
						log.debug("  with property "+paramName+"="+paramValue);
						handler.getProperties().put(paramName,paramValue);
					}
				}
			}
		}
		return handlersMap;
	}
	
	public static VelocityContext getDefaultVelocityContext() {
		VelocityContext context = new VelocityContext();
		context.put( Integer.class.getSimpleName(), Integer.class );
		context.put( Float.class.getSimpleName(), Float.class );
		return context;
	}

	public static String useVelocityTemplate(VelocityEngine ve, String template, VelocityContext context) {
		StringWriter sw = new StringWriter();
		Template vetemplate = ve.getTemplate(template);
		vetemplate.merge(context, sw);
		String result = sw.toString();
		return result;
	}
	
	public static String getHost(String url) {
		int startIndex = url.indexOf("://");
		if (startIndex>0) {
			startIndex+=3;
			int endIndex = url.indexOf("/",startIndex);
			if (endIndex>startIndex) {
				String currentHost = url.substring(startIndex,endIndex);
				return currentHost;
			}
		}
		return null;
	}

	public static String getTextFileName(String filename) {
		int index = filename.lastIndexOf(".");
		filename = filename.substring(0,index);
		return filename+".txt";
	}
	
	public static String getFileExtension(String filename) {
		int index = filename.lastIndexOf(".");
		return filename.substring(index+1);
	}
	
	public static String firstCharUpperCase(String input) {
		return input.substring(0, 1).toUpperCase()+input.substring(1);
	}
	
	public static List<DataPoint> mapLinesToDataPoints(List<String> rawContentList) {
		List<DataPoint> dataPoints = new ArrayList<DataPoint>();
		for (String rawContent : rawContentList) {
			dataPoints.add(mapLineToDataPoint(rawContent));
		}
		return dataPoints;
	}
	
	public static DataPoint mapLineToDataPoint(String line) {
		String[] lineParts = line.replace("\n","").split("\\|");
		DataPoint dataPoint = new DataPoint();
		for (int i=0; i<lineParts.length; i++) {
			switch(i) {
				case 0: dataPoint.date=lineParts[i]; break;
				case 1: dataPoint.time=lineParts[i]; break;
				case 2: dataPoint.ip=lineParts[i]; break;
				case 3: dataPoint.heap=lineParts[i]; break;
				case 4: dataPoint.value=lineParts[i]; break;
			}
			if (i>4) {
				dataPoint.values.add(lineParts[i]);
			}
		}
		return dataPoint;
	}
	
    public static void main( String[] args )
    {
        try {
        	log.info( "Hello World!" );
		} catch (Exception e) {
			log.error(e,e);
		}
    }
        
}
